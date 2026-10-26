package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.MegaRed
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen

data class StorageSlice(
    val label: String,
    val bytes: Long,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageInsightsCard(
    megaUsedBytes: Long = 18_420_000_000L,
    megaTotalBytes: Long = 50_000_000_000L,
    firebaseUsedBytes: Long = 1_390_000_000L,
    firebaseTotalBytes: Long = 5_000_000_000L,
    modifier: Modifier = Modifier
) {
    var selectedProviderIndex by remember { mutableIntStateOf(0) } // 0: MEGA, 1: Firebase

    val (currentUsed, currentTotal, slices) = if (selectedProviderIndex == 0) {
        val s = listOf(
            StorageSlice("APK Vault", 8_400_000_000L, CyanPrimary),
            StorageSlice("Voice Memos", 3_220_000_000L, ElectricPurple),
            StorageSlice("Audit Reports", 1_800_000_000L, SecurityGreen),
            StorageSlice("Other Documents", 5_000_000_000L, SecurityAmber)
        )
        Triple(megaUsedBytes, megaTotalBytes, s)
    } else {
        val s = listOf(
            StorageSlice("APK Builds", 820_000_000L, MegaRed),
            StorageSlice("User Vault", 450_000_000L, CyanPrimary),
            StorageSlice("Audit Logs", 120_000_000L, SecurityGreen)
        )
        Triple(firebaseUsedBytes, firebaseTotalBytes, s)
    }

    val usedFraction = (currentUsed.toFloat() / currentTotal).coerceIn(0f, 1f)
    val usedPercentage = (usedFraction * 100).toInt()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("storage_insights_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Storage Usage & Insights",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Live telemetry of cloud quotas & distribution",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Switcher Tabs
            TabRow(
                selectedTabIndex = selectedProviderIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedProviderIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedProviderIndex == 0,
                    onClick = { selectedProviderIndex = 0 },
                    text = {
                        Text(
                            text = "MEGA Drive (50 GB)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedProviderIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedProviderIndex == 1,
                    onClick = { selectedProviderIndex = 1 },
                    text = {
                        Text(
                            text = "Firebase Bucket (5 GB)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedProviderIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Donut Chart + Center Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

                    Canvas(modifier = Modifier.size(140.dp)) {
                        val strokeWidth = 20.dp.toPx()
                        val arcSize = size.width - strokeWidth
                        val arcOffset = strokeWidth / 2

                        // Background empty circle
                        drawArc(
                            color = outlineColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(arcOffset, arcOffset),
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth)
                        )

                        // Slices
                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sliceFraction = (slice.bytes.toFloat() / currentTotal).coerceIn(0f, 1f)
                            val sweepAngle = sliceFraction * 360f
                            if (sweepAngle > 0.5f) {
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(arcOffset, arcOffset),
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }

                    // Inside Donut Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$usedPercentage%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Used",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Breakdown metrics
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Total Quota Usage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${formatBytes(currentUsed)} / ${formatBytes(currentTotal)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Available Free Space:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(currentTotal - currentUsed),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecurityGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    val fraction = (slice.bytes.toFloat() / currentTotal * 100).toInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${slice.label}: ${formatBytes(slice.bytes)} ($fraction%)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
