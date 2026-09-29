package com.example.veilbrowse.engine

import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView

class VeilWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onTitleReceived: (String) -> Unit
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
}
