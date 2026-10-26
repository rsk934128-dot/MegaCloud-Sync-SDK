package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseStorageManager
import com.example.data.model.FirebaseStorageItem
import com.example.ui.components.FileCategoryFilter
import com.example.ui.components.FileSortOrder
import com.example.ui.components.FileTagBadge
import com.example.ui.components.FirebaseBucketDonutChart
import com.example.ui.components.SmartSearchFilterBar
import com.example.ui.components.formatBytes
import com.example.ui.dialogs.FilePreviewDialog
import com.example.ui.dialogs.VoiceMemoDialog
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MegaRed
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.viewmodel.AppPulseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FirebaseStorageScreen(
    viewModel: AppPulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageManager = remember { FirebaseStorageManager(context) }

    var currentPath by remember { mutableStateOf("") }
    var itemsList by remember { mutableStateOf<List<FirebaseStorageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showUploadModal by remember { mutableStateOf(false) }
    var showVoiceMemoModal by remember { mutableStateOf(false) }
    var previewItem by remember { mutableStateOf<FirebaseStorageItem?>(null) }

    fun refreshInventory() {
        scope.launch {
            isLoading = true
            val result = storageManager.listRemoteInventory(currentPath)
            result.onSuccess {
                itemsList = it
            }.onFailure {
                viewModel.showSnackbar("Failed to list bucket: ${it.localizedMessage}")
            }
            isLoading = false
        }
    }

    LaunchedEffect(currentPath) {
        refreshInventory()
    }

    // SAF (Storage Access Framework) System Document & File Picker
    val safFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                viewModel.showSnackbar("Reading selected file from device...")
                try {
                    var resolvedName = "uploaded_file_${System.currentTimeMillis()}"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            resolvedName = cursor.getString(nameIndex) ?: resolvedName
                        }
                    }

                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }

                    if (bytes != null) {
                        viewModel.showSnackbar("Uploading $resolvedName to Firebase bucket...")
                        val uploadRes = storageManager.uploadToRemoteStorage(
                            fileName = resolvedName,
                            content = bytes,
                            contentType = mimeType,
                            destinationFolder = currentPath
                        )
                        if (uploadRes.isSuccess) {
                            viewModel.showSnackbar("Uploaded $resolvedName successfully!")
                            refreshInventory()
                        } else {
                            viewModel.showSnackbar("Upload failed: ${uploadRes.exceptionOrNull()?.message}")
                        }
                    }
                } catch (e: Exception) {
                    viewModel.showSnackbar("Error reading file: ${e.message}")
                }
            }
        }
    }

    var selectedCategory by remember { mutableStateOf(FileCategoryFilter.ALL) }
    var sortOrder by remember { mutableStateOf(FileSortOrder.NEWEST) }

    val filteredItems = remember(itemsList, searchQuery, selectedCategory, sortOrder) {
        itemsList.filter { item ->
            val matchesQuery = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                FileCategoryFilter.ALL -> true
                FileCategoryFilter.APK -> item.name.endsWith(".apk", ignoreCase = true)
                FileCategoryFilter.AUDIO -> item.name.endsWith(".m4a", ignoreCase = true) || item.name.endsWith(".mp3", ignoreCase = true) || item.name.endsWith(".wav", ignoreCase = true)
                FileCategoryFilter.DOCS -> item.name.endsWith(".json", ignoreCase = true) || item.name.endsWith(".pdf", ignoreCase = true) || item.name.endsWith(".txt", ignoreCase = true)
                FileCategoryFilter.IMAGES -> item.name.endsWith(".png", ignoreCase = true) || item.name.endsWith(".jpg", ignoreCase = true) || item.name.endsWith(".jpeg", ignoreCase = true)
            }
            if (item.isFolder && selectedCategory == FileCategoryFilter.ALL && searchQuery.isBlank()) {
                true
            } else if (item.isFolder) {
                matchesQuery
            } else {
                matchesQuery && matchesCategory
            }
        }.sortedWith { a, b ->
            if (a.isFolder != b.isFolder) {
                if (a.isFolder) -1 else 1
            } else {
                when (sortOrder) {
                    FileSortOrder.NEWEST -> b.updatedEpochMs.compareTo(a.updatedEpochMs)
                    FileSortOrder.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                    FileSortOrder.SIZE -> b.sizeBytes.compareTo(a.sizeBytes)
                }
            }
        }
    }

    val totalFiles = filteredItems.count { !it.isFolder }
    val totalSizeBytes = filteredItems.filter { !it.isFolder }.sumOf { it.sizeBytes }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Bucket Identity & Quota Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .testTag("firebase_bucket_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecurityAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Bucket",
                            tint = SecurityAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Firebase Cloud Storage",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val isCloudOnline = storageManager.isCloudSyncOnline()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isCloudOnline) SecurityGreen.copy(alpha = 0.2f) else CyanPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isCloudOnline) "CLOUD SYNC" else "SECURE VAULT",
                                    color = if (isCloudOnline) SecurityGreen else CyanPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (storageManager.isCloudSyncOnline()) storageManager.bucketName else "${storageManager.bucketName} • Hybrid Vault",
                            fontSize = 11.sp,
                            color = CyanPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { refreshInventory() },
                        modifier = Modifier.testTag("refresh_bucket_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = CyanPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = CyanPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Remote Files: $totalFiles item(s)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total Storage: ${formatBytes(totalSizeBytes)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real-Time Compose Canvas Donut Chart Component
        FirebaseBucketDonutChart(
            items = itemsList,
            bucketName = storageManager.bucketName,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Breadcrumb Bar & Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPath.isNotBlank()) {
                IconButton(
                    onClick = {
                        val parent = currentPath.substringBeforeLast('/', "")
                        currentPath = parent
                    },
                    modifier = Modifier.size(36.dp).testTag("bucket_navigate_up")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = if (currentPath.isBlank()) "Bucket: /" else "Bucket: /$currentPath",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // SAF Device File Picker Button
            OutlinedButton(
                onClick = { safFilePickerLauncher.launch("*/*") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("saf_pick_file_btn")
            ) {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pick File", fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Voice Memo Button
            Button(
                onClick = { showVoiceMemoModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = MegaRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("voice_memo_modal_btn")
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Audio", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Smart Search, Tagging & Filter Bar
        SmartSearchFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            sortOrder = sortOrder,
            onCycleSortOrder = {
                sortOrder = when (sortOrder) {
                    FileSortOrder.NEWEST -> FileSortOrder.NAME
                    FileSortOrder.NAME -> FileSortOrder.SIZE
                    FileSortOrder.SIZE -> FileSortOrder.NEWEST
                }
            },
            totalCount = itemsList.size,
            filteredCount = filteredItems.size,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Items List
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isLoading) "Loading Firebase Storage Inventory..." else "No remote files in this folder",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Pick a device file, record a voice memo, or upload a report.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.path }) { item ->
                    FirebaseStorageItemRow(
                        item = item,
                        onFolderClick = { folderPath -> currentPath = folderPath },
                        onItemClick = { previewItem = item },
                        onDeleteClick = {
                            scope.launch {
                                val delRes = storageManager.deleteRemoteFile(item.path)
                                if (delRes.isSuccess) {
                                    viewModel.showSnackbar("Deleted ${item.name} from Firebase bucket")
                                    refreshInventory()
                                } else {
                                    viewModel.showSnackbar("Delete failed: ${delRes.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        onCopyUrl = { url ->
                            viewModel.copyToClipboard(url, "Firebase Download URL")
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Voice Memo Vault Modal
    if (showVoiceMemoModal) {
        VoiceMemoDialog(
            onDismiss = { showVoiceMemoModal = false },
            onUploadRecording = { audioFile, fileName ->
                scope.launch {
                    viewModel.showSnackbar("Uploading voice memo $fileName to Firebase...")
                    val bytes = withContext(Dispatchers.IO) { audioFile.readBytes() }
                    val res = storageManager.uploadToRemoteStorage(
                        fileName = fileName,
                        content = bytes,
                        contentType = "audio/mp4",
                        destinationFolder = currentPath
                    )
                    if (res.isSuccess) {
                        viewModel.showSnackbar("Voice memo uploaded to Firebase Storage!")
                        refreshInventory()
                    } else {
                        viewModel.showSnackbar("Upload failed: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        )
    }

    // In-App File Preview & Offline Cache Dialog
    previewItem?.let { item ->
        val localFile = item.localFilePath?.let { File(it) }
            ?: File(context.filesDir, "cloud_vault/${item.path}").takeIf { it.exists() }
        val isText = item.name.endsWith(".json", true) || item.name.endsWith(".txt", true) || item.name.endsWith(".log", true)
        val actualContent = if (isText && localFile != null && localFile.exists()) {
            try {
                localFile.readText()
            } catch (e: Exception) {
                null
            }
        } else null

        FilePreviewDialog(
            fileName = item.name,
            filePath = item.path,
            fileSize = item.sizeBytes,
            contentType = item.contentType,
            downloadUrl = item.downloadUrl,
            previewContent = actualContent ?: "{\n  \"fileName\": \"${item.name}\",\n  \"storageBucket\": \"${item.bucketName}\",\n  \"remotePath\": \"${item.path}\",\n  \"verified\": true\n}",
            localFile = localFile,
            onDismiss = { previewItem = null },
            onSaveOffline = {
                viewModel.showSnackbar("Saved ${item.name} to offline vault cache.")
            }
        )
    }

    // Manual Upload Modal
    if (showUploadModal) {
        var uploadName by remember { mutableStateOf("cloud_diagnostic_report.json") }
        var uploadContent by remember {
            mutableStateOf("{\n  \"source\": \"AppPulse Firebase Storage\",\n  \"status\": \"VERIFIED_HEALTHY\",\n  \"storageBucket\": \"${storageManager.bucketName}\"\n}")
        }

        AlertDialog(
            onDismissRequest = { showUploadModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Upload to Storage Bucket", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = uploadName,
                        onValueChange = { uploadName = it },
                        label = { Text("File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("upload_filename_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uploadContent,
                        onValueChange = { uploadContent = it },
                        label = { Text("Payload / Content") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            showUploadModal = false
                            viewModel.showSnackbar("Uploading $uploadName to Firebase...")
                            val res = storageManager.uploadToRemoteStorage(
                                fileName = uploadName,
                                content = uploadContent.toByteArray(Charsets.UTF_8),
                                destinationFolder = currentPath
                            )
                            if (res.isSuccess) {
                                viewModel.showSnackbar("Uploaded $uploadName successfully to Firebase bucket!")
                                refreshInventory()
                            } else {
                                viewModel.showSnackbar("Upload failed: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Upload", color = Color(0xFF00202E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadModal = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FirebaseStorageItemRow(
    item: FirebaseStorageItem,
    onFolderClick: (String) -> Unit,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyUrl: (String) -> Unit
) {
    val isApk = item.name.endsWith(".apk", ignoreCase = true)
    val isJson = item.name.endsWith(".json", ignoreCase = true)
    val isAudio = item.name.endsWith(".m4a", true) || item.name.endsWith(".mp3", true) || item.name.endsWith(".wav", true)
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(item.updatedEpochMs))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable {
                if (item.isFolder) {
                    onFolderClick(item.path)
                } else {
                    onItemClick()
                }
            }
            .testTag("storage_item_${item.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            item.isFolder -> CyanPrimary.copy(alpha = 0.15f)
                            isApk -> SecurityAmber.copy(alpha = 0.15f)
                            isJson -> MegaRed.copy(alpha = 0.15f)
                            isAudio -> SecurityGreen.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isFolder -> Icons.Default.Folder
                        isApk -> Icons.Default.Android
                        isJson -> Icons.Default.Security
                        isAudio -> Icons.Default.Mic
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = when {
                        item.isFolder -> CyanPrimary
                        isApk -> SecurityAmber
                        isJson -> MegaRed
                        isAudio -> SecurityGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!item.isFolder) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FileTagBadge(fileName = item.name)
                    }
                }
                Text(
                    text = if (item.isFolder) "Folder" else "${formatBytes(item.sizeBytes)} • $dateStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!item.isFolder) {
                IconButton(
                    onClick = onItemClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!item.downloadUrl.isNullOrBlank()) {
                    IconButton(
                        onClick = { onCopyUrl(item.downloadUrl) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Copy Link",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
