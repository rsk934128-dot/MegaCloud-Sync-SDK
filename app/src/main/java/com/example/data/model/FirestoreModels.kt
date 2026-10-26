package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreAuditReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reportId: String = "",
    val scanDateEpochMs: Long = 0L,
    val totalAppsScanned: Int = 0,
    val safeCount: Int = 0,
    val lowRiskCount: Int = 0,
    val mediumRiskCount: Int = 0,
    val criticalRiskCount: Int = 0,
    val overallThreatScore: Int = 0,
    val keyFindings: List<String> = emptyList(),
    val isSyncedToCloud: Boolean = false,
    val cloudSyncUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreBackedUpApk(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val versionName: String = "",
    val apkSizeBytes: Long = 0L,
    val threatScore: Int = 0,
    val threatLevel: String = "",
    val cloudStorageUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
