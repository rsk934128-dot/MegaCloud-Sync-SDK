package com.example.model

enum class TransferType {
    UPLOAD,
    DOWNLOAD
}

enum class TransferStatus {
    QUEUED,
    TRANSFERRING,
    PAUSED,
    COMPLETED,
    FAILED
}

enum class ThreatLevel(val label: String) {
    SAFE("Safe"),
    LOW("Low Risk"),
    MEDIUM("Medium Risk"),
    CRITICAL("Critical Risk")
}

data class CloudAccount(
    val provider: String = "MEGA Cloud",
    val email: String = "user@mega.nz",
    val sessionToken: String = "m_sess_89f0293da82c1",
    val quotaUsedBytes: Long = 18_420_000_000L, // ~18.42 GB
    val quotaTotalBytes: Long = 50_000_000_000L, // 50 GB Free Tier
    val isConnected: Boolean = true,
    val isEncrypted: Boolean = true,
    val lastSyncTime: Long = System.currentTimeMillis()
)

data class CloudFileItem(
    val id: String,
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val sizeBytes: Long,
    val modifiedEpochMs: Long,
    val isEncrypted: Boolean = true,
    val publicShareUrl: String? = null,
    val category: String = "GENERAL" // "APK", "AUDIT", "DOCUMENT", "GENERAL"
)

data class TransferItem(
    val id: String,
    val title: String,
    val type: TransferType,
    val sizeBytes: Long,
    val bytesTransferred: Long,
    val progressPercent: Float,
    val speedBytesPerSec: Long,
    val status: TransferStatus,
    val localPath: String = "",
    val remotePath: String = "",
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppAuditItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkSizeBytes: Long,
    val sourceDir: String,
    val isSystemApp: Boolean,
    val targetSdk: Int,
    val dangerousPermissions: List<String>,
    val threatLevel: ThreatLevel,
    val threatScore: Int, // 0 - 100
    val signatureValid: Boolean = true,
    val lastAuditTimestamp: Long = System.currentTimeMillis(),
    val isBackedUpToCloud: Boolean = false,
    val cloudBackupUrl: String? = null,
    val securityFlags: List<String> = emptyList()
)

data class ThreatAuditReport(
    val reportId: String,
    val scanDateEpochMs: Long,
    val totalAppsScanned: Int,
    val safeCount: Int,
    val lowRiskCount: Int,
    val mediumRiskCount: Int,
    val criticalRiskCount: Int,
    val overallThreatScore: Int,
    val keyFindings: List<String>,
    val isSyncedToCloud: Boolean,
    val cloudSyncUrl: String? = null
)

data class CloudSyncSettings(
    val syncOnWifiOnly: Boolean = false,
    val autoBackupThreatReports: Boolean = true,
    val autoBackupHighRiskApks: Boolean = false,
    val clientSideAesEncryption: Boolean = true,
    val liveEndpointUrl: String = "https://g.api.mega.co.nz/cs",
    val apiKey: String = "apppulse-mega-v3-enterprise"
)
