package com.example.manager

import com.example.model.CloudFileItem
import com.example.model.TransferItem
import com.example.model.TransferStatus
import com.example.model.TransferType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class TransferQueueManager(
    private val megaCloudManager: MegaCloudManager,
    private val scope: CoroutineScope
) {
    private val _transfers = MutableStateFlow<List<TransferItem>>(
        listOf(
            TransferItem(
                id = "demo_tx_1",
                title = "AppPulse_SecCore_v1.0.apk",
                type = TransferType.UPLOAD,
                sizeBytes = 28_400_000L,
                bytesTransferred = 28_400_000L,
                progressPercent = 1.0f,
                speedBytesPerSec = 0L,
                status = TransferStatus.COMPLETED,
                remotePath = "/APK_Vault/AppPulse_SecCore_v1.0.apk"
            ),
            TransferItem(
                id = "demo_tx_2",
                title = "ThreatReport_20261019_AutoSync.json",
                type = TransferType.UPLOAD,
                sizeBytes = 482_000L,
                bytesTransferred = 482_000L,
                progressPercent = 1.0f,
                speedBytesPerSec = 0L,
                status = TransferStatus.COMPLETED,
                remotePath = "/Security_Audits/ThreatReport_20261019_AutoSync.json"
            )
        )
    )
    val transfers: StateFlow<List<TransferItem>> = _transfers.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()

    fun enqueueUpload(
        file: File,
        remoteDirectory: String,
        category: String,
        authToken: String,
        uploadEndpoint: String?,
        onComplete: ((CloudFileItem?) -> Unit)? = null
    ): String {
        val id = "tx_" + UUID.randomUUID().toString().take(8)
        val newItem = TransferItem(
            id = id,
            title = file.name,
            type = TransferType.UPLOAD,
            sizeBytes = file.length().coerceAtLeast(1024L),
            bytesTransferred = 0L,
            progressPercent = 0.0f,
            speedBytesPerSec = 0L,
            status = TransferStatus.TRANSFERRING,
            localPath = file.absolutePath,
            remotePath = "$remoteDirectory/${file.name}"
        )

        _transfers.value = listOf(newItem) + _transfers.value

        val job = scope.launch(Dispatchers.IO) {
            val result = megaCloudManager.uploadFileToCloud(
                file = file,
                remoteDirectory = remoteDirectory,
                category = category,
                authToken = authToken,
                uploadEndpoint = uploadEndpoint,
                onProgress = { progress, transferred, speedBps ->
                    updateItem(id) {
                        it.copy(
                            progressPercent = progress,
                            bytesTransferred = transferred,
                            speedBytesPerSec = speedBps,
                            status = TransferStatus.TRANSFERRING
                        )
                    }
                }
            )

            result.onSuccess { cloudItem ->
                updateItem(id) {
                    it.copy(
                        status = TransferStatus.COMPLETED,
                        progressPercent = 1.0f,
                        bytesTransferred = it.sizeBytes,
                        speedBytesPerSec = 0L
                    )
                }
                onComplete?.invoke(cloudItem)
            }.onFailure { err ->
                updateItem(id) {
                    it.copy(
                        status = TransferStatus.FAILED,
                        error = err.message ?: "Upload error",
                        speedBytesPerSec = 0L
                    )
                }
                onComplete?.invoke(null)
            }
        }

        activeJobs[id] = job
        return id
    }

    fun enqueueDownload(
        fileItem: CloudFileItem,
        destinationFile: File,
        onComplete: ((File?) -> Unit)? = null
    ): String {
        val id = "tx_" + UUID.randomUUID().toString().take(8)
        val newItem = TransferItem(
            id = id,
            title = fileItem.name,
            type = TransferType.DOWNLOAD,
            sizeBytes = fileItem.sizeBytes,
            bytesTransferred = 0L,
            progressPercent = 0.0f,
            speedBytesPerSec = 0L,
            status = TransferStatus.TRANSFERRING,
            localPath = destinationFile.absolutePath,
            remotePath = fileItem.path
        )

        _transfers.value = listOf(newItem) + _transfers.value

        val job = scope.launch(Dispatchers.IO) {
            val result = megaCloudManager.downloadFileFromCloud(
                fileItem = fileItem,
                destinationFile = destinationFile,
                onProgress = { progress, transferred, speedBps ->
                    updateItem(id) {
                        it.copy(
                            progressPercent = progress,
                            bytesTransferred = transferred,
                            speedBytesPerSec = speedBps,
                            status = TransferStatus.TRANSFERRING
                        )
                    }
                }
            )

            result.onSuccess { localFile ->
                updateItem(id) {
                    it.copy(
                        status = TransferStatus.COMPLETED,
                        progressPercent = 1.0f,
                        bytesTransferred = it.sizeBytes,
                        speedBytesPerSec = 0L
                    )
                }
                onComplete?.invoke(localFile)
            }.onFailure { err ->
                updateItem(id) {
                    it.copy(
                        status = TransferStatus.FAILED,
                        error = err.message ?: "Download failed",
                        speedBytesPerSec = 0L
                    )
                }
                onComplete?.invoke(null)
            }
        }

        activeJobs[id] = job
        return id
    }

    fun pauseTransfer(id: String) {
        activeJobs[id]?.cancel()
        updateItem(id) { it.copy(status = TransferStatus.PAUSED, speedBytesPerSec = 0L) }
    }

    fun resumeTransfer(id: String) {
        val item = _transfers.value.find { it.id == id } ?: return
        if (item.status != TransferStatus.PAUSED && item.status != TransferStatus.FAILED) return

        val job = scope.launch(Dispatchers.IO) {
            updateItem(id) { it.copy(status = TransferStatus.TRANSFERRING) }
            val file = File(item.localPath)
            if (item.type == TransferType.UPLOAD && file.exists()) {
                val dir = item.remotePath.substringBeforeLast("/", "/")
                val result = megaCloudManager.uploadFileToCloud(
                    file = file,
                    remoteDirectory = dir,
                    category = "GENERAL",
                    authToken = "dummy",
                    uploadEndpoint = null,
                    onProgress = { progress, transferred, speedBps ->
                        updateItem(id) {
                            it.copy(
                                progressPercent = progress,
                                bytesTransferred = transferred,
                                speedBytesPerSec = speedBps,
                                status = TransferStatus.TRANSFERRING
                            )
                        }
                    }
                )
                if (result.isSuccess) {
                    updateItem(id) { it.copy(status = TransferStatus.COMPLETED, progressPercent = 1f) }
                } else {
                    updateItem(id) { it.copy(status = TransferStatus.FAILED, error = "Transfer failed") }
                }
            } else {
                updateItem(id) { it.copy(status = TransferStatus.COMPLETED, progressPercent = 1f) }
            }
        }
        activeJobs[id] = job
    }

    fun cancelTransfer(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        _transfers.value = _transfers.value.filter { it.id != id }
    }

    fun clearCompleted() {
        _transfers.value = _transfers.value.filter { it.status != TransferStatus.COMPLETED }
    }

    private fun updateItem(id: String, transform: (TransferItem) -> TransferItem) {
        _transfers.value = _transfers.value.map { if (it.id == id) transform(it) else it }
    }
}
