package com.example.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.manager.AudioRecorderManager
import com.example.ui.components.formatBytes
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MegaRed
import com.example.ui.theme.SecurityGreen
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceMemoDialog(
    onDismiss: () -> Unit,
    onUploadRecording: (File, String) -> Unit
) {
    val context = LocalContext.current
    val recorderManager = remember { AudioRecorderManager(context) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    var isRecording by remember { mutableStateOf(false) }
    var isPlayingPreview by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var secondsElapsed by remember { mutableIntStateOf(0) }

    // Timer effect while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            secondsElapsed = 0
            while (isRecording) {
                delay(1000L)
                secondsElapsed++
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            recorderManager.cancelRecording()
            recorderManager.stopPreview()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = {
            recorderManager.cancelRecording()
            recorderManager.stopPreview()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Secure Voice Memo Vault",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                if (!hasPermission) {
                    Text(
                        text = "Microphone permission is required to record voice notes for your cloud vault.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("request_audio_perm_btn")
                    ) {
                        Text("Grant Permission", color = Color(0xFF00202E), fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Timer readout
                    val mins = secondsElapsed / 60
                    val secs = secondsElapsed % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", mins, secs),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRecording) MegaRed else CyanPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Microphone Record / Stop Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .then(if (isRecording) Modifier.scale(pulseScale) else Modifier)
                            .clip(CircleShape)
                            .background(if (isRecording) MegaRed else CyanPrimary.copy(alpha = 0.2f))
                            .border(2.dp, if (isRecording) MegaRed else CyanPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    val file = recorderManager.stopRecording()
                                    recordedFile = file
                                    isRecording = false
                                } else {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    val file = recorderManager.startRecording("memo_$timestamp.m4a")
                                    recordedFile = file
                                    isRecording = true
                                    isPlayingPreview = false
                                }
                            },
                            modifier = Modifier.size(70.dp).testTag("record_audio_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop" else "Record",
                                tint = if (isRecording) Color.White else CyanPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isRecording) "Recording live voice memo..." else if (recordedFile != null) "Recording saved! You can preview or upload." else "Tap the microphone to record",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Audio Preview & Info
                    if (recordedFile != null && !isRecording) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlayingPreview) {
                                            recorderManager.stopPreview()
                                            isPlayingPreview = false
                                        } else {
                                            isPlayingPreview = true
                                            recorderManager.playPreview(recordedFile!!) {
                                                isPlayingPreview = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = SecurityGreen
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recordedFile!!.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Size: ${formatBytes(recordedFile!!.length())}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (recordedFile != null && !isRecording) {
                Button(
                    onClick = {
                        val file = recordedFile!!
                        onUploadRecording(file, file.name)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("upload_voice_memo_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color(0xFF00202E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Upload to Cloud",
                        color = Color(0xFF00202E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    recorderManager.cancelRecording()
                    recorderManager.stopPreview()
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
