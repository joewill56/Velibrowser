package com.example.veilbrowse.engine

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

object SessionCleaner {

    fun clearCookies(onComplete: (() -> Unit)? = null) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {
            cookieManager.flush()
            onComplete?.invoke()
        }
    }

    fun clearStorage() {
        try {
            WebStorage.getInstance().deleteAllData()
        } catch (_: Exception) {}
    }

    fun clearCache(context: Context, webView: WebView? = null) {
        try {
            webView?.clearCache(true)
            webView?.clearFormData()
            webView?.clearHistory()
            context.cacheDir.deleteRecursively()
        } catch (_: Exception) {}
    }

    fun startFreshPrivateSession(
        context: Context,
        currentWebView: WebView?,
        onComplete: () -> Unit
    ) {
        clearCookies {
            clearStorage()
            clearCache(context, currentWebView)
            onComplete()
        }
    }
}
