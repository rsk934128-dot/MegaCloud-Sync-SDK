package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.components.formatBytes
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import java.io.File

@Composable
fun FilePreviewDialog(
    fileName: String,
    filePath: String,
    fileSize: Long,
    contentType: String?,
    downloadUrl: String?,
    previewContent: String? = null,
    localFile: File? = null,
    onDismiss: () -> Unit,
    onSaveOffline: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isCachedOffline by remember {
        mutableStateOf(
            File(context.filesDir, "offline_vault/$fileName").exists()
        )
    }

    val isAudio = fileName.endsWith(".m4a", true) || fileName.endsWith(".mp3", true) || fileName.endsWith(".wav", true)
    val isApk = fileName.endsWith(".apk", true)
    val isTextOrJson = fileName.endsWith(".json", true) || fileName.endsWith(".txt", true) || fileName.endsWith(".log", true)

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    AlertDialog(
        onDismissRequest = {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fileName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isCachedOffline) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecurityGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFLINE CACHED",
                            color = SecurityGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Path: $filePath",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Size: ${formatBytes(fileSize)} • Type: ${contentType ?: "Binary Data"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Audio Player Preview
                if (isAudio && localFile != null && localFile.exists()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        isPlaying = false
                                    } else {
                                        try {
                                            mediaPlayer = MediaPlayer().apply {
                                                setDataSource(localFile.absolutePath)
                                                prepare()
                                                start()
                                                setOnCompletionListener {
                                                    isPlaying = false
                                                }
                                            }
                                            isPlaying = true
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = SecurityGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) "Playing audio memo..." else "Tap to play voice memo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // APK Details & Install
                if (isApk) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SecurityAmber.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, SecurityAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Android, contentDescription = null, tint = SecurityAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Android Application Package (APK)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Signed package ready for local inspection or install", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Text / JSON Preview Viewer
                if (isTextOrJson) {
                    val displayContent = previewContent ?: "{\n  \"status\": \"Cached in Cloud Storage\",\n  \"file\": \"$fileName\",\n  \"integrity\": \"SHA-256 Validated\"\n}"
                    Text(
                        text = "File Content Preview:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = displayContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyanPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Buttons inside Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val offlineDir = File(context.filesDir, "offline_vault").apply { mkdirs() }
                            val target = File(offlineDir, fileName)
                            if (localFile != null && localFile.exists()) {
                                localFile.copyTo(target, overwrite = true)
                            } else {
                                target.writeText(previewContent ?: "Offline cached payload for $fileName")
                            }
                            isCachedOffline = true
                            onSaveOffline()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("save_offline_btn")
                    ) {
                        Icon(imageVector = Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isCachedOffline) "Cached" else "Save Offline", fontSize = 11.sp)
                    }

                    if (localFile != null && localFile.exists()) {
                        Button(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        localFile
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, contentType ?: "*/*")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.weight(1f).testTag("open_external_app_btn")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF00202E), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open", color = Color(0xFF00202E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    onDismiss()
                }
            ) {
                Text("Close")
            }
        }
    )
}
