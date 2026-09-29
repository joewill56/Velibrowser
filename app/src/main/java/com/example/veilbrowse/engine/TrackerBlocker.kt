package com.example.veilbrowse.engine

import android.net.Uri
import java.util.Locale

object TrackerBlocker {

    data class BlockResult(
        val isBlocked: Boolean,
        val domain: String,
        val category: String
    )

    private val ADVERTISING_DOMAINS = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "adnxs.com",
        "advertising.com",
        "criteo.com",
        "criteo.net",
        "outbrain.com",
        "taboola.com",
        "scorecardresearch.com",
        "adroll.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "amazon-adsystem.com",
        "media.net",
        "smartadserver.com",
        "chartbeat.com",
        "quantserve.com",
        "revcontent.com",
        "moatads.com",
        "ads-twitter.com",
        "inmobi.com",
        "vungle.com",
        "applovin.com",
        "ironsrc.com",
        "flurry.com",
        "tapjoy.com",
        "mintegral.com",
        "unityads.unity3d.com",
        "casalemedia.com",
        "popads.net",
        "zergnet.com",
        "adcolony.com"
    )

    private val ANALYTICS_DOMAINS = setOf(
        "google-analytics.com",
        "analytics.google.com",
        "hotjar.com",
        "segment.com",
        "segment.io",
        "mixpanel.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "amplitude.com",
        "fullstory.com",
        "crazyegg.com",
        "mouseflow.com",
        "clarity.ms",
        "newrelic.com",
        "heap.io",
        "matomo.org",
        "statcounter.com",
        "mc.yandex.ru",
        "yandex.ru/metrika",
        "optimizely.com",
        "kissmetrics.io",
        "logrocket.io"
    )

    private val SOCIAL_PIXELS = setOf(
        "connect.facebook.net",
        "facebook.com/tr",
        "pixel.wp.com",
        "analytics.tiktok.com",
        "snap.licdn.com",
        "tr.snapchat.com",
        "ct.pinterest.com"
    )

    private val FINGERPRINTING_DOMAINS = setOf(
        "fpjs.io",
        "fingerprint.com",
        "threatmetrix.com",
        "maxmind.com",
        "iovation.com",
        "siftscience.com"
    )

    fun extractHost(url: String): String? {
        val hostFromJava = try {
            java.net.URI(url).host
        } catch (_: Exception) {
            null
        }
        if (!hostFromJava.isNullOrBlank()) return hostFromJava.lowercase(Locale.ROOT)

        val hostFromAndroid = try {
            Uri.parse(url).host
        } catch (_: Exception) {
            null
        }
        if (!hostFromAndroid.isNullOrBlank()) return hostFromAndroid.lowercase(Locale.ROOT)

        val withoutScheme = url.substringAfter("://").substringBefore("/").substringBefore("?").substringBefore(":")
        return if (withoutScheme.isNotBlank()) withoutScheme.lowercase(Locale.ROOT) else null
    }

    fun checkUrl(url: String): BlockResult {
        val host = extractHost(url) ?: return BlockResult(isBlocked = false, domain = "", category = "")

        for (domain in ADVERTISING_DOMAINS) {
            if (host == domain || host.endsWith(".$domain")) {
                return BlockResult(isBlocked = true, domain = host, category = "Advertising Tracker")
            }
        }

        for (domain in ANALYTICS_DOMAINS) {
            if (host == domain || host.endsWith(".$domain")) {
                return BlockResult(isBlocked = true, domain = host, category = "Analytics / Telemetry")
            }
        }

        for (domain in SOCIAL_PIXELS) {
            if (host == domain || host.endsWith(".$domain")) {
                return BlockResult(isBlocked = true, domain = host, category = "Social Tracking Pixel")
            }
        }

        for (domain in FINGERPRINTING_DOMAINS) {
            if (host == domain || host.endsWith(".$domain")) {
                return BlockResult(isBlocked = true, domain = host, category = "Device Fingerprinter")
            }
        }

        return BlockResult(isBlocked = false, domain = host, category = "")
    }
}
