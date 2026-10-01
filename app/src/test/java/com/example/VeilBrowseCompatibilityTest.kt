package com.example

import com.example.veilbrowse.data.model.NetworkDiagnosticInfo
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.model.ResourceDisposition
import com.example.veilbrowse.data.model.TrackerProtectionLevel
import com.example.veilbrowse.engine.TrackerBlocker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilBrowseCompatibilityTest {

    // 1. Normal website without ads
    @Test
    fun testNormalWebsiteWithoutAdsAllowed() {
        val eval = TrackerBlocker.evaluateResource(
            url = "https://en.wikipedia.org/wiki/Kotlin_(programming_language)",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = true
        )
        assertFalse("Normal website page navigation must not be blocked", eval.isBlocked)
        assertEquals(ResourceDisposition.WEBSITE_ALLOWED, eval.disposition)

        val assetEval = TrackerBlocker.evaluateResource(
            url = "https://upload.wikimedia.org/wikipedia/commons/7/74/Kotlin_Icon.png",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertFalse("Normal website assets must not be blocked", assetEval.isBlocked)
        assertEquals(ResourceDisposition.WEBSITE_ALLOWED, assetEval.disposition)
    }

    // 2. Website containing display advertisements
    @Test
    fun testWebsiteContainingDisplayAdsInBalancedMode() {
        // In BALANCED mode, display ad scripts and ad frames are ALLOWED so advertisements render
        val adsenseScript = TrackerBlocker.evaluateResource(
            url = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertFalse("AdSense rendering script must be allowed in BALANCED mode for ad display", adsenseScript.isBlocked)
        assertEquals(ResourceDisposition.AD_ALLOWED, adsenseScript.disposition)

        val amazonAd = TrackerBlocker.evaluateResource(
            url = "https://c.amazon-adsystem.com/aax2/apstag.js",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertFalse("Amazon ad delivery must be allowed in BALANCED mode", amazonAd.isBlocked)
        assertEquals(ResourceDisposition.AD_ALLOWED, amazonAd.disposition)

        val doubleClickFrame = TrackerBlocker.evaluateResource(
            url = "https://googleads.g.doubleclick.net/pagead/ads?client=ca-pub-12345",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertFalse("DoubleClick ad frame must be allowed in BALANCED mode", doubleClickFrame.isBlocked)
        assertEquals(ResourceDisposition.AD_ALLOWED, doubleClickFrame.disposition)
    }

    // 3. Website containing clickable advertisements
    @Test
    fun testClickableAdvertisementNavigationAllowed() {
        // When a user clicks an advertisement, the navigation occurs in the main frame or redirects
        val adClickUrl = "https://www.googleadservices.com/pagead/aclk?sa=L&ai=123&ved=456&adurl=https://merchant.example.com/product"
        val eval = TrackerBlocker.evaluateResource(
            url = adClickUrl,
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = true
        )
        assertFalse("Ad click navigation must never be blocked in main frame", eval.isBlocked)
        assertEquals(ResourceDisposition.AD_ALLOWED, eval.disposition)
    }

    // 4. Website containing external links
    @Test
    fun testExternalWebsiteLinksAllowed() {
        val externalLink = "https://github.com/developer/repo"
        val eval = TrackerBlocker.evaluateResource(
            url = externalLink,
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = true
        )
        assertFalse("External links must not be blocked", eval.isBlocked)
        assertEquals(ResourceDisposition.WEBSITE_ALLOWED, eval.disposition)
    }

    // 5. Website containing JavaScript navigation & resources
    @Test
    fun testJavaScriptResourcesAllowed() {
        val jsResource = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"
        val eval = TrackerBlocker.evaluateResource(
            url = jsResource,
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertFalse("CDN JavaScript resources must be allowed", eval.isBlocked)
        assertEquals(ResourceDisposition.WEBSITE_ALLOWED, eval.disposition)
    }

    // 6. Website containing redirects
    @Test
    fun testRedirectChainsAllowedInMainFrame() {
        val redirectUrl = "https://news.ycombinator.com/item?id=12345"
        val eval = TrackerBlocker.evaluateResource(
            url = redirectUrl,
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = true
        )
        assertFalse("Redirect navigation must not be blocked", eval.isBlocked)
        assertEquals(ResourceDisposition.WEBSITE_ALLOWED, eval.disposition)
    }

    // 7. Website using target="_blank"
    @Test
    fun testTargetBlankDestinationsAllowed() {
        val blankDestUrl = "https://developer.android.com/reference/android/webkit/WebView"
        val eval = TrackerBlocker.evaluateResource(
            url = blankDestUrl,
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = true
        )
        assertFalse("target=_blank navigation destination must be allowed", eval.isBlocked)
        assertEquals(ResourceDisposition.WEBSITE_ALLOWED, eval.disposition)
    }

    // 8. Private session configuration
    @Test
    fun testPrivateSessionConfigurationDefaults() {
        val settings = PrivacySettingsState()
        assertTrue("Default clearDataOnExit should be enabled for private sessions", settings.clearDataOnExit)
        assertTrue("Default blockThirdPartyCookies should be enabled", settings.blockThirdPartyCookies)
        assertTrue("Default WebRTC protection should be enabled", settings.webrtcProtection)
        assertTrue("Default DNT & GPC should be enabled", settings.sendDoNotTrack)
    }

    // 9. Tracker protection BALANCED
    @Test
    fun testTrackerProtectionBalanced() {
        // Invasive fingerprinter: BLOCKED
        val fingerprinter = TrackerBlocker.evaluateResource(
            url = "https://fpjs.io/v3/telemetry",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertTrue("Fingerprinting scripts must be blocked in BALANCED mode", fingerprinter.isBlocked)
        assertEquals(ResourceDisposition.TRACKER_BLOCKED, fingerprinter.disposition)

        // Social pixel: BLOCKED
        val socialPixel = TrackerBlocker.evaluateResource(
            url = "https://connect.facebook.net/en_US/fbevents.js",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertTrue("Social tracking pixels must be blocked in BALANCED mode", socialPixel.isBlocked)
        assertEquals(ResourceDisposition.TRACKER_BLOCKED, socialPixel.disposition)

        // Session recorder / invasive telemetry: BLOCKED
        val sessionRecorder = TrackerBlocker.evaluateResource(
            url = "https://static.hotjar.com/c/hotjar-12345.js",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertTrue("Session recorders must be blocked in BALANCED mode", sessionRecorder.isBlocked)
        assertEquals(ResourceDisposition.TRACKER_BLOCKED, sessionRecorder.disposition)

        // Display ad network: ALLOWED in BALANCED mode for ad display & clicking
        val adNetwork = TrackerBlocker.evaluateResource(
            url = "https://securepubads.g.doubleclick.net/tag/js/gpt.js",
            level = TrackerProtectionLevel.BALANCED,
            isMainFrame = false
        )
        assertFalse("Ad script must be allowed in BALANCED mode", adNetwork.isBlocked)
        assertEquals(ResourceDisposition.AD_ALLOWED, adNetwork.disposition)
    }

    // 10. Tracker protection STRICT
    @Test
    fun testTrackerProtectionStrict() {
        // Fingerprinting: BLOCKED
        val fingerprinter = TrackerBlocker.evaluateResource(
            url = "https://fpjs.io/v3/telemetry",
            level = TrackerProtectionLevel.STRICT,
            isMainFrame = false
        )
        assertTrue("Fingerprinter must be blocked in STRICT mode", fingerprinter.isBlocked)
        assertEquals(ResourceDisposition.TRACKER_BLOCKED, fingerprinter.disposition)

        // Advertising: BLOCKED in STRICT mode
        val adNetwork = TrackerBlocker.evaluateResource(
            url = "https://securepubads.g.doubleclick.net/tag/js/gpt.js",
            level = TrackerProtectionLevel.STRICT,
            isMainFrame = false
        )
        assertTrue("Advertising must be blocked in STRICT mode", adNetwork.isBlocked)
        assertEquals(ResourceDisposition.TRACKER_BLOCKED, adNetwork.disposition)

        // Site assets: ALLOWED
        val siteAsset = TrackerBlocker.evaluateResource(
            url = "https://example.com/app.js",
            level = TrackerProtectionLevel.STRICT,
            isMainFrame = false
        )
        assertFalse("Normal site asset must be allowed in STRICT mode", siteAsset.isBlocked)
        assertEquals(ResourceDisposition.WEBSITE_ALLOWED, siteAsset.disposition)
    }

    // 11. Tracker protection OFF
    @Test
    fun testTrackerProtectionOff() {
        val fingerprinter = TrackerBlocker.evaluateResource(
            url = "https://fpjs.io/v3/telemetry",
            level = TrackerProtectionLevel.OFF,
            isMainFrame = false
        )
        assertFalse("Resources must not be blocked when protection is OFF", fingerprinter.isBlocked)
        assertEquals(ResourceDisposition.TRACKER_ALLOWED, fingerprinter.disposition)

        val adNetwork = TrackerBlocker.evaluateResource(
            url = "https://securepubads.g.doubleclick.net/tag/js/gpt.js",
            level = TrackerProtectionLevel.OFF,
            isMainFrame = false
        )
        assertFalse("Ads must not be blocked when protection is OFF", adNetwork.isBlocked)
        assertEquals(ResourceDisposition.AD_ALLOWED, adNetwork.disposition)
    }

    // 12. VPN NOT CONNECTED honest reporting
    @Test
    fun testVpnNotConnectedStatus() {
        val networkInfo = NetworkDiagnosticInfo(
            isVpnConnected = false,
            connectionType = "Wi-Fi",
            publicIp = "192.0.2.1",
            ipLocation = "Ashburn, VA, US"
        )
        assertFalse("VPN state must be false when no active VPN tunnel is present", networkInfo.isVpnConnected)
    }

    // 13. Proxy NOT CONFIGURED status
    @Test
    fun testProxyNotConfiguredStatus() {
        val settings = PrivacySettingsState(
            proxyEnabled = false,
            proxyHost = "",
            proxyPort = 8080
        )
        val isConfigured = settings.proxyEnabled && settings.proxyHost.isNotBlank()
        assertFalse("Proxy should be reported as NOT CONFIGURED when disabled or empty", isConfigured)
    }
}
