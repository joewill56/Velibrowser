package com.example.veilbrowse.engine

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

sealed class ProxyTestResult {
    data class Success(
        val observedIp: String,
        val observedLocation: String?,
        val responseTimeMs: Long
    ) : ProxyTestResult()

    data class Failure(
        val errorMessage: String,
        val responseTimeMs: Long? = null
    ) : ProxyTestResult()
}

object ProxyManager {
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Executes a real network request routed through the specified proxy server
     * using OkHttp and java.net.Proxy to verify real-world connectivity, latency,
     * and observed public egress IP.
     *
     * Note: Never logs or exposes password.
     */
    suspend fun testProxyConnection(
        host: String,
        port: Int,
        type: String = "HTTP",
        username: String? = null,
        password: String? = null
    ): ProxyTestResult = withContext(Dispatchers.IO) {
        val trimmedHost = host.trim()
        if (trimmedHost.isBlank()) {
            return@withContext ProxyTestResult.Failure("Host is empty")
        }
        if (port !in 1..65535) {
            return@withContext ProxyTestResult.Failure("Invalid port number ($port). Port must be between 1 and 65535.")
        }

        val proxyType = if (type.equals("SOCKS", ignoreCase = true)) {
            Proxy.Type.SOCKS
        } else {
            Proxy.Type.HTTP
        }

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)

        try {
            val socketAddress = InetSocketAddress(trimmedHost, port)
            val proxy = Proxy(proxyType, socketAddress)
            clientBuilder.proxy(proxy)

            if (!username.isNullOrBlank()) {
                clientBuilder.proxyAuthenticator { _, response ->
                    val credential = Credentials.basic(username, password ?: "")
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
            }
        } catch (e: Exception) {
            return@withContext ProxyTestResult.Failure("Proxy address initialization error: ${e.localizedMessage ?: "Invalid configuration"}")
        }

        val client = clientBuilder.build()
        val startTime = System.currentTimeMillis()

        try {
            // Test request to public IP reflection endpoint through the proxy
            val request = Request.Builder()
                .url("https://api.ipify.org?format=json")
                .header("User-Agent", "VeilBrowse-Proxy-Verifier/1.0")
                .build()

            val response = client.newCall(request).execute()
            val latencyMs = System.currentTimeMillis() - startTime

            if (response.code == 407) {
                return@withContext ProxyTestResult.Failure(
                    "Proxy authentication required (HTTP 407). Please verify username and credentials.",
                    latencyMs
                )
            }

            if (!response.isSuccessful) {
                return@withContext ProxyTestResult.Failure(
                    "Proxy responded with HTTP error status: ${response.code} ${response.message}",
                    latencyMs
                )
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val ip = json.optString("ip", "").trim()
            if (ip.isBlank()) {
                return@withContext ProxyTestResult.Failure("Proxy connection succeeded but failed to parse public IP", latencyMs)
            }

            // Attempt secondary lightweight query through the proxy for geolocation if possible
            var location: String? = null
            try {
                val geoReq = Request.Builder()
                    .url("https://ipapi.co/$ip/json/")
                    .header("User-Agent", "VeilBrowse-Proxy-Verifier/1.0")
                    .build()
                val geoResp = client.newCall(geoReq).execute()
                if (geoResp.isSuccessful) {
                    val geoBody = geoResp.body?.string()
                    if (!geoBody.isNullOrBlank()) {
                        val geoJson = JSONObject(geoBody)
                        val city = geoJson.optString("city", "")
                        val region = geoJson.optString("region", "")
                        val country = geoJson.optString("country_name", "")
                        val locParts = listOf(city, region, country).filter { it.isNotBlank() }
                        if (locParts.isNotEmpty()) {
                            location = locParts.joinToString(", ")
                        }
                    }
                }
            } catch (_: Exception) {
                // Secondary location check is optional
            }

            ProxyTestResult.Success(
                observedIp = ip,
                observedLocation = location,
                responseTimeMs = latencyMs
            )
        } catch (e: ConnectException) {
            val latencyMs = System.currentTimeMillis() - startTime
            ProxyTestResult.Failure(
                "Connection refused. Ensure the proxy server is running and accessible on $trimmedHost:$port.",
                latencyMs
            )
        } catch (e: SocketTimeoutException) {
            val latencyMs = System.currentTimeMillis() - startTime
            ProxyTestResult.Failure(
                "Connection timed out (10s). The proxy server did not respond in time.",
                latencyMs
            )
        } catch (e: UnknownHostException) {
            val latencyMs = System.currentTimeMillis() - startTime
            ProxyTestResult.Failure(
                "Cannot resolve host '$trimmedHost'. Check server hostname and network DNS.",
                latencyMs
            )
        } catch (e: Exception) {
            val latencyMs = System.currentTimeMillis() - startTime
            val msg = e.localizedMessage ?: e.javaClass.simpleName
            ProxyTestResult.Failure("Proxy test failed: $msg", latencyMs)
        }
    }

    /**
     * Applies the proxy override configuration to Android WebView.
     */
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

    /**
     * Clears any active proxy override from Android WebView.
     */
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
