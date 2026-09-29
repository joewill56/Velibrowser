package com.example

import com.example.veilbrowse.data.model.PrivacyProfileType
import com.example.veilbrowse.engine.TrackerBlocker
import com.example.veilbrowse.engine.UserAgentManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun trackerBlocker_blocksAdvertisingAndAnalytics() {
    val adCheck = TrackerBlocker.checkUrl("https://adservice.google.com/ads/test")
    assertTrue("Should block adservice.google.com", adCheck.isBlocked)
    assertEquals("Advertising Tracker", adCheck.category)

    val doubleclickCheck = TrackerBlocker.checkUrl("https://stats.doubleclick.net/r/collect")
    assertTrue("Should block doubleclick.net", doubleclickCheck.isBlocked)

    val analyticsCheck = TrackerBlocker.checkUrl("https://www.google-analytics.com/analytics.js")
    assertTrue("Should block analytics.google.com", analyticsCheck.isBlocked)

    val fbPixelCheck = TrackerBlocker.checkUrl("https://connect.facebook.net/en_US/fbevents.js")
    assertTrue("Should block Facebook pixel", fbPixelCheck.isBlocked)

    val safeSiteCheck = TrackerBlocker.checkUrl("https://duckduckgo.com")
    assertFalse("Should allow duckduckgo.com", safeSiteCheck.isBlocked)
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
