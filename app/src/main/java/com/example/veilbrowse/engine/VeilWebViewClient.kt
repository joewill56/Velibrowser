package com.example.veilbrowse.engine

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.veilbrowse.data.db.TrackerDao
import com.example.veilbrowse.data.model.BlockedTrackerEntity
import com.example.veilbrowse.data.model.InspectedResource
import com.example.veilbrowse.data.model.TrackerProtectionLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.util.Locale

class VeilWebViewClient(
    private val scope: CoroutineScope,
    private val trackerDao: TrackerDao,
    private val getProtectionLevel: () -> TrackerProtectionLevel,
    private val onPageTitleChanged: (String) -> Unit,
    private val onUrlChanged: (String) -> Unit,
    private val onLoadingStateChanged: (Boolean) -> Unit,
    private val onResourceInspected: (InspectedResource) -> Unit,
    private val onTrackerBlocked: (domain: String, category: String) -> Unit,
    private val onNavigationError: ((url: String, description: String) -> Unit)? = null
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val uri = request?.url ?: return false
        val url = uri.toString()
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""

        // Standard web navigation: allow WebView to handle HTTP/HTTPS, redirects, and data URIs
        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "javascript") {
            return false
        }

        // Handle external intent schemes, app store links, mailto, tel, etc.
        return try {
            val intent = if (url.startsWith("intent:", ignoreCase = true)) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val context = view?.context ?: return false
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // If the target app is not installed, attempt fallback URL specified in intent
            if (url.startsWith("intent:", ignoreCase = true)) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (!fallbackUrl.isNullOrBlank()) {
                        view?.loadUrl(fallbackUrl)
                        return true
                    }
                } catch (_: Exception) {}
            }
            onNavigationError?.invoke(url, "No application available to open link: $scheme")
            false
        }
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
        val isMainFrame = request.isForMainFrame
        val level = getProtectionLevel()

        val eval = TrackerBlocker.evaluateResource(
            url = url,
            level = level,
            isMainFrame = isMainFrame
        )

        // Always record resource inspection for the Inspector sheet
        val inspected = InspectedResource(
            domain = eval.domain,
            category = eval.category,
            disposition = eval.disposition,
            timestamp = System.currentTimeMillis(),
            url = url
        )
        onResourceInspected(inspected)

        // Only block if evaluation explicitly determines it is blocked
        if (eval.isBlocked) {
            scope.launch(Dispatchers.IO) {
                trackerDao.insertBlockedTracker(
                    BlockedTrackerEntity(
                        domain = eval.domain,
                        category = eval.category,
                        blockedOnUrl = view?.url ?: url
                    )
                )
            }
            onTrackerBlocked(eval.domain, eval.category)

            // Return 200 OK empty response to prevent site layout disruption while dropping tracking payload
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                200,
                "OK",
                emptyMap(),
                ByteArrayInputStream(ByteArray(0))
            )
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

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val description = error?.description?.toString() ?: "Navigation failed"
            onNavigationError?.invoke(request.url.toString(), description)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // Enforce strict security: reject invalid/mitm certificates by default in privacy browser
        handler?.cancel()
        onNavigationError?.invoke(view?.url ?: "", "Security warning: SSL certificate verification failed")
    }
}
