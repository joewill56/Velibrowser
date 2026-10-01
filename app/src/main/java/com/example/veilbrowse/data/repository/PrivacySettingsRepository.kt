package com.example.veilbrowse.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.veilbrowse.data.model.PrivacyProfileType
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.model.SearchEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PrivacySettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("veil_privacy_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PrivacySettingsState> = _settings.asStateFlow()

    private fun loadSettings(): PrivacySettingsState {
        val profileName = prefs.getString(KEY_PRIVACY_PROFILE, PrivacyProfileType.GENERIC_ANDROID.name)
        val profile = try {
            PrivacyProfileType.valueOf(profileName ?: PrivacyProfileType.GENERIC_ANDROID.name)
        } catch (_: Exception) {
            PrivacyProfileType.GENERIC_ANDROID
        }

        val searchName = prefs.getString(KEY_SEARCH_ENGINE, SearchEngine.DUCKDUCKGO.name)
        val searchEngine = try {
            SearchEngine.valueOf(searchName ?: SearchEngine.DUCKDUCKGO.name)
        } catch (_: Exception) {
            SearchEngine.DUCKDUCKGO
        }

        val levelName = prefs.getString(KEY_TRACKER_PROTECTION_LEVEL, null)
        val protectionLevel = if (levelName != null) {
            try {
                com.example.veilbrowse.data.model.TrackerProtectionLevel.valueOf(levelName)
            } catch (_: Exception) {
                com.example.veilbrowse.data.model.TrackerProtectionLevel.BALANCED
            }
        } else {
            val legacyBlock = prefs.getBoolean(KEY_BLOCK_TRACKERS, true)
            if (legacyBlock) com.example.veilbrowse.data.model.TrackerProtectionLevel.BALANCED else com.example.veilbrowse.data.model.TrackerProtectionLevel.OFF
        }

        return PrivacySettingsState(
            trackerProtectionLevel = protectionLevel,
            blockTrackers = protectionLevel != com.example.veilbrowse.data.model.TrackerProtectionLevel.OFF,
            blockThirdPartyCookies = prefs.getBoolean(KEY_BLOCK_3RD_PARTY_COOKIES, true),
            clearDataOnExit = prefs.getBoolean(KEY_CLEAR_ON_EXIT, true),
            webrtcProtection = prefs.getBoolean(KEY_WEBRTC_PROTECT, true),
            sendDoNotTrack = prefs.getBoolean(KEY_SEND_DNT, true),
            privacyProfile = profile,
            defaultSearchEngine = searchEngine,
            homepageUrl = prefs.getString(KEY_HOMEPAGE, "about:blank") ?: "about:blank",
            javaScriptEnabled = prefs.getBoolean(KEY_JAVASCRIPT, true),
            blockPopups = prefs.getBoolean(KEY_BLOCK_POPUPS, true),
            enforceHttps = prefs.getBoolean(KEY_ENFORCE_HTTPS, true),
            proxyEnabled = prefs.getBoolean(KEY_PROXY_ENABLED, false),
            proxyHost = prefs.getString(KEY_PROXY_HOST, "") ?: "",
            proxyPort = prefs.getInt(KEY_PROXY_PORT, 8080),
            proxyType = prefs.getString(KEY_PROXY_TYPE, "HTTP") ?: "HTTP"
        )
    }

    fun updateSettings(transform: (PrivacySettingsState) -> PrivacySettingsState) {
        val updated = transform(_settings.value)
        prefs.edit().apply {
            putString(KEY_TRACKER_PROTECTION_LEVEL, updated.trackerProtectionLevel.name)
            putBoolean(KEY_BLOCK_TRACKERS, updated.blockTrackers)
            putBoolean(KEY_BLOCK_3RD_PARTY_COOKIES, updated.blockThirdPartyCookies)
            putBoolean(KEY_CLEAR_ON_EXIT, updated.clearDataOnExit)
            putBoolean(KEY_WEBRTC_PROTECT, updated.webrtcProtection)
            putBoolean(KEY_SEND_DNT, updated.sendDoNotTrack)
            putString(KEY_PRIVACY_PROFILE, updated.privacyProfile.name)
            putString(KEY_SEARCH_ENGINE, updated.defaultSearchEngine.name)
            putString(KEY_HOMEPAGE, updated.homepageUrl)
            putBoolean(KEY_JAVASCRIPT, updated.javaScriptEnabled)
            putBoolean(KEY_BLOCK_POPUPS, updated.blockPopups)
            putBoolean(KEY_ENFORCE_HTTPS, updated.enforceHttps)
            putBoolean(KEY_PROXY_ENABLED, updated.proxyEnabled)
            putString(KEY_PROXY_HOST, updated.proxyHost)
            putInt(KEY_PROXY_PORT, updated.proxyPort)
            putString(KEY_PROXY_TYPE, updated.proxyType)
            apply()
        }
        _settings.value = updated
    }

    companion object {
        private const val KEY_TRACKER_PROTECTION_LEVEL = "tracker_protection_level"
        private const val KEY_BLOCK_TRACKERS = "block_trackers"
        private const val KEY_BLOCK_3RD_PARTY_COOKIES = "block_3rd_party_cookies"
        private const val KEY_CLEAR_ON_EXIT = "clear_on_exit"
        private const val KEY_WEBRTC_PROTECT = "webrtc_protect"
        private const val KEY_SEND_DNT = "send_dnt"
        private const val KEY_PRIVACY_PROFILE = "privacy_profile"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_HOMEPAGE = "homepage"
        private const val KEY_JAVASCRIPT = "javascript"
        private const val KEY_BLOCK_POPUPS = "block_popups"
        private const val KEY_ENFORCE_HTTPS = "enforce_https"
        private const val KEY_PROXY_ENABLED = "proxy_enabled"
        private const val KEY_PROXY_HOST = "proxy_host"
        private const val KEY_PROXY_PORT = "proxy_port"
        private const val KEY_PROXY_TYPE = "proxy_type"
    }
}
