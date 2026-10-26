package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.model.FirebaseStorageItem
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.MegaRed
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import java.util.Locale

/**
 * Data model representing an individual category segment in the Donut Chart.
 */
data class BucketCategoryStats(
    val categoryId: String,
    val label: String,
    val count: Int,
    val bytes: Long,
    val color: Color
)

/**
 * High-performance, animated Donut Chart rendered using Jetpack Compose Canvas.
 * Displays real-time storage telemetry, quota usage, and breakdown by file types
 * for the Firebase Storage bucket.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirebaseBucketDonutChart(
    items: List<FirebaseStorageItem>,
    modifier: Modifier = Modifier,
    totalQuotaBytes: Long = 5_368_709_120L, // 5 GB default Firebase free tier quota
    bucketName: String = "Firebase Cloud Storage"
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }
    var animationPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "donut_animation"
    )

    // Compute real-time statistics from bucket items
    val fileItems = items.filter { !it.isFolder }
    val totalUsedBytes = fileItems.sumOf { it.sizeBytes }

    // Categorize files
    val apkFiles = fileItems.filter { it.name.endsWith(".apk", ignoreCase = true) }
    val audioFiles = fileItems.filter {
        it.name.endsWith(".m4a", true) || it.name.endsWith(".mp3", true) || it.name.endsWith(".wav", true) ||
                it.contentType?.startsWith("audio/") == true
    }
    val docFiles = fileItems.filter {
        it.name.endsWith(".json", true) || it.name.endsWith(".pdf", true) || it.name.endsWith(".txt", true) ||
                it.contentType == "application/json"
    }
    val otherFiles = fileItems.filter {
        !apkFiles.contains(it) && !audioFiles.contains(it) && !docFiles.contains(it)
    }

    // Build category slices
    val categories = remember(fileItems) {
        val list = mutableListOf<BucketCategoryStats>()
        if (apkFiles.isNotEmpty()) {
            list.add(BucketCategoryStats("apk", "APK Packages", apkFiles.size, apkFiles.sumOf { it.sizeBytes }, SecurityAmber))
        }
        if (audioFiles.isNotEmpty()) {
            list.add(BucketCategoryStats("audio", "Audio Vault", audioFiles.size, audioFiles.sumOf { it.sizeBytes }, SecurityGreen))
        }
        if (docFiles.isNotEmpty()) {
            list.add(BucketCategoryStats("docs", "Audit Reports", docFiles.size, docFiles.sumOf { it.sizeBytes }, MegaRed))
        }
        if (otherFiles.isNotEmpty()) {
            list.add(BucketCategoryStats("other", "Other Data", otherFiles.size, otherFiles.sumOf { it.sizeBytes }, CyanPrimary))
        }

        // If bucket is currently empty or initial load, provide clear breakdown
        if (list.isEmpty()) {
            listOf(
                BucketCategoryStats("empty", "Available Quota", 0, totalQuotaBytes, CyanPrimary.copy(alpha = 0.5f))
            )
        } else {
            list
        }
    }

    val usedPercentage = if (totalQuotaBytes > 0) {
        (totalUsedBytes.toDouble() / totalQuotaBytes.toDouble() * 100).coerceIn(0.0, 100.0)
    } else 0.0

    val remainingBytes = (totalQuotaBytes - totalUsedBytes).coerceAtLeast(0L)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("firebase_donut_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanPrimary.copy(alpha = 0.15f))
                        .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Chart",
                        tint = CyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Storage Donut Analytics",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SecurityGreen.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "REAL-TIME",
                                color = SecurityGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Live Compose Canvas distribution of bucket assets",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp).testTag("toggle_donut_chart_btn")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = CyanPrimary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Donut Chart Display Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Canvas Donut Chart Box
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clickable { selectedCategoryIndex = null }
                                .testTag("canvas_donut_box"),
                            contentAlignment = Alignment.Center
                        ) {
                            val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                            Canvas(modifier = Modifier.size(140.dp)) {
                                val strokeWidth = 20.dp.toPx()
                                val arcSize = size.width - strokeWidth
                                val arcOffset = strokeWidth / 2

                                // Background circular track
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(arcOffset, arcOffset),
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth)
                                )

                                var startAngle = -90f

                                if (totalUsedBytes == 0L || categories.all { it.categoryId == "empty" }) {
                                    // Empty state: show subtle animated arc indicating ready state
                                    drawArc(
                                        color = CyanPrimary.copy(alpha = 0.4f),
                                        startAngle = startAngle,
                                        sweepAngle = 360f * animatedProgress,
                                        useCenter = false,
                                        topLeft = Offset(arcOffset, arcOffset),
                                        size = Size(arcSize, arcSize),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                } else {
                                    categories.forEachIndexed { index, cat ->
                                        val sliceFraction = if (totalUsedBytes > 0) {
                                            (cat.bytes.toDouble() / totalUsedBytes.toDouble()).toFloat()
                                        } else 0f

                                        val targetSweep = sliceFraction * 360f * animatedProgress
                                        val isSelected = selectedCategoryIndex == index
                                        val currentStrokeWidth = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth

                                        if (targetSweep > 1f) {
                                            drawArc(
                                                color = if (selectedCategoryIndex == null || isSelected) cat.color else cat.color.copy(alpha = 0.35f),
                                                startAngle = startAngle,
                                                sweepAngle = targetSweep,
                                                useCenter = false,
                                                topLeft = Offset(arcOffset, arcOffset),
                                                size = Size(arcSize, arcSize),
                                                style = Stroke(
                                                    width = currentStrokeWidth,
                                                    cap = StrokeCap.Round
                                                )
                                            )
                                            startAngle += targetSweep
                                        }
                                    }
                                }
                            }

                            // Donut Center Cutout Text
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                val focusedCategory = selectedCategoryIndex?.let { categories.getOrNull(it) }

                                if (focusedCategory != null) {
                                    Text(
                                        text = formatBytes(focusedCategory.bytes),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = focusedCategory.color
                                    )
                                    Text(
                                        text = focusedCategory.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    val catFraction = if (totalUsedBytes > 0) (focusedCategory.bytes * 100 / totalUsedBytes) else 0
                                    Text(
                                        text = "$catFraction% of used",
                                        fontSize = 9.sp,
                                        color = focusedCategory.color
                                    )
                                } else {
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", usedPercentage),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Quota Used",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${fileItems.size} Files",
                                        fontSize = 9.sp,
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        // Real-Time Quota Telemetry Metrics
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Bucket Quota Allocation",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "${formatBytes(totalUsedBytes)} / ${formatBytes(totalQuotaBytes)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SecurityGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Available: ${formatBytes(remainingBytes)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SecurityGreen
                                )
                            }

                            Text(
                                text = "Target: $bucketName",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interactive Category Breakdown Chips
                    Text(
                        text = "Categorized Breakdown (Tap to inspect):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEachIndexed { index, cat ->
                            if (cat.categoryId != "empty") {
                                val isSelected = selectedCategoryIndex == index
                                val percentage = if (totalUsedBytes > 0) (cat.bytes.toDouble() / totalUsedBytes.toDouble() * 100).toInt() else 0

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) cat.color.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) cat.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedCategoryIndex = if (isSelected) null else index
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("donut_category_${cat.categoryId}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${cat.label} (${cat.count})",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${formatBytes(cat.bytes)} ($percentage%)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cat.color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
