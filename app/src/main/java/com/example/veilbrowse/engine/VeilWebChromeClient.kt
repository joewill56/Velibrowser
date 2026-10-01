package com.example.veilbrowse.engine

import android.graphics.Bitmap
import android.os.Message
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class VeilWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onTitleReceived: (String) -> Unit,
    private val onOpenWindowRequested: ((url: String) -> Unit)? = null
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleReceived(it) }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        // Privacy enforcement: Always deny location requests from web origins
        callback?.invoke(origin, false, false)
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        // Allow standard alert dismissal
        return super.onJsAlert(view, url, message, result)
    }

    /**
     * Handles target="_blank" and window.open navigation requests.
     * Captures the destination URL from the new window request and directs the browser
     * to navigate to it rather than discarding the click.
     */
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (resultMsg == null) return false
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false

        val context = view?.context ?: return false
        val tempWebView = WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                    val destUrl = req?.url?.toString()
                    if (!destUrl.isNullOrBlank()) {
                        if (onOpenWindowRequested != null) {
                            onOpenWindowRequested.invoke(destUrl)
                        } else {
                            view.loadUrl(destUrl)
                        }
                    }
                    return true
                }

                override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                    if (!url.isNullOrBlank() && url != "about:blank") {
                        if (onOpenWindowRequested != null) {
                            onOpenWindowRequested.invoke(url)
                        } else {
                            view.loadUrl(url)
                        }
                    }
                }
            }
        }

        transport.webView = tempWebView
        resultMsg.sendToTarget()
        return true
    }
}
