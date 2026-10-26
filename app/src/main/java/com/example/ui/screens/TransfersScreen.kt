package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransferItem
import com.example.model.TransferStatus
import com.example.model.TransferType
import com.example.ui.components.FileSyncStatusWidget
import com.example.ui.components.formatBytes
import com.example.ui.components.formatSpeed
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MegaRed
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.viewmodel.AppPulseViewModel

@Composable
fun TransfersScreen(
    viewModel: AppPulseViewModel,
    modifier: Modifier = Modifier
) {
    val transfers by viewModel.transfers.collectAsState()
    val activeCount = transfers.count { it.status == TransferStatus.TRANSFERRING }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Transfer & Sync Manager",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (activeCount > 0) "$activeCount transfer(s) currently in progress" else "All cloud transfers idle",
                    fontSize = 12.sp,
                    color = if (activeCount > 0) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { viewModel.clearCompletedTransfers() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("clear_completed_transfers_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Done", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Live Real-Time File Synchronization Status Widget with Animated Text & Progress
        FileSyncStatusWidget(
            transfers = transfers,
            onPauseTransfer = { id -> viewModel.pauseTransfer(id) },
            onResumeTransfer = { id -> viewModel.resumeTransfer(id) },
            onForceSync = {
                viewModel.showSnackbar("Cloud sync engine active • Integrity verified.")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (transfers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Transfer queue is empty",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Upload files or backup APKs to see real-time sync progress here.",
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
                items(transfers, key = { it.id }) { item ->
                    TransferItemRow(item = item, viewModel = viewModel)
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun TransferItemRow(
    item: TransferItem,
    viewModel: AppPulseViewModel
) {
    val isUpload = item.type == TransferType.UPLOAD

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .testTag("transfer_row_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isUpload) CyanPrimary.copy(alpha = 0.15f) else SecurityGreen.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUpload) Icons.Default.CloudUpload else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (isUpload) CyanPrimary else SecurityGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.remotePath,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Icon / Controls
                when (item.status) {
                    TransferStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = SecurityGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    TransferStatus.FAILED -> {
                        IconButton(
                            onClick = { viewModel.resumeTransfer(item.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = MegaRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    TransferStatus.TRANSFERRING -> {
                        IconButton(
                            onClick = { viewModel.pauseTransfer(item.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = SecurityAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    TransferStatus.PAUSED -> {
                        IconButton(
                            onClick = { viewModel.resumeTransfer(item.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    TransferStatus.QUEUED -> {}
                }

                IconButton(
                    onClick = { viewModel.cancelTransfer(item.id) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { item.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = when (item.status) {
                    TransferStatus.COMPLETED -> SecurityGreen
                    TransferStatus.FAILED -> MegaRed
                    TransferStatus.PAUSED -> SecurityAmber
                    else -> CyanPrimary
                },
                trackColor = Color(0xFF223554)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress text and speed
            Row(modifier = Modifier.fillMaxWidth()) {
                val transferredStr = formatBytes(item.bytesTransferred)
                val totalStr = formatBytes(item.sizeBytes)
                val percentInt = (item.progressPercent * 100).toInt()

                Text(
                    text = "$transferredStr / $totalStr ($percentInt%)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                if (item.status == TransferStatus.TRANSFERRING && item.speedBytesPerSec > 0) {
                    Text(
                        text = formatSpeed(item.speedBytesPerSec),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                } else {
                    Text(
                        text = item.status.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (item.status) {
                            TransferStatus.COMPLETED -> SecurityGreen
                            TransferStatus.FAILED -> MegaRed
                            TransferStatus.PAUSED -> SecurityAmber
                            else -> CyanPrimary
                        }
                    )
                }
            }
        }
    }
}
