package com.example.veilbrowse.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPrivateSession: Boolean = true,
    val isCurrent: Boolean = false
)

@Entity(tableName = "blocked_trackers")
data class BlockedTrackerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val category: String, // Advertising, Analytics, Social Pixel, Fingerprinting
    val timestamp: Long = System.currentTimeMillis(),
    val blockedOnUrl: String
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PrivacyProfileType(val displayName: String, val description: String) {
    STANDARD("Standard", "Uses standard browser behavior with tracker blocking enabled."),
    GENERIC_ANDROID("Generic Android", "Removes device hardware model and build ID. Exposes a generic Android version."),
    GENERIC_MOBILE("Generic Mobile", "Uses a standardized mobile Safari/WebKit identity to reduce platform-specific fingerprinting."),
    GENERIC_DESKTOP("Generic Desktop", "Requests desktop versions with a generic Linux/Chrome desktop signature.")
}

enum class SearchEngine(val displayName: String, val searchUrl: String) {
    DUCKDUCKGO("DuckDuckGo (Private)", "https://duckduckgo.com/?q="),
    BRAVE("Brave Search (Private)", "https://search.brave.com/search?q="),
    STARTPAGE("Startpage (Private)", "https://www.startpage.com/sp/search?query="),
    GOOGLE("Google Search", "https://www.google.com/search?q="),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=")
}

data class PrivacySettingsState(
    val blockTrackers: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val clearDataOnExit: Boolean = true,
    val webrtcProtection: Boolean = true,
    val sendDoNotTrack: Boolean = true,
    val privacyProfile: PrivacyProfileType = PrivacyProfileType.GENERIC_ANDROID,
    val defaultSearchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val homepageUrl: String = "about:blank",
    val javaScriptEnabled: Boolean = true,
    val blockPopups: Boolean = true,
    val enforceHttps: Boolean = true,
    // Proxy configuration
    val proxyEnabled: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: Int = 8080,
    val proxyType: String = "HTTP" // HTTP or SOCKS
)

data class NetworkDiagnosticInfo(
    val isVpnConnected: Boolean = false,
    val connectionType: String = "Unknown",
    val publicIp: String? = null,
    val ipLocation: String? = null,
    val ispOrOrg: String? = null,
    val dnsServers: List<String> = emptyList(),
    val dnsSecOrDoH: String = "System Resolver",
    val webrtcStatus: WebRtcStatus = WebRtcStatus.PROTECTED,
    val lastTestedTimestamp: Long = 0L,
    val isTesting: Boolean = false,
    val errorMessage: String? = null
)

enum class WebRtcStatus(val label: String, val isSafe: Boolean) {
    PROTECTED("Protected (No Local IP Leak)", true),
    POTENTIAL_LEAK("Potential leak detected", false),
    UNABLE_TO_VERIFY("Unable to verify", false)
}
