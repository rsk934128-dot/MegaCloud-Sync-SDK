package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransferStatus
import com.example.ui.dialogs.AccountConfigDialog
import com.example.ui.dialogs.AppDetailDialog
import com.example.ui.dialogs.CreateFolderDialog
import com.example.ui.dialogs.ShareLinkDialog
import com.example.ui.dialogs.ThreatReportDialog
import com.example.ui.dialogs.UploadFileDialog
import com.example.ui.screens.ApkSecurityScreen
import com.example.ui.screens.CloudDriveScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FirebaseStorageScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransfersScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MegaRed
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppPulseViewModel
import com.example.viewmodel.NavTab

class MainActivity : ComponentActivity() {

    private val viewModel: AppPulseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val accentOption by viewModel.selectedAccentOption.collectAsState()
            val isOledMode by viewModel.isOledMode.collectAsState()

            MyApplicationTheme(
                accentOption = accentOption,
                isOledBlack = isOledMode
            ) {
                val currentUser by viewModel.currentUser.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                androidx.compose.runtime.DisposableEffect(Unit) {
                    val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
                        viewModel.currentUser.value = auth.currentUser
                        auth.currentUser?.let { user ->
                            viewModel.onUserSignedIn(user)
                        }
                    }
                    val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    firebaseAuth.addAuthStateListener(authListener)

                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                    com.example.auth.AuthManager.attemptAutoSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            firebaseAuth.currentUser?.let { user ->
                                viewModel.onUserSignedIn(user)
                            }
                        },
                        onUnauthenticated = {},
                        scope = coroutineScope
                    )

                    onDispose {
                        firebaseAuth.removeAuthStateListener(authListener)
                    }
                }

                if (currentUser == null) {
                    com.example.ui.screens.AuthScreen(
                        onAuthSuccess = {
                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.let { user ->
                                viewModel.onUserSignedIn(user)
                            }
                        }
                    )
                } else {
                    AppPulseMainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPulseMainApp(viewModel: AppPulseViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    val activeTransferCount = transfers.count { it.status == TransferStatus.TRANSFERRING }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Support hardware/system back button
    BackHandler(enabled = selectedTab != NavTab.DASHBOARD) {
        viewModel.selectTab(NavTab.DASHBOARD)
    }

    // Dialog state collectors
    val shareLinkUrl by viewModel.shareLinkDialogUrl.collectAsState()
    val showCreateFolder by viewModel.showCreateFolderDialog.collectAsState()
    val showUploadDialog by viewModel.showUploadDialog.collectAsState()
    val showAccountDialog by viewModel.showAccountDialog.collectAsState()
    val selectedAppForDetail by viewModel.selectedAppForDetail.collectAsState()
    val showReportDetail by viewModel.showReportDetail.collectAsState()
    val currentAccount by viewModel.cloudAccount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanPrimary.copy(alpha = 0.2f))
                                .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Logo",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AppPulse",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MegaRed)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "MEGA",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = selectedTab.title,
                                fontSize = 11.sp,
                                color = CyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                // Dashboard
                NavigationBarItem(
                    selected = selectedTab == NavTab.DASHBOARD,
                    onClick = { viewModel.selectTab(NavTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavTab.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Overview", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00202E),
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // Cloud Drive
                NavigationBarItem(
                    selected = selectedTab == NavTab.CLOUD_DRIVE,
                    onClick = { viewModel.selectTab(NavTab.CLOUD_DRIVE) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavTab.CLOUD_DRIVE) Icons.Default.Cloud else Icons.Outlined.Cloud,
                            contentDescription = "MEGA Drive"
                        )
                    },
                    label = { Text("Drive", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00202E),
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_drive")
                )

                // APK Security Audit
                NavigationBarItem(
                    selected = selectedTab == NavTab.APK_AUDIT,
                    onClick = { viewModel.selectTab(NavTab.APK_AUDIT) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavTab.APK_AUDIT) Icons.Default.Security else Icons.Outlined.Security,
                            contentDescription = "Security Audit"
                        )
                    },
                    label = { Text("Security", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00202E),
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_security")
                )

                // Transfers
                NavigationBarItem(
                    selected = selectedTab == NavTab.TRANSFERS,
                    onClick = { viewModel.selectTab(NavTab.TRANSFERS) },
                    icon = {
                        if (activeTransferCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = CyanPrimary) {
                                        Text(
                                            text = "$activeTransferCount",
                                            color = Color(0xFF00202E),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == NavTab.TRANSFERS) Icons.Default.Sync else Icons.Outlined.Sync,
                                    contentDescription = "Transfers"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (selectedTab == NavTab.TRANSFERS) Icons.Default.Sync else Icons.Outlined.Sync,
                                contentDescription = "Transfers"
                            )
                        }
                    },
                    label = { Text("Transfers", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00202E),
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_transfers")
                )

                // Firebase Storage Bucket
                NavigationBarItem(
                    selected = selectedTab == NavTab.FIREBASE_STORAGE,
                    onClick = { viewModel.selectTab(NavTab.FIREBASE_STORAGE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Firebase Storage"
                        )
                    },
                    label = { Text("Bucket", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00202E),
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_firebase_bucket")
                )

                // Settings
                NavigationBarItem(
                    selected = selectedTab == NavTab.SETTINGS,
                    onClick = { viewModel.selectTab(NavTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00202E),
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "tab_transition") { tab ->
                when (tab) {
                    NavTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    NavTab.CLOUD_DRIVE -> CloudDriveScreen(viewModel = viewModel)
                    NavTab.FIREBASE_STORAGE -> FirebaseStorageScreen(viewModel = viewModel)
                    NavTab.APK_AUDIT -> ApkSecurityScreen(viewModel = viewModel)
                    NavTab.TRANSFERS -> TransfersScreen(viewModel = viewModel)
                    NavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Modal Dialogs
    shareLinkUrl?.let { url ->
        ShareLinkDialog(
            url = url,
            onDismiss = { viewModel.shareLinkDialogUrl.value = null },
            onCopy = { viewModel.copyToClipboard(it, "MEGA Share Link") }
        )
    }

    if (showCreateFolder) {
        CreateFolderDialog(
            onDismiss = { viewModel.showCreateFolderDialog.value = false },
            onCreate = { viewModel.createFolder(it) }
        )
    }

    if (showUploadDialog) {
        UploadFileDialog(
            onDismiss = { viewModel.showUploadDialog.value = false },
            onUpload = { name, content -> viewModel.uploadCustomFile(name, content) }
        )
    }

    if (showAccountDialog) {
        AccountConfigDialog(
            currentAccount = currentAccount,
            onDismiss = { viewModel.showAccountDialog.value = false },
            onSave = { viewModel.updateAccount(it) }
        )
    }

    selectedAppForDetail?.let { app ->
        AppDetailDialog(
            app = app,
            onDismiss = { viewModel.selectedAppForDetail.value = null },
            onBackup = { viewModel.backupApkToCloud(it) }
        )
    }

    showReportDetail?.let { report ->
        ThreatReportDialog(
            report = report,
            onDismiss = { viewModel.showReportDetail.value = null },
            onSync = { viewModel.syncAuditReportToCloud(it) }
        )
    }
}
