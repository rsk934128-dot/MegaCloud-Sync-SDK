package com.example.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.repository.FirebaseRepository
import com.example.manager.ApkSecurityScanner
import com.example.manager.MegaCloudManager
import com.example.manager.TransferQueueManager
import com.example.model.AppAuditItem
import com.example.model.CloudAccount
import com.example.model.CloudFileItem
import com.example.model.CloudSyncSettings
import com.example.model.ThreatAuditReport
import com.example.model.ThreatLevel
import com.example.model.TransferItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppPulseRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val megaCloudManager = MegaCloudManager(context)
    val apkSecurityScanner = ApkSecurityScanner(context)
    val transferQueueManager = TransferQueueManager(megaCloudManager, scope)
    val firebaseRepository = FirebaseRepository(context)

    private val _installedApps = MutableStateFlow<List<AppAuditItem>>(emptyList())
    val installedApps: StateFlow<List<AppAuditItem>> = _installedApps.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _cloudFiles = MutableStateFlow<List<CloudFileItem>>(emptyList())
    val cloudFiles: StateFlow<List<CloudFileItem>> = _cloudFiles.asStateFlow()

    private val _currentCloudPath = MutableStateFlow("/")
    val currentCloudPath: StateFlow<String> = _currentCloudPath.asStateFlow()

    private val _auditReports = MutableStateFlow<List<ThreatAuditReport>>(emptyList())
    val auditReports: StateFlow<List<ThreatAuditReport>> = _auditReports.asStateFlow()

    private val _cloudAccount = MutableStateFlow(CloudAccount())
    val cloudAccount: StateFlow<CloudAccount> = _cloudAccount.asStateFlow()

    private val _settings = MutableStateFlow(CloudSyncSettings())
    val settings: StateFlow<CloudSyncSettings> = _settings.asStateFlow()

    val transfers: StateFlow<List<TransferItem>> = transferQueueManager.transfers

    private var firestoreObservationJob: Job? = null

    init {
        // Initial load
        refreshApps()
        loadCloudFiles("/")

        FirebaseAuth.getInstance().currentUser?.let { user ->
            onUserSignedIn(user)
        }
    }

    fun onUserSignedIn(user: FirebaseUser) {
        _cloudAccount.value = _cloudAccount.value.copy(
            email = user.email ?: _cloudAccount.value.email,
            provider = "Google Cloud & MEGA Sync"
        )
        scope.launch {
            firebaseRepository.syncUserProfile(user)
            startObservingFirestore()
        }
    }

    private fun startObservingFirestore() {
        firestoreObservationJob?.cancel()
        firestoreObservationJob = scope.launch {
            firebaseRepository.observeAuditReports().collect { firestoreReports ->
                if (firestoreReports.isNotEmpty()) {
                    val converted = firestoreReports.map { fs ->
                        ThreatAuditReport(
                            reportId = fs.reportId,
                            scanDateEpochMs = fs.scanDateEpochMs,
                            totalAppsScanned = fs.totalAppsScanned,
                            safeCount = fs.safeCount,
                            lowRiskCount = fs.lowRiskCount,
                            mediumRiskCount = fs.mediumRiskCount,
                            criticalRiskCount = fs.criticalRiskCount,
                            overallThreatScore = fs.overallThreatScore,
                            keyFindings = fs.keyFindings,
                            isSyncedToCloud = true,
                            cloudSyncUrl = fs.cloudSyncUrl
                        )
                    }
                    _auditReports.value = converted
                }
            }
        }
    }

    fun refreshApps() {
        scope.launch {
            _isScanning.value = true
            val apps = apkSecurityScanner.scanInstalledApps()
            _installedApps.value = apps
            _isScanning.value = false

            // Auto-generate current baseline report if empty
            if (_auditReports.value.isEmpty()) {
                val report = apkSecurityScanner.generateThreatReport(apps)
                _auditReports.value = listOf(report)
            }
        }
    }

    fun runFullAudit() {
        scope.launch {
            _isScanning.value = true
            val apps = apkSecurityScanner.scanInstalledApps()
            _installedApps.value = apps
            val report = apkSecurityScanner.generateThreatReport(apps)
            _auditReports.value = listOf(report) + _auditReports.value
            _isScanning.value = false

            if (_settings.value.autoBackupThreatReports) {
                syncAuditReportToCloud(report)
            }
        }
    }

    fun loadCloudFiles(path: String = _currentCloudPath.value) {
        scope.launch {
            _currentCloudPath.value = path
            val files = megaCloudManager.getCloudFiles(path)
            _cloudFiles.value = files
        }
    }

    fun navigateToFolder(path: String) {
        loadCloudFiles(path)
    }

    fun navigateUp() {
        val current = _currentCloudPath.value
        if (current == "/") return
        val parent = current.substringBeforeLast("/")
        val target = if (parent.isEmpty()) "/" else parent
        loadCloudFiles(target)
    }

    fun createCloudFolder(name: String) {
        scope.launch {
            megaCloudManager.createFolder(name, _currentCloudPath.value)
            loadCloudFiles(_currentCloudPath.value)
        }
    }

    fun deleteCloudFile(id: String) {
        scope.launch {
            megaCloudManager.deleteFile(id)
            loadCloudFiles(_currentCloudPath.value)
        }
    }

    fun backupApkToCloud(app: AppAuditItem) {
        scope.launch {
            val apkFile = apkSecurityScanner.extractApkToCache(app)
            transferQueueManager.enqueueUpload(
                file = apkFile,
                remoteDirectory = "/APK_Vault",
                category = "APK",
                authToken = _cloudAccount.value.sessionToken,
                uploadEndpoint = _settings.value.liveEndpointUrl
            ) { uploadedItem ->
                if (uploadedItem != null) {
                    _installedApps.value = _installedApps.value.map {
                        if (it.packageName == app.packageName) {
                            it.copy(
                                isBackedUpToCloud = true,
                                cloudBackupUrl = uploadedItem.publicShareUrl
                            )
                        } else it
                    }
                    loadCloudFiles(_currentCloudPath.value)

                    // Also mirror to Firebase Firestore if authenticated
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        scope.launch {
                            try {
                                firebaseRepository.saveBackedUpApk(app, uploadedItem.publicShareUrl ?: "")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    fun syncAuditReportToCloud(report: ThreatAuditReport) {
        scope.launch {
            val file = apkSecurityScanner.exportReportToFile(report, _installedApps.value)
            transferQueueManager.enqueueUpload(
                file = file,
                remoteDirectory = "/Security_Audits",
                category = "AUDIT",
                authToken = _cloudAccount.value.sessionToken,
                uploadEndpoint = _settings.value.liveEndpointUrl
            ) { uploadedItem ->
                if (uploadedItem != null) {
                    _auditReports.value = _auditReports.value.map {
                        if (it.reportId == report.reportId) {
                            it.copy(isSyncedToCloud = true, cloudSyncUrl = uploadedItem.publicShareUrl)
                        } else it
                    }
                    loadCloudFiles(_currentCloudPath.value)

                    // Also mirror to Firebase Firestore if authenticated
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        scope.launch {
                            try {
                                firebaseRepository.saveAuditReport(report.copy(isSyncedToCloud = true, cloudSyncUrl = uploadedItem.publicShareUrl))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    fun syncAllCriticalApks() {
        val criticalApps = _installedApps.value.filter { it.threatLevel == ThreatLevel.CRITICAL && !it.isBackedUpToCloud }
        criticalApps.forEach { backupApkToCloud(it) }
    }

    fun downloadAndInstallApk(item: CloudFileItem, onReadyToInstall: (File) -> Unit) {
        scope.launch {
            val downloadsDir = File(context.filesDir, "cloud_downloads").apply { mkdirs() }
            val destFile = File(downloadsDir, item.name)
            transferQueueManager.enqueueDownload(item, destFile) { downloadedFile ->
                if (downloadedFile != null) {
                    onReadyToInstall(downloadedFile)
                }
            }
        }
    }

    fun installApk(file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateAccount(account: CloudAccount) {
        _cloudAccount.value = account
    }

    fun updateSettings(settings: CloudSyncSettings) {
        _settings.value = settings
    }
}
