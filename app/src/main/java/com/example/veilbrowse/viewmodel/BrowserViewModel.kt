package com.example.veilbrowse.viewmodel

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veilbrowse.data.db.VeilDatabase
import com.example.veilbrowse.data.model.BlockedTrackerEntity
import com.example.veilbrowse.data.model.BookmarkEntity
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.model.SearchEngine
import com.example.veilbrowse.data.model.TabEntity
import com.example.veilbrowse.data.repository.PrivacySettingsRepository
import com.example.veilbrowse.engine.SessionCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class BrowserUiState(
    val currentTabId: String = "",
    val currentUrl: String = "",
    val currentTitle: String = "New Tab",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val sessionTrackersBlockedCount: Int = 0,
    val sessionBlockedTrackersList: List<BlockedTrackerEntity> = emptyList(),
    val isSecureHttps: Boolean = false,
    val isPrivateSession: Boolean = true
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VeilDatabase.getInstance(application)
    private val tabDao = db.tabDao()
    private val trackerDao = db.trackerDao()
    private val bookmarkDao = db.bookmarkDao()
    private val settingsRepo = PrivacySettingsRepository(application)

    val settings: StateFlow<PrivacySettingsState> = settingsRepo.settings

    val allTabs: StateFlow<List<TabEntity>> = tabDao.getAllTabs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalBlockedCount: StateFlow<Int> = trackerDao.getTotalBlockedCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val recentBlockedTrackers: StateFlow<List<BlockedTrackerEntity>> = trackerDao.getRecentBlockedTrackers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    // Reference to current active WebView for navigation control
    var activeWebView: WebView? = null

    init {
        initializeFirstTab()
    }

    private fun initializeFirstTab() {
        viewModelScope.launch(Dispatchers.IO) {
            val initialUrl = settings.value.homepageUrl
            val firstTab = TabEntity(
                id = UUID.randomUUID().toString(),
                title = "Private Home",
                url = if (initialUrl.isBlank() || initialUrl == "about:blank") "" else initialUrl,
                isCurrent = true
            )
            tabDao.insertTab(firstTab)
            _uiState.value = _uiState.value.copy(
                currentTabId = firstTab.id,
                currentUrl = firstTab.url,
                currentTitle = firstTab.title,
                isSecureHttps = firstTab.url.startsWith("https://")
            )
        }
    }

    fun openUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val targetUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> settings.value.defaultSearchEngine.searchUrl + java.net.URLEncoder.encode(trimmed, "UTF-8")
        }

        _uiState.value = _uiState.value.copy(
            currentUrl = targetUrl,
            isSecureHttps = targetUrl.startsWith("https://", ignoreCase = true)
        )

        activeWebView?.loadUrl(targetUrl)

        // Update current tab entity
        val currentTabId = _uiState.value.currentTabId
        if (currentTabId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val existing = tabDao.getTabById(currentTabId)
                if (existing != null) {
                    tabDao.updateTab(existing.copy(url = targetUrl))
                }
            }
        }
    }

    fun openNewTab(url: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val newTab = TabEntity(
                id = UUID.randomUUID().toString(),
                title = if (url.isBlank()) "New Tab" else "Loading…",
                url = url,
                isCurrent = true
            )
            tabDao.insertTab(newTab)
            _uiState.value = _uiState.value.copy(
                currentTabId = newTab.id,
                currentUrl = newTab.url,
                currentTitle = newTab.title,
                isLoading = false,
                progress = 0,
                isSecureHttps = newTab.url.startsWith("https://")
            )
            if (url.isNotBlank()) {
                launch(Dispatchers.Main) {
                    activeWebView?.loadUrl(url)
                }
            }
        }
    }

    fun selectTab(tab: TabEntity) {
        _uiState.value = _uiState.value.copy(
            currentTabId = tab.id,
            currentUrl = tab.url,
            currentTitle = tab.title,
            isSecureHttps = tab.url.startsWith("https://")
        )
        if (tab.url.isNotBlank()) {
            activeWebView?.loadUrl(tab.url)
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tabDao.deleteTabById(tabId)
            val remaining = tabDao.getAllTabs()
            // If the closed tab was active, switch to next available tab or open empty one
            if (_uiState.value.currentTabId == tabId) {
                // If all tabs closed, create a fresh one
                val next = TabEntity(
                    id = UUID.randomUUID().toString(),
                    title = "New Tab",
                    url = "",
                    isCurrent = true
                )
                tabDao.insertTab(next)
                launch(Dispatchers.Main) {
                    selectTab(next)
                }
            }
        }
    }

    fun reload() {
        activeWebView?.reload()
    }

    fun goBack(): Boolean {
        if (activeWebView?.canGoBack() == true) {
            activeWebView?.goBack()
            return true
        }
        return false
    }

    fun goForward() {
        if (activeWebView?.canGoForward() == true) {
            activeWebView?.goForward()
        }
    }

    fun updateNavigationState(canBack: Boolean, canForward: Boolean) {
        _uiState.value = _uiState.value.copy(
            canGoBack = canBack,
            canGoForward = canForward
        )
    }

    fun onPageStarted(url: String) {
        _uiState.value = _uiState.value.copy(
            currentUrl = url,
            isLoading = true,
            isSecureHttps = url.startsWith("https://", ignoreCase = true)
        )
    }

    fun onPageFinished(url: String, title: String?) {
        val safeTitle = title?.takeIf { it.isNotBlank() } ?: url
        _uiState.value = _uiState.value.copy(
            currentUrl = url,
            currentTitle = safeTitle,
            isLoading = false,
            isSecureHttps = url.startsWith("https://", ignoreCase = true)
        )
        // Update tab in DB
        val tabId = _uiState.value.currentTabId
        if (tabId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val tab = tabDao.getTabById(tabId)
                if (tab != null) {
                    tabDao.updateTab(tab.copy(url = url, title = safeTitle))
                }
            }
        }
    }

    fun onProgressChanged(progress: Int) {
        _uiState.value = _uiState.value.copy(
            progress = progress,
            isLoading = progress < 100
        )
    }

    fun onTrackerBlocked(domain: String, category: String) {
        val tracker = BlockedTrackerEntity(
            domain = domain,
            category = category,
            blockedOnUrl = _uiState.value.currentUrl
        )
        val currentList = _uiState.value.sessionBlockedTrackersList.toMutableList()
        currentList.add(0, tracker)

        _uiState.value = _uiState.value.copy(
            sessionTrackersBlockedCount = _uiState.value.sessionTrackersBlockedCount + 1,
            sessionBlockedTrackersList = currentList.take(50)
        )
    }

    fun startNewPrivateSession() {
        SessionCleaner.startFreshPrivateSession(
            getApplication(),
            activeWebView
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                tabDao.deleteAllTabs()
                val freshTab = TabEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Private Session",
                    url = "",
                    isCurrent = true
                )
                tabDao.insertTab(freshTab)

                launch(Dispatchers.Main) {
                    _uiState.value = BrowserUiState(
                        currentTabId = freshTab.id,
                        currentUrl = "",
                        currentTitle = "Private Session",
                        sessionTrackersBlockedCount = 0,
                        sessionBlockedTrackersList = emptyList()
                    )
                    activeWebView?.loadUrl("about:blank")
                }
            }
        }
    }

    fun clearBrowsingData(
        cookies: Boolean,
        cache: Boolean,
        storage: Boolean,
        trackerLogs: Boolean,
        onDone: () -> Unit
    ) {
        if (cookies) {
            SessionCleaner.clearCookies()
        }
        if (storage) {
            SessionCleaner.clearStorage()
        }
        if (cache) {
            SessionCleaner.clearCache(getApplication(), activeWebView)
        }
        if (trackerLogs) {
            viewModelScope.launch(Dispatchers.IO) {
                trackerDao.clearAllBlockedTrackers()
            }
            _uiState.value = _uiState.value.copy(
                sessionTrackersBlockedCount = 0,
                sessionBlockedTrackersList = emptyList()
            )
        }
        onDone()
    }

    fun toggleBookmark() {
        val url = _uiState.value.currentUrl
        if (url.isBlank() || url == "about:blank") return

        val title = _uiState.value.currentTitle
        viewModelScope.launch(Dispatchers.IO) {
            val existing = bookmarks.value.firstOrNull { it.url == url }
            if (existing != null) {
                bookmarkDao.deleteBookmark(existing.id)
            } else {
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        url = url
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (settings.value.clearDataOnExit) {
            SessionCleaner.startFreshPrivateSession(getApplication(), activeWebView) {}
        }
    }
}
