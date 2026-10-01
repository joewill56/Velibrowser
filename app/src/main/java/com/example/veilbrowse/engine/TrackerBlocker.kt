package com.example.veilbrowse.engine

import android.net.Uri
import com.example.veilbrowse.data.model.ResourceDisposition
import com.example.veilbrowse.data.model.TrackerProtectionLevel
import java.util.Locale

object TrackerBlocker {

    data class BlockResult(
        val isBlocked: Boolean,
        val domain: String,
        val category: String
    )

    data class ResourceEvaluation(
        val disposition: ResourceDisposition,
        val domain: String,
        val category: String,
        val isBlocked: Boolean
    )

    // Advertising & Display Ad Networks
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
        "adcolony.com",
        "thetradedesk.com",
        "bidswitch.net",
        "sharethrough.com",
        "yieldmo.com",
        "sovrn.com",
        "exponential.com"
    )

    // Invasive Session Recorders & Behavioral Telemetry
    private val INVASIVE_ANALYTICS = setOf(
        "hotjar.com",
        "fullstory.com",
        "crazyegg.com",
        "mouseflow.com",
        "clarity.ms",
        "logrocket.io",
        "optimizely.com",
        "kissmetrics.io",
        "segment.com",
        "segment.io",
        "mixpanel.com",
        "amplitude.com",
        "heap.io"
    )

    // Standard Telemetry & Site Analytics
    private val STANDARD_ANALYTICS = setOf(
        "google-analytics.com",
        "analytics.google.com",
        "statcounter.com",
        "matomo.org",
        "newrelic.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "mc.yandex.ru",
        "yandex.ru/metrika"
    )

    // Cross-Site Social Tracking Pixels
    private val SOCIAL_PIXELS = setOf(
        "connect.facebook.net",
        "facebook.com/tr",
        "pixel.wp.com",
        "analytics.tiktok.com",
        "snap.licdn.com",
        "tr.snapchat.com",
        "ct.pinterest.com"
    )

    // Device & Canvas Fingerprinting Networks
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

    private fun domainMatches(host: String, domainSet: Set<String>): Boolean {
        for (domain in domainSet) {
            if (host == domain || host.endsWith(".$domain")) {
                return true
            }
        }
        return false
    }

    /**
     * Evaluates a network resource request based on URL, the active TrackerProtectionLevel,
     * and whether the request is for the main frame.
     *
     * In BALANCED mode:
     * - Legitimate advertising scripts, display frames, and click redirection chains are ALLOWED
     *   so that websites display ads and buttons/click targets navigate normally.
     * - Invasive device fingerprinters, social tracking pixels, and session telemetry ARE BLOCKED.
     *
     * In STRICT mode:
     * - Both advertising networks and tracking/fingerprinting domains are intercepted.
     *
     * In OFF mode:
     * - No third-party resources are blocked.
     *
     * Main frame navigations (top-level page visits and ad click destinations) are NEVER blocked.
     */
    fun evaluateResource(
        url: String,
        level: TrackerProtectionLevel,
        isMainFrame: Boolean
    ): ResourceEvaluation {
        val host = extractHost(url) ?: return ResourceEvaluation(
            disposition = ResourceDisposition.WEBSITE_ALLOWED,
            domain = "",
            category = "Website Resource",
            isBlocked = false
        )

        // 1. Main frame navigation: Top-level navigation (user click, ad click redirection destination)
        // is always allowed so the user can reach their intended destination.
        if (isMainFrame) {
            val isAd = domainMatches(host, ADVERTISING_DOMAINS)
            val isTracker = domainMatches(host, FINGERPRINTING_DOMAINS) ||
                    domainMatches(host, SOCIAL_PIXELS) ||
                    domainMatches(host, INVASIVE_ANALYTICS) ||
                    domainMatches(host, STANDARD_ANALYTICS)

            return ResourceEvaluation(
                disposition = when {
                    isAd -> ResourceDisposition.AD_ALLOWED
                    isTracker -> ResourceDisposition.TRACKER_ALLOWED
                    else -> ResourceDisposition.WEBSITE_ALLOWED
                },
                domain = host,
                category = if (isAd) "Ad Destination Navigation" else "Website Navigation",
                isBlocked = false
            )
        }

        // 2. Protection is OFF: allow everything
        if (level == TrackerProtectionLevel.OFF) {
            val isAd = domainMatches(host, ADVERTISING_DOMAINS)
            val isTracker = domainMatches(host, FINGERPRINTING_DOMAINS) ||
                    domainMatches(host, SOCIAL_PIXELS) ||
                    domainMatches(host, INVASIVE_ANALYTICS) ||
                    domainMatches(host, STANDARD_ANALYTICS)

            return ResourceEvaluation(
                disposition = when {
                    isAd -> ResourceDisposition.AD_ALLOWED
                    isTracker -> ResourceDisposition.TRACKER_ALLOWED
                    else -> ResourceDisposition.WEBSITE_ALLOWED
                },
                domain = host,
                category = when {
                    isAd -> "Advertisement Resource"
                    isTracker -> "Tracker / Analytics"
                    else -> "Website Resource"
                },
                isBlocked = false
            )
        }

        // 3. Check high-confidence Fingerprinters (blocked in BALANCED & STRICT)
        if (domainMatches(host, FINGERPRINTING_DOMAINS)) {
            return ResourceEvaluation(
                disposition = ResourceDisposition.TRACKER_BLOCKED,
                domain = host,
                category = "Device Fingerprinter",
                isBlocked = true
            )
        }

        // 4. Check Social Tracking Pixels (blocked in BALANCED & STRICT)
        if (domainMatches(host, SOCIAL_PIXELS)) {
            return ResourceEvaluation(
                disposition = ResourceDisposition.TRACKER_BLOCKED,
                domain = host,
                category = "Social Tracking Pixel",
                isBlocked = true
            )
        }

        // 5. Check Invasive Analytics & Session Recorders (blocked in BALANCED & STRICT)
        if (domainMatches(host, INVASIVE_ANALYTICS)) {
            return ResourceEvaluation(
                disposition = ResourceDisposition.TRACKER_BLOCKED,
                domain = host,
                category = "Session Recorder / Analytics",
                isBlocked = true
            )
        }

        // 6. Check Standard Analytics (blocked in BALANCED & STRICT)
        if (domainMatches(host, STANDARD_ANALYTICS)) {
            return ResourceEvaluation(
                disposition = ResourceDisposition.TRACKER_BLOCKED,
                domain = host,
                category = "Analytics / Telemetry",
                isBlocked = true
            )
        }

        // 7. Check Advertising Domains
        if (domainMatches(host, ADVERTISING_DOMAINS)) {
            return if (level == TrackerProtectionLevel.STRICT) {
                // In STRICT mode: block advertising resources
                ResourceEvaluation(
                    disposition = ResourceDisposition.TRACKER_BLOCKED,
                    domain = host,
                    category = "Advertising Network",
                    isBlocked = true
                )
            } else {
                // In BALANCED mode: ALLOW advertising resources so ad scripts, display banners,
                // iframe containers, and interactive click targets render and operate normally.
                ResourceEvaluation(
                    disposition = ResourceDisposition.AD_ALLOWED,
                    domain = host,
                    category = "Advertisement Resource",
                    isBlocked = false
                )
            }
        }

        // 8. General website resource
        return ResourceEvaluation(
            disposition = ResourceDisposition.WEBSITE_ALLOWED,
            domain = host,
            category = "Website Resource",
            isBlocked = false
        )
    }

    /**
     * Backward-compatible checkUrl method used by tests or legacy callers.
     * Evaluates in BALANCED mode for background sub-resources.
     */
    fun checkUrl(url: String): BlockResult {
        val eval = evaluateResource(url, TrackerProtectionLevel.BALANCED, isMainFrame = false)
        return BlockResult(
            isBlocked = eval.isBlocked,
            domain = eval.domain,
            category = eval.category
        )
    }
}
