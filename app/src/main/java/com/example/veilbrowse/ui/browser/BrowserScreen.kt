package com.example.veilbrowse.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningYellow
import com.example.veilbrowse.data.model.BookmarkEntity
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.model.TabEntity
import com.example.veilbrowse.engine.TrackerBlocker
import com.example.veilbrowse.engine.UserAgentManager
import com.example.veilbrowse.engine.VeilWebChromeClient
import com.example.veilbrowse.engine.VeilWebViewClient
import com.example.veilbrowse.viewmodel.BrowserUiState
import com.example.veilbrowse.viewmodel.BrowserViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    uiState: BrowserUiState,
    tabs: List<TabEntity>,
    privacySettings: PrivacySettingsState,
    bookmarks: List<BookmarkEntity>,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf(uiState.currentUrl) }
    var isEditingUrl by remember { mutableStateOf(false) }

    var showTabSwitcher by remember { mutableStateOf(false) }
    var showTrackersSheet by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Keep url input in sync with loaded page
    LaunchedEffect(uiState.currentUrl) {
        if (!isEditingUrl) {
            urlInput = uiState.currentUrl
        }
    }

    // Intercept back button when inside browser
    BackHandler(enabled = uiState.canGoBack) {
        viewModel.goBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("browser_screen")
    ) {
        // TOP ADDRESS & SECURITY TOOLBAR
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Security Lock Indicator
                    val isHttps = uiState.isSecureHttps
                    IconButton(
                        onClick = { showTrackersSheet = true },
                        modifier = Modifier.size(36.dp).testTag("security_lock_button")
                    ) {
                        Icon(
                            imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isHttps) "Encrypted HTTPS" else "Not Encrypted",
                            tint = if (isHttps) SafeGreen else WarningYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // URL Input bar
                    OutlinedTextField(
                        value = if (isEditingUrl) urlInput else (uiState.currentUrl.ifBlank { "" }),
                        onValueChange = {
                            urlInput = it
                            isEditingUrl = true
                        },
                        placeholder = {
                            Text(
                                text = "Search or type URL",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                isEditingUrl = false
                                viewModel.openUrl(urlInput)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isEditingUrl && urlInput.isNotBlank()) {
                                    IconButton(
                                        onClick = { urlInput = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Tracker Shield Counter Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (uiState.sessionTrackersBlockedCount > 0)
                                                CyanPrimary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { showTrackersSheet = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("tracker_shield_badge")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Trackers",
                                            tint = if (uiState.sessionTrackersBlockedCount > 0) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${uiState.sessionTrackersBlockedCount}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.sessionTrackersBlockedCount > 0) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("browser_address_bar")
                    )

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.reload() },
                        modifier = Modifier.size(36.dp).testTag("refresh_page_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tab Count Button
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = CyanPrimary,
                                contentColor = Color.Black
                            ) {
                                Text(
                                    text = "${tabs.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = { showTabSwitcher = true },
                            modifier = Modifier.size(36.dp).testTag("open_tab_switcher_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tab,
                                contentDescription = "Tabs",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(36.dp).testTag("browser_overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Private Session") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.startNewPrivateSession()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonPurple)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Browsing Data") },
                                onClick = {
                                    showOverflowMenu = false
                                    showClearDataDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = DangerRed)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Blocked Trackers (${uiState.sessionTrackersBlockedCount})") },
                                onClick = {
                                    showOverflowMenu = false
                                    showTrackersSheet = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = CyanPrimary)
                                }
                            )
                            val isBookmarked = bookmarks.any { it.url == uiState.currentUrl }
                            DropdownMenuItem(
                                text = { Text(if (isBookmarked) "Remove Bookmark" else "Bookmark Page") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleBookmark()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = CyanPrimary
                                    )
                                }
                            )
                        }
                    }
                }

                // Loading progress bar
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        progress = { uiState.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = CyanPrimary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // BROWSER CONTENT (WEBVIEW OR NEW TAB START PAGE)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isBlankPage = uiState.currentUrl.isBlank() || uiState.currentUrl == "about:blank"

            // Native Android WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Privacy & Security settings
                        val ws = this.settings
                        ws.javaScriptEnabled = privacySettings.javaScriptEnabled
                        ws.domStorageEnabled = true
                        ws.setSupportMultipleWindows(false)
                        ws.javaScriptCanOpenWindowsAutomatically = !privacySettings.blockPopups
                        ws.setGeolocationEnabled(false)
                        ws.savePassword = false
                        ws.saveFormData = false
                        ws.allowFileAccess = false
                        ws.allowContentAccess = false
                        ws.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        ws.cacheMode = WebSettings.LOAD_DEFAULT

                        // Generic User-Agent application to reduce hardware model fingerprinting
                        val defaultUa = ws.userAgentString
                        ws.userAgentString = UserAgentManager.getUserAgent(
                            type = privacySettings.privacyProfile,
                            defaultUa = defaultUa
                        )

                        // Block 3rd-party cookies
                        CookieManager.getInstance().setAcceptThirdPartyCookies(
                            this,
                            !privacySettings.blockThirdPartyCookies
                        )

                        webViewClient = VeilWebViewClient(
                            scope = coroutineScope,
                            trackerDao = com.example.veilbrowse.data.db.VeilDatabase.getInstance(ctx).trackerDao(),
                            isTrackerBlockingEnabled = { privacySettings.blockTrackers },
                            onPageTitleChanged = { title ->
                                viewModel.onPageFinished(url ?: "", title)
                            },
                            onUrlChanged = { newUrl ->
                                viewModel.onPageStarted(newUrl)
                                viewModel.updateNavigationState(canGoBack(), canGoForward())
                            },
                            onLoadingStateChanged = { loading ->
                                viewModel.onProgressChanged(if (loading) 20 else 100)
                                viewModel.updateNavigationState(canGoBack(), canGoForward())
                            },
                            onTrackerBlocked = { domain, category ->
                                viewModel.onTrackerBlocked(domain, category)
                            }
                        )

                        webChromeClient = VeilWebChromeClient(
                            onProgressChanged = { progress ->
                                viewModel.onProgressChanged(progress)
                            },
                            onTitleReceived = { title ->
                                viewModel.onPageFinished(url ?: "", title)
                            }
                        )

                        viewModel.activeWebView = this
                        if (uiState.currentUrl.isNotBlank() && uiState.currentUrl != "about:blank") {
                            loadUrl(uiState.currentUrl)
                        }
                    }
                },
                update = { webView ->
                    viewModel.activeWebView = webView
                    webView.settings.javaScriptEnabled = privacySettings.javaScriptEnabled
                    val currentUa = webView.settings.userAgentString
                    webView.settings.userAgentString = UserAgentManager.getUserAgent(
                        type = privacySettings.privacyProfile,
                        defaultUa = currentUa
                    )
                },
                modifier = Modifier.fillMaxSize().testTag("webview_container")
            )

            // Sleek New Tab Privacy Home if currentUrl is empty
            if (isBlankPage) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "VeilBrowse Private Tab",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Search privately with ${privacySettings.defaultSearchEngine.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Search shortcuts
                        Text(
                            text = "Popular Privacy Portals",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickShortcutChip("DuckDuckGo") { viewModel.openUrl("https://duckduckgo.com") }
                            QuickShortcutChip("Brave") { viewModel.openUrl("https://search.brave.com") }
                            QuickShortcutChip("Wikipedia") { viewModel.openUrl("https://wikipedia.org") }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickShortcutChip("EFF Privacy") { viewModel.openUrl("https://eff.org") }
                            QuickShortcutChip("Archive.org") { viewModel.openUrl("https://archive.org") }
                            QuickShortcutChip("Proton") { viewModel.openUrl("https://proton.me") }
                        }

                        // Bookmarks if any
                        if (bookmarks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Saved Bookmarks",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(bookmarks) { bm ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.clickable { viewModel.openUrl(bm.url) }
                                    ) {
                                        Text(
                                            text = bm.title.take(18),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM BROWSER CONTROLS TOOLBAR
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    enabled = uiState.canGoBack,
                    modifier = Modifier.testTag("browser_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (uiState.canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = { viewModel.goForward() },
                    enabled = uiState.canGoForward,
                    modifier = Modifier.testTag("browser_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (uiState.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = { viewModel.openUrl("about:blank") },
                    modifier = Modifier.testTag("browser_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { viewModel.openNewTab("") },
                    modifier = Modifier.testTag("browser_new_tab_action")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Tab",
                        tint = CyanPrimary
                    )
                }

                IconButton(
                    onClick = { showClearDataDialog = true },
                    modifier = Modifier.testTag("browser_clear_action")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Data",
                        tint = DangerRed
                    )
                }
            }
        }
    }

    // Modal Sheets and Dialogs
    if (showTabSwitcher) {
        TabSwitcherSheet(
            tabs = tabs,
            activeTabId = uiState.currentTabId,
            onTabSelected = { tab -> viewModel.selectTab(tab) },
            onTabClosed = { tabId -> viewModel.closeTab(tabId) },
            onNewTab = { viewModel.openNewTab("") },
            onDismiss = { showTabSwitcher = false }
        )
    }

    if (showTrackersSheet) {
        BlockedTrackersSheet(
            trackers = uiState.sessionBlockedTrackersList,
            onDismiss = { showTrackersSheet = false }
        )
    }

    if (showClearDataDialog) {
        ClearDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirmClear = { cookies, cache, storage, trackers ->
                viewModel.clearBrowsingData(cookies, cache, storage, trackers) {}
            }
        )
    }
}

@Composable
private fun QuickShortcutChip(label: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("shortcut_$label")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}
