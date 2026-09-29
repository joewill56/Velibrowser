package com.example.veilbrowse.engine

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.veilbrowse.data.db.TrackerDao
import com.example.veilbrowse.data.model.BlockedTrackerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class VeilWebViewClient(
    private val scope: CoroutineScope,
    private val trackerDao: TrackerDao,
    private val isTrackerBlockingEnabled: () -> Boolean,
    private val onPageTitleChanged: (String) -> Unit,
    private val onUrlChanged: (String) -> Unit,
    private val onLoadingStateChanged: (Boolean) -> Unit,
    private val onTrackerBlocked: (domain: String, category: String) -> Unit
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

        if (isTrackerBlockingEnabled()) {
            val check = TrackerBlocker.checkUrl(url)
            if (check.isBlocked) {
                scope.launch(Dispatchers.IO) {
                    trackerDao.insertBlockedTracker(
                        BlockedTrackerEntity(
                            domain = check.domain,
                            category = check.category,
                            blockedOnUrl = view?.url ?: url
                        )
                    )
                }
                onTrackerBlocked(check.domain, check.category)
                // Return 200 OK empty response to prevent site layout disruption while dropping tracker payload
                return WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    200,
                    "OK",
                    emptyMap(),
                    ByteArrayInputStream(ByteArray(0))
                )
            }
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onLoadingStateChanged(true)
        url?.let { onUrlChanged(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onLoadingStateChanged(false)
        url?.let { onUrlChanged(it) }
        view?.title?.let { onPageTitleChanged(it) }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // Enforce strict security: reject invalid/mitm certificates by default in privacy browser
        handler?.cancel()
    }
}
