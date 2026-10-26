package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransferItem
import com.example.model.TransferStatus
import com.example.model.TransferType
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.MegaRed
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import kotlinx.coroutines.delay

/**
 * Modern real-time file synchronization status widget.
 * Features animated text transitions, an animated progress bar with glowing gradient sweep,
 * live transfer telemetry (speed, ETA, bytes transferred), and interactive pause/resume triggers.
 */
@Composable
fun FileSyncStatusWidget(
    transfers: List<TransferItem>,
    modifier: Modifier = Modifier,
    onPauseTransfer: ((String) -> Unit)? = null,
    onResumeTransfer: ((String) -> Unit)? = null,
    onForceSync: (() -> Unit)? = null
) {
    val activeTransfers = transfers.filter { it.status == TransferStatus.TRANSFERRING }
    val queuedTransfers = transfers.filter { it.status == TransferStatus.QUEUED }
    val pausedTransfers = transfers.filter { it.status == TransferStatus.PAUSED }
    val failedTransfers = transfers.filter { it.status == TransferStatus.FAILED }
    val completedTransfers = transfers.filter { it.status == TransferStatus.COMPLETED }

    val isSyncing = activeTransfers.isNotEmpty()
    val isPaused = !isSyncing && pausedTransfers.isNotEmpty()
    val hasFailed = failedTransfers.isNotEmpty()

    // Aggregate progress calculation
    val totalActiveBytes = activeTransfers.sumOf { it.sizeBytes }
    val totalTransferredBytes = activeTransfers.sumOf { it.bytesTransferred }
    val aggregatedSpeed = activeTransfers.sumOf { it.speedBytesPerSec }

    val rawProgress = if (totalActiveBytes > 0) {
        (totalTransferredBytes.toFloat() / totalActiveBytes.toFloat()).coerceIn(0f, 1f)
    } else if (isSyncing) 0.15f else 1f

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "sync_progress_anim"
    )

    // Infinite breathing/rotation animations for active sync
    val infiniteTransition = rememberInfiniteTransition(label = "sync_pulse_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    // Animated text ticker for subtitle feedback
    var tickerIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(isSyncing, activeTransfers.size) {
        if (isSyncing) {
            while (true) {
                delay(3000)
                tickerIndex = (tickerIndex + 1) % 3
            }
        }
    }

    // Determine current primary active item
    val primaryTransfer = activeTransfers.firstOrNull()

    // Formatting ETA
    val etaSeconds = if (aggregatedSpeed > 0 && totalActiveBytes > totalTransferredBytes) {
        (totalActiveBytes - totalTransferredBytes) / aggregatedSpeed
    } else 0L

    val etaText = when {
        etaSeconds in 1..59 -> "${etaSeconds}s remaining"
        etaSeconds in 60..3599 -> "${etaSeconds / 60}m ${etaSeconds % 60}s remaining"
        etaSeconds >= 3600 -> "${etaSeconds / 3600}h ${(etaSeconds % 3600) / 60}m remaining"
        else -> "Calculating ETA..."
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when {
                    isSyncing -> CyanPrimary.copy(alpha = 0.5f)
                    hasFailed -> MegaRed.copy(alpha = 0.4f)
                    isPaused -> SecurityAmber.copy(alpha = 0.4f)
                    else -> SecurityGreen.copy(alpha = 0.35f)
                },
                RoundedCornerShape(16.dp)
            )
            .testTag("file_sync_status_widget")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Status Icon, Animated Badge & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon Container with animated state
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSyncing -> CyanPrimary.copy(alpha = 0.15f)
                                hasFailed -> MegaRed.copy(alpha = 0.15f)
                                isPaused -> SecurityAmber.copy(alpha = 0.15f)
                                else -> SecurityGreen.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isSyncing -> CyanPrimary.copy(alpha = 0.4f)
                                hasFailed -> MegaRed.copy(alpha = 0.4f)
                                isPaused -> SecurityAmber.copy(alpha = 0.4f)
                                else -> SecurityGreen.copy(alpha = 0.4f)
                            },
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isSyncing -> {
                            Icon(
                                imageVector = if (primaryTransfer?.type == TransferType.DOWNLOAD) Icons.Default.CloudDownload else Icons.Default.CloudUpload,
                                contentDescription = "Syncing",
                                tint = CyanPrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(pulseScale)
                            )
                        }
                        hasFailed -> {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Failed",
                                tint = MegaRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        isPaused -> {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Paused",
                                tint = SecurityAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Synced",
                                tint = SecurityGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Real-time Badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cloud Sync Engine",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isSyncing -> CyanPrimary.copy(alpha = 0.2f)
                                        hasFailed -> MegaRed.copy(alpha = 0.2f)
                                        isPaused -> SecurityAmber.copy(alpha = 0.2f)
                                        else -> SecurityGreen.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSyncing) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier
                                            .size(10.dp)
                                            .rotate(rotationAngle)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = when {
                                        isSyncing -> "SYNCING (${activeTransfers.size})"
                                        hasFailed -> "ATTENTION NEEDED"
                                        isPaused -> "SYNC PAUSED"
                                        else -> "UP TO DATE"
                                    },
                                    color = when {
                                        isSyncing -> CyanPrimary
                                        hasFailed -> MegaRed
                                        isPaused -> SecurityAmber
                                        else -> SecurityGreen
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Real-Time Animated Primary Status Line
                    AnimatedContent(
                        targetState = when {
                            isSyncing && primaryTransfer != null -> {
                                val actionVerb = if (primaryTransfer.type == TransferType.UPLOAD) "Uploading" else "Downloading"
                                "$actionVerb: ${primaryTransfer.title}"
                            }
                            isPaused -> "File transfers temporarily paused"
                            hasFailed -> "${failedTransfers.size} sync task(s) failed"
                            else -> "All cloud files & audits are synchronized"
                        },
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "sync_primary_text"
                    ) { animatedText ->
                        Text(
                            text = animatedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                isSyncing -> CyanPrimary
                                hasFailed -> MegaRed
                                isPaused -> SecurityAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            modifier = Modifier.testTag("sync_animated_primary_text")
                        )
                    }
                }

                // Quick Header Action Button
                if (isSyncing && primaryTransfer != null && onPauseTransfer != null) {
                    IconButton(
                        onClick = { onPauseTransfer(primaryTransfer.id) },
                        modifier = Modifier.size(32.dp).testTag("pause_primary_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = SecurityAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isPaused && onResumeTransfer != null && pausedTransfers.isNotEmpty()) {
                    IconButton(
                        onClick = { onResumeTransfer(pausedTransfers.first().id) },
                        modifier = Modifier.size(32.dp).testTag("resume_primary_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = SecurityGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (!isSyncing && onForceSync != null) {
                    IconButton(
                        onClick = onForceSync,
                        modifier = Modifier.size(32.dp).testTag("force_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Force Sync",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Linear Progress Indicator Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .testTag("sync_progress_bar")
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSyncing) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        CyanPrimary,
                                        ElectricPurple,
                                        CyanPrimary
                                    ),
                                    startX = shimmerOffset * 500f,
                                    endX = (shimmerOffset + 1f) * 500f
                                )
                            } else if (hasFailed) {
                                Brush.horizontalGradient(listOf(MegaRed, SecurityAmber))
                            } else if (isPaused) {
                                Brush.horizontalGradient(listOf(SecurityAmber, SecurityAmber))
                            } else {
                                Brush.horizontalGradient(listOf(SecurityGreen, CyanPrimary))
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Metrics & Animated Ticker Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left text: Animated secondary telemetry feedback
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = when {
                            isSyncing -> {
                                when (tickerIndex) {
                                    0 -> "Transferred: ${formatBytes(totalTransferredBytes)} of ${formatBytes(totalActiveBytes)}"
                                    1 -> "Speed: ${formatSpeed(aggregatedSpeed)}"
                                    else -> etaText
                                }
                            }
                            isPaused -> "Paused at ${formatBytes(totalTransferredBytes)} (${(animatedProgress * 100).toInt()}%)"
                            hasFailed -> "Error in sync process • Tap retry"
                            else -> "Verified healthy • Last synced just now"
                        },
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "sync_secondary_text"
                    ) { feedbackText ->
                        Text(
                            text = feedbackText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right text: Percentage Indicator
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        isSyncing -> CyanPrimary
                        hasFailed -> MegaRed
                        isPaused -> SecurityAmber
                        else -> SecurityGreen
                    }
                )
            }

            // Optional Queue Details Row when active
            if (isSyncing || queuedTransfers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active: ${activeTransfers.size} • Queued: ${queuedTransfers.size} • Completed: ${completedTransfers.size}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (aggregatedSpeed > 0) {
                        Text(
                            text = formatSpeed(aggregatedSpeed),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                }
            }
        }
    }
}
