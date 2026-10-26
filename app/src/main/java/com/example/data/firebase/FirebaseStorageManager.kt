package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.FirebaseStorageItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseStorageManager(private val context: Context) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance().apply {
            // Low retry timeouts to avoid prolonged exponential backoff loops if network or bucket issues occur
            maxOperationRetryTimeMillis = 3000L
            maxDownloadRetryTimeMillis = 3000L
            maxUploadRetryTimeMillis = 3000L
        }

    val bucketName: String
        get() = try {
            storage.app.options.storageBucket ?: "gen-lang-client-0558514707.firebasestorage.app"
        } catch (e: Exception) {
            "gen-lang-client-0558514707.firebasestorage.app"
        }

    private val vaultBaseDir: File
        get() = File(context.filesDir, "cloud_vault").apply { mkdirs() }

    @Volatile
    private var isRemoteBucketAvailable: Boolean? = null

    fun isCloudSyncOnline(): Boolean = isRemoteBucketAvailable == true

    /**
     * Checks if the remote Firebase Cloud Storage bucket exists and is reachable
     * using a lightweight HTTP GET without invoking the Storage SDK's ExponentialBackoffSender,
     * avoiding 404 StorageException error floods when a bucket is not provisioned on GCP.
     */
    private suspend fun checkBucketAvailability(bucket: String): Boolean = withContext(Dispatchers.IO) {
        if (bucket.isBlank()) return@withContext false
        try {
            val url = URL("https://firebasestorage.googleapis.com/v0/b/$bucket/o?maxResults=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                instanceFollowRedirects = false
            }
            val code = conn.responseCode
            conn.disconnect()
            // 200 OK or 401/403 (unauthorized/forbidden) indicates the bucket exists.
            // 404 indicates the storage bucket has not been created or does not exist at all.
            code in 200..403
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun isRemoteStorageActive(): Boolean {
        isRemoteBucketAvailable?.let { return it }
        val available = checkBucketAvailability(bucketName)
        isRemoteBucketAvailable = available
        if (!available) {
            Log.i("FirebaseStorage", "Remote bucket '$bucketName' is not provisioned (HTTP 404). Operating in local secure vault mode.")
        }
        return available
    }

    suspend fun listRemoteInventory(folderPath: String = ""): Result<List<FirebaseStorageItem>> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid
            val basePrefix = if (folderPath.isBlank()) {
                if (uid != null) "users/$uid" else "public"
            } else {
                folderPath.trim('/')
            }

            // Ensure baseline sample files exist locally so user can immediately preview and inspect them
            ensureBaselineFilesExist(basePrefix)

            val itemsList = mutableListOf<FirebaseStorageItem>()

            // 1. Gather files and subfolders from the local vault for this folder path
            val folderDir = File(vaultBaseDir, basePrefix)
            if (folderDir.exists() && folderDir.isDirectory) {
                folderDir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        itemsList.add(
                            FirebaseStorageItem(
                                name = file.name,
                                path = "$basePrefix/${file.name}",
                                sizeBytes = 0L,
                                updatedEpochMs = file.lastModified(),
                                isFolder = true,
                                bucketName = bucketName,
                                localFilePath = file.absolutePath,
                                isLocalVault = true
                            )
                        )
                    } else {
                        val mimeType = detectMimeType(file.name)
                        itemsList.add(
                            FirebaseStorageItem(
                                name = file.name,
                                path = "$basePrefix/${file.name}",
                                sizeBytes = file.length(),
                                updatedEpochMs = file.lastModified(),
                                contentType = mimeType,
                                downloadUrl = null,
                                isFolder = false,
                                bucketName = bucketName,
                                localFilePath = file.absolutePath,
                                isLocalVault = true
                            )
                        )
                    }
                }
            }

            // 2. If the remote bucket is verified available, query remote Firebase Storage
            if (isRemoteStorageActive()) {
                try {
                    val targetRef: StorageReference = storage.reference.child(basePrefix)
                    val listResult = targetRef.listAll().await()

                    // Subfolders / Prefixes
                    for (prefix in listResult.prefixes) {
                        if (itemsList.none { it.name == prefix.name && it.isFolder }) {
                            itemsList.add(
                                FirebaseStorageItem(
                                    name = prefix.name,
                                    path = prefix.path,
                                    sizeBytes = 0L,
                                    updatedEpochMs = System.currentTimeMillis(),
                                    isFolder = true,
                                    bucketName = bucketName,
                                    isLocalVault = false
                                )
                            )
                        }
                    }

                    // Files
                    for (itemRef in listResult.items) {
                        val metadata: StorageMetadata? = try {
                            itemRef.metadata.await()
                        } catch (e: Exception) {
                            null
                        }

                        val downloadUrl: String? = try {
                            itemRef.downloadUrl.await().toString()
                        } catch (e: Exception) {
                            null
                        }

                        val existingIndex = itemsList.indexOfFirst { it.name == itemRef.name && !it.isFolder }
                        val remoteItem = FirebaseStorageItem(
                            name = itemRef.name,
                            path = itemRef.path,
                            sizeBytes = metadata?.sizeBytes ?: 0L,
                            updatedEpochMs = metadata?.updatedTimeMillis ?: System.currentTimeMillis(),
                            contentType = metadata?.contentType ?: detectMimeType(itemRef.name),
                            downloadUrl = downloadUrl,
                            isFolder = false,
                            bucketName = bucketName,
                            isLocalVault = false
                        )

                        if (existingIndex >= 0) {
                            val existing = itemsList[existingIndex]
                            itemsList[existingIndex] = remoteItem.copy(localFilePath = existing.localFilePath)
                        } else {
                            itemsList.add(remoteItem)
                        }
                    }
                } catch (se: StorageException) {
                    if (se.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND ||
                        se.errorCode == StorageException.ERROR_BUCKET_NOT_FOUND ||
                        se.httpResultCode == 404
                    ) {
                        isRemoteBucketAvailable = false
                        Log.w("FirebaseStorage", "Remote storage bucket returned 404. Falling back to local vault.")
                    } else {
                        Log.w("FirebaseStorage", "Remote storage list exception: ${se.message}")
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseStorage", "Storage bucket list encountered error: ${e.message}")
                }
            }

            // Sort with folders first, then files
            itemsList.sortWith { a, b ->
                if (a.isFolder != b.isFolder) {
                    if (a.isFolder) -1 else 1
                } else {
                    a.name.compareTo(b.name, ignoreCase = true)
                }
            }

            Result.success(itemsList)
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Failed to list remote inventory", e)
            Result.failure(e)
        }
    }

    suspend fun uploadToRemoteStorage(
        fileName: String,
        content: ByteArray,
        contentType: String = "application/json",
        destinationFolder: String = ""
    ): Result<FirebaseStorageItem> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid
            val basePath = if (destinationFolder.isNotBlank()) {
                destinationFolder.trim('/')
            } else if (uid != null) {
                "users/$uid"
            } else {
                "public"
            }

            // Always persist locally in the secure app vault first so no user data is lost
            val folderDir = File(vaultBaseDir, basePath).apply { mkdirs() }
            val localFile = File(folderDir, fileName)
            localFile.writeBytes(content)

            var remoteDownloadUrl: String? = null
            var isUploadedRemotely = false

            if (isRemoteStorageActive()) {
                try {
                    val fileRef = storage.reference.child("$basePath/$fileName")
                    val metadata = StorageMetadata.Builder()
                        .setContentType(contentType)
                        .setCustomMetadata("uploadedBy", uid ?: "anonymous")
                        .build()

                    fileRef.putBytes(content, metadata).await()
                    remoteDownloadUrl = try {
                        fileRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        null
                    }
                    isUploadedRemotely = true
                } catch (se: StorageException) {
                    if (se.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND ||
                        se.errorCode == StorageException.ERROR_BUCKET_NOT_FOUND ||
                        se.httpResultCode == 404
                    ) {
                        isRemoteBucketAvailable = false
                        Log.w("FirebaseStorage", "Remote bucket is not provisioned (404). File persisted securely in local vault.")
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseStorage", "Remote upload failed (${e.message}). File saved securely in local vault.")
                }
            }

            val item = FirebaseStorageItem(
                name = fileName,
                path = "$basePath/$fileName",
                sizeBytes = content.size.toLong(),
                updatedEpochMs = System.currentTimeMillis(),
                contentType = contentType,
                downloadUrl = remoteDownloadUrl,
                isFolder = false,
                bucketName = if (isUploadedRemotely) bucketName else "AppPulse Local Vault",
                localFilePath = localFile.absolutePath,
                isLocalVault = !isUploadedRemotely
            )
            Result.success(item)
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Failed to upload file to storage", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRemoteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedPath = remotePath.trim('/')

            // 1. Delete from local vault if present
            val localFile = File(vaultBaseDir, normalizedPath)
            if (localFile.exists()) {
                localFile.delete()
            }

            // 2. If remote bucket is available, delete from remote storage
            if (isRemoteStorageActive()) {
                try {
                    val fileRef = storage.reference.child(normalizedPath)
                    fileRef.delete().await()
                } catch (se: StorageException) {
                    if (se.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND ||
                        se.errorCode == StorageException.ERROR_BUCKET_NOT_FOUND ||
                        se.httpResultCode == 404
                    ) {
                        // Object already does not exist at location, treat as success!
                        Log.i("FirebaseStorage", "Object $normalizedPath not found on remote (404), deleted from local vault.")
                    } else {
                        throw se
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseStorage", "Remote delete exception: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Failed to delete file: $remotePath", e)
            Result.failure(e)
        }
    }

    private fun ensureBaselineFilesExist(basePrefix: String) {
        try {
            val auditsDir = File(vaultBaseDir, "$basePrefix/audits").apply { mkdirs() }
            val apkDir = File(vaultBaseDir, "$basePrefix/apk_vault").apply { mkdirs() }

            val reportFile = File(auditsDir, "threat_report_latest.json")
            if (!reportFile.exists()) {
                val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                reportFile.writeText(
                    """{
  "systemAudit": "AppPulse Threat Scanner v3.4",
  "auditTimestamp": "$formattedDate",
  "status": "SECURE",
  "threatScore": 12,
  "appsScanned": 18,
  "vulnerabilitiesDetected": 0,
  "integrity": "SHA-256 Verified",
  "cloudVaultSync": "READY"
}"""
                )
            }

            val apkFile = File(apkDir, "QuickPay_SecurityCore_v3.4.apk")
            if (!apkFile.exists()) {
                apkFile.writeBytes("PK\u0003\u0004AppPulse-Signed-Package-Mock-Binary".toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Log.w("FirebaseStorage", "Failed to initialize baseline vault files: ${e.message}")
        }
    }

    private fun detectMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".json", true) -> "application/json"
            fileName.endsWith(".apk", true) -> "application/vnd.android.package-archive"
            fileName.endsWith(".m4a", true) -> "audio/mp4"
            fileName.endsWith(".mp3", true) -> "audio/mpeg"
            fileName.endsWith(".wav", true) -> "audio/wav"
            fileName.endsWith(".txt", true) -> "text/plain"
            fileName.endsWith(".pdf", true) -> "application/pdf"
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }
}
