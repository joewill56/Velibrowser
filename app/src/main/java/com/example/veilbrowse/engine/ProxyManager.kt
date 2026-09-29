package com.example.veilbrowse.engine

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executors

object ProxyManager {
    private val executor = Executors.newSingleThreadExecutor()

    fun applyProxy(
        host: String,
        port: Int,
        type: String = "HTTP",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onError("Proxy override is not supported on this Android WebView engine.")
            return
        }

        try {
            val scheme = if (type.equals("SOCKS", ignoreCase = true)) "socks" else "http"
            val proxyUrl = "$scheme://$host:$port"

            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .addBypassRule("<local>")
                .addBypassRule("127.0.0.1")
                .addBypassRule("localhost")
                .build()

            ProxyController.getInstance().setProxyOverride(
                proxyConfig,
                executor,
                Runnable {
                    onSuccess()
                }
            )
        } catch (e: Exception) {
            onError(e.message ?: "Failed to apply proxy configuration.")
        }
    }

    fun clearProxy(onComplete: () -> Unit = {}) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                ProxyController.getInstance().clearProxyOverride(
                    executor,
                    Runnable {
                        onComplete()
                    }
                )
                return
            } catch (_: Exception) {}
        }
        onComplete()
    }
}
