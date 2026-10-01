package com.example

import com.example.veilbrowse.data.model.PrivacyProfileType
import com.example.veilbrowse.data.model.TrackerProtectionLevel
import com.example.veilbrowse.engine.TrackerBlocker
import com.example.veilbrowse.engine.UserAgentManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun trackerBlocker_blocksAdvertisingInStrictMode() {
    val strictAdCheck = TrackerBlocker.evaluateResource(
        url = "https://adservice.google.com/ads/test",
        level = TrackerProtectionLevel.STRICT,
        isMainFrame = false
    )
    assertTrue("In STRICT mode, advertising should be blocked", strictAdCheck.isBlocked)
    assertEquals("Advertising Network", strictAdCheck.category)
  }

  @Test
  fun trackerBlocker_balancedModeAllowsAdsWhileBlockingAnalyticsAndPixels() {
    // In BALANCED mode, advertising is allowed for compatibility and click navigation
    val adCheck = TrackerBlocker.evaluateResource(
        url = "https://adservice.google.com/ads/test",
        level = TrackerProtectionLevel.BALANCED,
        isMainFrame = false
    )
    assertFalse("In BALANCED mode, legitimate ad resources should be allowed for rendering", adCheck.isBlocked)

    // Analytics and Telemetry are blocked in BALANCED mode
    val analyticsCheck = TrackerBlocker.evaluateResource(
        url = "https://www.google-analytics.com/analytics.js",
        level = TrackerProtectionLevel.BALANCED,
        isMainFrame = false
    )
    assertTrue("In BALANCED mode, analytics telemetry should be blocked", analyticsCheck.isBlocked)

    // Facebook Pixel is blocked in BALANCED mode
    val fbPixelCheck = TrackerBlocker.evaluateResource(
        url = "https://connect.facebook.net/en_US/fbevents.js",
        level = TrackerProtectionLevel.BALANCED,
        isMainFrame = false
    )
    assertTrue("In BALANCED mode, social tracking pixels should be blocked", fbPixelCheck.isBlocked)

    // Safe website is allowed
    val safeSiteCheck = TrackerBlocker.evaluateResource(
        url = "https://duckduckgo.com",
        level = TrackerProtectionLevel.BALANCED,
        isMainFrame = true
    )
    assertFalse("Safe sites must be allowed", safeSiteCheck.isBlocked)
  }

  @Test
  fun userAgentManager_stripsDeviceHardwareModels() {
    val genericAndroid = UserAgentManager.getUserAgent(PrivacyProfileType.GENERIC_ANDROID, "CustomUA")
    assertTrue("Generic Android UA should contain standard token", genericAndroid.contains("Android 14; K"))
    assertFalse("Generic Android UA should not contain phone brand names", genericAndroid.contains("Pixel"))

    val genericMobile = UserAgentManager.getUserAgent(PrivacyProfileType.GENERIC_MOBILE, "CustomUA")
    assertTrue("Generic Mobile should contain Mobile Safari token", genericMobile.contains("Safari"))
  }
}
