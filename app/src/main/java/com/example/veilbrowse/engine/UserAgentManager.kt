package com.example.veilbrowse.engine

import com.example.veilbrowse.data.model.PrivacyProfileType

object UserAgentManager {
    // Chromium 128 standard generic token for Android
    private const val GENERIC_ANDROID_UA =
        "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    private const val GENERIC_MOBILE_UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"

    private const val GENERIC_DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    fun getUserAgent(type: PrivacyProfileType, defaultUa: String): String {
        return when (type) {
            PrivacyProfileType.STANDARD -> defaultUa
            PrivacyProfileType.GENERIC_ANDROID -> GENERIC_ANDROID_UA
            PrivacyProfileType.GENERIC_MOBILE -> GENERIC_MOBILE_UA
            PrivacyProfileType.GENERIC_DESKTOP -> GENERIC_DESKTOP_UA
        }
    }
}
