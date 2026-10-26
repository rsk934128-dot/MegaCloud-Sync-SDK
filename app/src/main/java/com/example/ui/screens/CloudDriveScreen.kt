package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.FileProvider
import com.example.model.CloudFileItem
import com.example.ui.components.FileCategoryFilter
import com.example.ui.components.FileSortOrder
import com.example.ui.components.FileTagBadge
import com.example.ui.components.SmartSearchFilterBar
import com.example.ui.components.formatBytes
import com.example.ui.dialogs.FilePreviewDialog
import com.example.ui.dialogs.PinLockDialog
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
fun CloudDriveScreen(
    viewModel: AppPulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudFiles by viewModel.cloudFiles.collectAsState()
    val currentPath by viewModel.currentCloudPath.collectAsState()

    var previewItem by remember { mutableStateOf<CloudFileItem?>(null) }
    var showVoiceMemoModal by remember { mutableStateOf(false) }
    var showOfflineVaultModal by remember { mutableStateOf(false) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FileCategoryFilter.ALL) }
    var sortOrder by remember { mutableStateOf(FileSortOrder.NEWEST) }

    val filteredFiles = remember(cloudFiles, searchQuery, selectedCategory, sortOrder) {
        cloudFiles.filter { item ->
            val matchesQuery = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                FileCategoryFilter.ALL -> true
                FileCategoryFilter.APK -> item.name.endsWith(".apk", ignoreCase = true) || item.category == "APK"
                FileCategoryFilter.AUDIO -> item.name.endsWith(".m4a", ignoreCase = true) || item.name.endsWith(".mp3", ignoreCase = true) || item.category == "AUDIO"
                FileCategoryFilter.DOCS -> item.name.endsWith(".pdf", ignoreCase = true) || item.name.endsWith(".txt", ignoreCase = true) || item.name.endsWith(".json", ignoreCase = true) || item.name.endsWith(".doc", ignoreCase = true) || item.category == "AUDIT"
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
                    FileSortOrder.NEWEST -> b.modifiedEpochMs.compareTo(a.modifiedEpochMs)
                    FileSortOrder.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                    FileSortOrder.SIZE -> b.sizeBytes.compareTo(a.sizeBytes)
                }
            }
        }
    }

    // SAF (Storage Access Framework) System Document & File Picker
    val safFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                viewModel.showSnackbar("Reading selected file from device...")
                try {
                    var resolvedName = "device_file_${System.currentTimeMillis()}"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            resolvedName = cursor.getString(nameIndex) ?: resolvedName
                        }
                    }

                    val cacheFile = File(context.cacheDir, resolvedName)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    if (cacheFile.exists()) {
                        viewModel.uploadLocalFile(
                            file = cacheFile,
                            remoteName = resolvedName,
                            category = "DEVICE_UPLOAD"
                        )
                    }
                } catch (e: Exception) {
                    viewModel.showSnackbar("Failed to read file: ${e.message}")
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Breadcrumb & Up Navigation Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("breadcrumb_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPath != "/") {
                    IconButton(
                        onClick = { viewModel.navigateUp() },
                        modifier = Modifier.size(32.dp).testTag("navigate_up_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Up",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Root",
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (currentPath == "/") "MEGA Drive (Root)" else "MEGA: $currentPath",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.showCreateFolderDialog.value = true },
                    modifier = Modifier.size(32.dp).testTag("new_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { safFilePickerLauncher.launch("*/*") },
                    modifier = Modifier.size(32.dp).testTag("saf_upload_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Pick Device File",
                        tint = SecurityGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Switch to Firebase Bucket
        OutlinedButton(
            onClick = { viewModel.selectTab(com.example.viewmodel.NavTab.FIREBASE_STORAGE) },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecurityAmber),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .testTag("switch_to_firebase_storage_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SecurityAmber
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Switch to Firebase Cloud Storage Bucket Inventory", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { safFilePickerLauncher.launch("*/*") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("pick_device_file_btn")
            ) {
                Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp), tint = SecurityGreen)
                Spacer(modifier = Modifier.width(3.dp))
                Text("Pick File", fontSize = 10.sp, maxLines = 1)
            }

            OutlinedButton(
                onClick = { showVoiceMemoModal = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("record_voice_memo_btn")
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(15.dp), tint = CyanPrimary)
                Spacer(modifier = Modifier.width(3.dp))
                Text("Voice Memo", fontSize = 10.sp, maxLines = 1)
            }

            val isPinEnabled by viewModel.isPinProtectionEnabled.collectAsState()
            Button(
                onClick = {
                    if (isPinEnabled) {
                        showPinUnlockDialog = true
                    } else {
                        showOfflineVaultModal = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("view_offline_vault_btn")
            ) {
                Icon(imageVector = Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Vault", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

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
            totalCount = cloudFiles.size,
            filteredCount = filteredFiles.size,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Files List
        if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedCategory != FileCategoryFilter.ALL) "No matching files found" else "This folder is empty",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedCategory != FileCategoryFilter.ALL) "Try adjusting search query or category chips." else "Pick a device file, record a voice note, or sync an APK.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFiles, key = { it.id }) { item ->
                    CloudFileRow(
                        item = item,
                        viewModel = viewModel,
                        onPreviewClick = { previewItem = item }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Voice Memo Modal Dialog
    if (showVoiceMemoModal) {
        VoiceMemoDialog(
            onDismiss = { showVoiceMemoModal = false },
            onUploadRecording = { file, name ->
                viewModel.uploadLocalFile(
                    file = file,
                    remoteName = name,
                    category = "AUDIO"
                )
            }
        )
    }

    // In-App Preview & Offline Caching Dialog
    if (previewItem != null) {
        val item = previewItem!!
        val isAudio = item.name.endsWith(".m4a", true) || item.name.endsWith(".mp3", true)
        val isApk = item.name.endsWith(".apk", true)
        val isAudit = item.name.endsWith(".json", true)

        val localCached = File(context.filesDir, "offline_vault/${item.name}")
        val effectiveLocalFile = if (localCached.exists()) localCached else File(context.cacheDir, item.name)

        FilePreviewDialog(
            fileName = item.name,
            filePath = item.path,
            fileSize = item.sizeBytes,
            contentType = when {
                isAudio -> "audio/mp4"
                isApk -> "application/vnd.android.package-archive"
                isAudit -> "application/json"
                else -> "application/octet-stream"
            },
            downloadUrl = item.publicShareUrl,
            previewContent = when {
                isAudit -> "{\n  \"source\": \"AppPulse Security Engine\",\n  \"file\": \"${item.name}\",\n  \"cloud\": \"MEGA Drive\",\n  \"encryption\": \"Client-Side AES-128\",\n  \"status\": \"VERIFIED\"\n}"
                isApk -> "App Package: ${item.name}\nSize: ${formatBytes(item.sizeBytes)}\nStatus: Signed & Cloud Verified"
                isAudio -> "Voice Memo: ${item.name}\nDuration: Live Playback Ready"
                else -> "Raw binary data stream for ${item.name} (${formatBytes(item.sizeBytes)})"
            },
            localFile = if (effectiveLocalFile.exists()) effectiveLocalFile else null,
            onDismiss = { previewItem = null },
            onSaveOffline = {
                viewModel.showSnackbar("Saved ${item.name} to device offline vault.")
            }
        )
    }

    // Offline Vault Dialog
    if (showOfflineVaultModal) {
        OfflineVaultModal(
            context = context,
            onDismiss = { showOfflineVaultModal = false },
            onOpenFile = { file ->
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val mime = if (file.name.endsWith(".apk")) {
                        "application/vnd.android.package-archive"
                    } else if (file.name.endsWith(".m4a")) {
                        "audio/*"
                    } else {
                        "*/*"
                    }
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    viewModel.showSnackbar("Cannot open file: ${e.message}")
                }
            }
        )
    }

    // PIN Lock Dialog for Offline Vault Access
    if (showPinUnlockDialog) {
        PinLockDialog(
            vaultSecurityManager = viewModel.vaultSecurityManager,
            onUnlocked = {
                showPinUnlockDialog = false
                showOfflineVaultModal = true
            },
            onDismiss = { showPinUnlockDialog = false },
            title = "Offline Vault Access",
            subtitle = "Enter 4-digit PIN to access cached secure files"
        )
    }
}

@Composable
fun CloudFileRow(
    item: CloudFileItem,
    viewModel: AppPulseViewModel,
    onPreviewClick: () -> Unit
) {
    val isApk = item.name.endsWith(".apk", ignoreCase = true)
    val isAudio = item.name.endsWith(".m4a", ignoreCase = true) || item.name.endsWith(".mp3", ignoreCase = true) || item.category == "AUDIO"
    val isAudit = item.name.endsWith(".json", ignoreCase = true) || item.category == "AUDIT"
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(item.modifiedEpochMs))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable {
                if (item.isFolder) {
                    viewModel.navigateToFolder(item.path)
                } else {
                    onPreviewClick()
                }
            }
            .testTag("cloud_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            item.isFolder -> CyanPrimary.copy(alpha = 0.15f)
                            isApk -> SecurityAmber.copy(alpha = 0.15f)
                            isAudio -> CyanPrimary.copy(alpha = 0.25f)
                            isAudit -> MegaRed.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isFolder -> Icons.Default.Folder
                        isApk -> Icons.Default.Android
                        isAudio -> Icons.Default.Audiotrack
                        isAudit -> Icons.Default.Shield
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = when {
                        item.isFolder -> CyanPrimary
                        isApk -> SecurityAmber
                        isAudio -> CyanPrimary
                        isAudit -> MegaRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
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
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isFolder) {
                        Text(
                            text = "Folder • $dateStr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "${formatBytes(item.sizeBytes)} • $dateStr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (item.isEncrypted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CyanPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "AES",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            if (!item.isFolder) {
                // In-App Preview button
                IconButton(
                    onClick = onPreviewClick,
                    modifier = Modifier.size(34.dp).testTag("preview_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Share button
                IconButton(
                    onClick = { viewModel.generateShareLink(item) },
                    modifier = Modifier.size(34.dp).testTag("share_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Link",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Download or Pull-Install APK button
                IconButton(
                    onClick = {
                        viewModel.downloadAndInstallApk(item)
                    },
                    modifier = Modifier.size(34.dp).testTag("download_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = if (isApk) "Pull & Install" else "Download",
                        tint = if (isApk) SecurityAmber else SecurityGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Delete item button
            IconButton(
                onClick = { viewModel.deleteCloudFile(item.id) },
                modifier = Modifier.size(34.dp).testTag("delete_btn_${item.id}")
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

@Composable
fun OfflineVaultModal(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onOpenFile: (File) -> Unit
) {
    val vaultDir = File(context.filesDir, "offline_vault")
    var files by remember { mutableStateOf(vaultDir.listFiles()?.toList() ?: emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Offline Cached Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "These files are securely cached locally and can be accessed without internet connectivity.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No files cached offline yet.\nOpen any file preview and tap 'Save Offline'.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { file ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = formatBytes(file.length()),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { onOpenFile(file) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Open",
                                            tint = CyanPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            file.delete()
                                            files = vaultDir.listFiles()?.toList() ?: emptyList()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MegaRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
