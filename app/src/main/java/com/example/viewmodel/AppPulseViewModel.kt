package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppAuditItem
import com.example.model.CloudAccount
import com.example.model.CloudFileItem
import com.example.model.CloudSyncSettings
import com.example.model.ThreatAuditReport
import com.example.model.TransferItem
import com.example.repository.AppPulseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class NavTab(val title: String) {
    DASHBOARD("Dashboard"),
    CLOUD_DRIVE("MEGA Drive"),
    FIREBASE_STORAGE("Firebase Bucket"),
    APK_AUDIT("Security Audit"),
    TRANSFERS("Transfers"),
    SETTINGS("Settings")
}

class AppPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppPulseRepository(application, viewModelScope)

    val isScanning: StateFlow<Boolean> = repository.isScanning
    val cloudFiles: StateFlow<List<CloudFileItem>> = repository.cloudFiles
    val currentCloudPath: StateFlow<String> = repository.currentCloudPath
    val auditReports: StateFlow<List<ThreatAuditReport>> = repository.auditReports
    val cloudAccount: StateFlow<CloudAccount> = repository.cloudAccount
    val settings: StateFlow<CloudSyncSettings> = repository.settings
    val transfers: StateFlow<List<TransferItem>> = repository.transfers

    // Firebase Auth user session
    val currentUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    )

    // Theme & Personalization State
    val selectedAccentOption = MutableStateFlow(com.example.ui.theme.AccentColorOption.CYAN)
    val isOledMode = MutableStateFlow(false)

    // Security & Vault Manager
    val vaultSecurityManager = com.example.manager.VaultSecurityManager(application)
    val isPinProtectionEnabled = MutableStateFlow(vaultSecurityManager.isPinProtectionEnabled())

    // UI State
    val selectedTab = MutableStateFlow(NavTab.DASHBOARD)
    val appSearchQuery = MutableStateFlow("")
    val filterCriticalOnly = MutableStateFlow(false)
    val selectedAppForDetail = MutableStateFlow<AppAuditItem?>(null)
    val shareLinkDialogUrl = MutableStateFlow<String?>(null)
    val showCreateFolderDialog = MutableStateFlow(false)
    val showUploadDialog = MutableStateFlow(false)
    val showAccountDialog = MutableStateFlow(false)
    val showReportDetail = MutableStateFlow<ThreatAuditReport?>(null)
    val snackbarMessage = MutableStateFlow<String?>(null)

    fun setAccentColor(option: com.example.ui.theme.AccentColorOption) {
        selectedAccentOption.value = option
        showSnackbar("Applied accent theme: ${option.label}")
    }

    fun setOledMode(enabled: Boolean) {
        isOledMode.value = enabled
        showSnackbar(if (enabled) "OLED Pure Black enabled" else "Cyber Dark theme enabled")
    }

    fun togglePinProtection(enabled: Boolean) {
        vaultSecurityManager.setPinProtectionEnabled(enabled)
        isPinProtectionEnabled.value = enabled
        showSnackbar(if (enabled) "Vault PIN protection activated" else "Vault PIN protection disabled")
    }

    fun changeVaultPin(newPin: String): Boolean {
        val success = vaultSecurityManager.updatePin(newPin)
        if (success) {
            showSnackbar("Vault security PIN updated successfully.")
        } else {
            showSnackbar("Invalid PIN. Must be 4 digits.")
        }
        return success
    }

    // Filtered apps
    val filteredApps: StateFlow<List<AppAuditItem>> = combine(
        repository.installedApps,
        appSearchQuery,
        filterCriticalOnly
    ) { apps, query, criticalOnly ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            val matchesCritical = !criticalOnly || app.threatScore >= 45
            matchesQuery && matchesCritical
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: NavTab) {
        selectedTab.value = tab
    }

    fun showSnackbar(msg: String) {
        snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }

    fun refreshApps() {
        repository.refreshApps()
        showSnackbar("Scanning installed applications...")
    }

    fun runFullThreatAudit() {
        repository.runFullAudit()
        showSnackbar("Running in-depth APK security & permission audit...")
    }

    fun backupApkToCloud(app: AppAuditItem) {
        repository.backupApkToCloud(app)
        showSnackbar("Backing up ${app.appName} to MEGA Cloud (/APK_Vault)...")
    }

    fun syncAllCriticalApks() {
        repository.syncAllCriticalApks()
        showSnackbar("Enqueued all high-risk APKs for cloud backup.")
    }

    fun syncAuditReportToCloud(report: ThreatAuditReport) {
        repository.syncAuditReportToCloud(report)
        showSnackbar("Syncing Audit Report ${report.reportId} to MEGA Drive...")
    }

    fun navigateToFolder(path: String) {
        repository.navigateToFolder(path)
    }

    fun navigateUp() {
        repository.navigateUp()
    }

    fun createFolder(name: String) {
        if (name.isNotBlank()) {
            repository.createCloudFolder(name)
            showSnackbar("Folder created: $name")
        }
        showCreateFolderDialog.value = false
    }

    fun deleteCloudFile(id: String) {
        repository.deleteCloudFile(id)
        showSnackbar("Item removed from cloud drive.")
    }

    fun pauseTransfer(id: String) {
        repository.transferQueueManager.pauseTransfer(id)
    }

    fun resumeTransfer(id: String) {
        repository.transferQueueManager.resumeTransfer(id)
    }

    fun cancelTransfer(id: String) {
        repository.transferQueueManager.cancelTransfer(id)
        showSnackbar("Transfer canceled.")
    }

    fun clearCompletedTransfers() {
        repository.transferQueueManager.clearCompleted()
        showSnackbar("Cleared completed transfers.")
    }

    fun downloadAndInstallApk(item: CloudFileItem) {
        showSnackbar("Downloading ${item.name} from MEGA Cloud...")
        repository.downloadAndInstallApk(item) { downloadedFile ->
            showSnackbar("Downloaded ${downloadedFile.name}. Opening installer...")
            repository.installApk(downloadedFile)
        }
    }

    fun uploadCustomFile(fileName: String, content: String) {
        viewModelScope.launch {
            val tempFile = File(getApplication<Application>().cacheDir, fileName)
            tempFile.writeText(content)
            repository.transferQueueManager.enqueueUpload(
                file = tempFile,
                remoteDirectory = repository.currentCloudPath.value,
                category = "GENERAL",
                authToken = repository.cloudAccount.value.sessionToken,
                uploadEndpoint = repository.settings.value.liveEndpointUrl
            ) {
                repository.loadCloudFiles(repository.currentCloudPath.value)
                showSnackbar("Uploaded $fileName to MEGA Drive.")
            }
        }
    }

    fun uploadLocalFile(file: File, remoteName: String = file.name, category: String = "GENERAL") {
        viewModelScope.launch {
            repository.transferQueueManager.enqueueUpload(
                file = file,
                remoteDirectory = repository.currentCloudPath.value,
                category = category,
                authToken = repository.cloudAccount.value.sessionToken,
                uploadEndpoint = repository.settings.value.liveEndpointUrl
            ) {
                repository.loadCloudFiles(repository.currentCloudPath.value)
                showSnackbar("Uploaded $remoteName to MEGA Drive.")
            }
        }
    }

    fun generateShareLink(item: CloudFileItem) {
        val link = item.publicShareUrl ?: repository.megaCloudManager.generateShareLink(item.id, item.name)
        shareLinkDialogUrl.value = link
    }

    fun copyToClipboard(text: String, label: String = "Link") {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        showSnackbar("Copied to clipboard: $text")
    }

    fun updateAccount(account: CloudAccount) {
        repository.updateAccount(account)
        showSnackbar("MEGA Cloud account settings updated.")
        showAccountDialog.value = false
    }

    fun updateSettings(settings: CloudSyncSettings) {
        repository.updateSettings(settings)
        showSnackbar("Sync preferences saved.")
    }

    fun onUserSignedIn(user: com.google.firebase.auth.FirebaseUser) {
        currentUser.value = user
        repository.onUserSignedIn(user)
        showSnackbar("Signed in as ${user.email}. Firebase Firestore sync active!")
    }

    fun signOut() {
        val credentialManager = androidx.credentials.CredentialManager.create(getApplication())
        com.example.auth.AuthManager.signOut(
            credentialManager = credentialManager,
            onSignOutComplete = {
                currentUser.value = null
                showSnackbar("Signed out of Google account.")
            },
            scope = viewModelScope
        )
    }
}
