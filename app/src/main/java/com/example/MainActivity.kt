package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.veilbrowse.ui.browser.BrowserScreen
import com.example.veilbrowse.ui.browser.ClearDataDialog
import com.example.veilbrowse.ui.components.VeilBottomNavBar
import com.example.veilbrowse.ui.components.VeilNavigationItem
import com.example.veilbrowse.ui.components.VeilTopBar
import com.example.veilbrowse.ui.connection.ConnectionScreen
import com.example.veilbrowse.ui.dashboard.DashboardScreen
import com.example.veilbrowse.ui.privacy.PrivacyDiagnosticsScreen
import com.example.veilbrowse.ui.settings.SettingsScreen
import com.example.veilbrowse.viewmodel.BrowserViewModel
import com.example.veilbrowse.viewmodel.ConnectionViewModel
import com.example.veilbrowse.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VeilBrowseApp()
            }
        }
    }
}

@Composable
fun VeilBrowseApp(
    browserViewModel: BrowserViewModel = viewModel(),
    connectionViewModel: ConnectionViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(VeilNavigationItem.HOME) }
    var showGlobalClearDataDialog by remember { mutableStateOf(false) }

    val browserUiState by browserViewModel.uiState.collectAsState()
    val allTabs by browserViewModel.allTabs.collectAsState()
    val totalTrackersBlocked by browserViewModel.totalBlockedCount.collectAsState()
    val bookmarks by browserViewModel.bookmarks.collectAsState()

    val networkInfo by connectionViewModel.networkInfo.collectAsState()
    val proxyStatusMessage by connectionViewModel.proxyStatusMessage.collectAsState()
    val settingsState by settingsViewModel.settings.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle back button on sub-screens to return to HOME
    BackHandler(enabled = currentScreen != VeilNavigationItem.HOME && !browserUiState.canGoBack) {
        currentScreen = VeilNavigationItem.HOME
    }

    Scaffold(
        topBar = {
            if (currentScreen != VeilNavigationItem.BROWSER) {
                VeilTopBar(
                    title = currentScreen.label,
                    isVpnActive = networkInfo.isVpnConnected
                )
            }
        },
        bottomBar = {
            VeilBottomNavBar(
                selectedItem = currentScreen,
                onItemSelected = { selected ->
                    currentScreen = selected
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                VeilNavigationItem.HOME -> {
                    DashboardScreen(
                        networkInfo = networkInfo,
                        totalTrackersBlocked = totalTrackersBlocked,
                        sessionTrackersBlocked = browserUiState.sessionTrackersBlockedCount,
                        onNavigateToBrowser = { url ->
                            browserViewModel.openUrl(url)
                            currentScreen = VeilNavigationItem.BROWSER
                        },
                        onNavigateToConnection = {
                            currentScreen = VeilNavigationItem.CONNECTION
                        },
                        onNavigateToPrivacyTest = {
                            currentScreen = VeilNavigationItem.PRIVACY
                        },
                        onStartNewPrivateSession = {
                            browserViewModel.startNewPrivateSession()
                            currentScreen = VeilNavigationItem.BROWSER
                            scope.launch {
                                snackbarHostState.showSnackbar("Fresh Private Session initialized")
                            }
                        },
                        onOpenClearDataDialog = {
                            showGlobalClearDataDialog = true
                        },
                        onRefreshDiagnostics = {
                            connectionViewModel.runNetworkDiagnostics()
                        }
                    )
                }

                VeilNavigationItem.BROWSER -> {
                    BrowserScreen(
                        viewModel = browserViewModel,
                        uiState = browserUiState,
                        tabs = allTabs,
                        privacySettings = settingsState,
                        bookmarks = bookmarks,
                        onNavigateHome = {
                            currentScreen = VeilNavigationItem.HOME
                        }
                    )
                }

                VeilNavigationItem.PRIVACY -> {
                    PrivacyDiagnosticsScreen(
                        networkInfo = networkInfo,
                        settings = settingsState,
                        totalTrackersBlocked = totalTrackersBlocked,
                        onRunDiagnostics = {
                            connectionViewModel.runNetworkDiagnostics()
                        }
                    )
                }

                VeilNavigationItem.CONNECTION -> {
                    ConnectionScreen(
                        networkInfo = networkInfo,
                        settings = settingsState,
                        proxyStatusMessage = proxyStatusMessage,
                        onSaveProxy = { enabled, host, port, type ->
                            connectionViewModel.saveProxyConfiguration(enabled, host, port, type)
                        },
                        onRefresh = {
                            connectionViewModel.runNetworkDiagnostics()
                        },
                        onClearStatusMessage = {
                            connectionViewModel.clearProxyStatusMessage()
                        }
                    )
                }

                VeilNavigationItem.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        settings = settingsState,
                        onNavigateToConnection = {
                            currentScreen = VeilNavigationItem.CONNECTION
                        }
                    )
                }
            }
        }
    }

    if (showGlobalClearDataDialog) {
        ClearDataDialog(
            onDismiss = { showGlobalClearDataDialog = false },
            onConfirmClear = { cookies, cache, storage, trackers ->
                browserViewModel.clearBrowsingData(cookies, cache, storage, trackers) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Browsing data cleared successfully")
                    }
                }
            }
        )
    }
}
