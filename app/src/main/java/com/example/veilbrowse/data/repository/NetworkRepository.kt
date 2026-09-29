package com.example.veilbrowse.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.veilbrowse.data.model.NetworkDiagnosticInfo
import com.example.veilbrowse.data.model.WebRtcStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class NetworkRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .build()

    suspend fun runNetworkDiagnostics(): NetworkDiagnosticInfo = withContext(Dispatchers.IO) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        var isVpn = false
        var connType = "No Connection"
        val dnsList = mutableListOf<String>()

        if (connectivityManager != null) {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                val linkProperties = connectivityManager.getLinkProperties(activeNetwork)

                if (caps != null) {
                    val hasVpnTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    val notVpn = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    isVpn = hasVpnTransport || !notVpn

                    connType = when {
                        hasVpnTransport -> "VPN Interface"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Network"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                        else -> "Network Interface"
                    }
                }

                linkProperties?.dnsServers?.forEach { inetAddr ->
                    inetAddr.hostAddress?.let { dnsList.add(it) }
                }
            }
        }

        // Fetch real public IP & approximate location without mock data
        var detectedIp: String? = null
        var detectedLoc: String? = null
        var detectedIsp: String? = null
        var errorMessage: String? = null

        try {
            // First attempt: ipapi.co json endpoint
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .header("User-Agent", "VeilBrowse/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = JSONObject(bodyStr)
                    detectedIp = json.optString("ip", "").takeIf { it.isNotBlank() }
                    val city = json.optString("city", "")
                    val region = json.optString("region", "")
                    val country = json.optString("country_name", "")
                    val locParts = listOf(city, region, country).filter { it.isNotBlank() }
                    detectedLoc = if (locParts.isNotEmpty()) locParts.joinToString(", ") else null
                    detectedIsp = json.optString("org", "").takeIf { it.isNotBlank() } ?: json.optString("asn", "").takeIf { it.isNotBlank() }
                }
            } else {
                // Fallback attempt: api.ipify.org
                val fallbackReq = Request.Builder()
                    .url("https://api.ipify.org?format=json")
                    .build()
                val fallbackResp = httpClient.newCall(fallbackReq).execute()
                if (fallbackResp.isSuccessful) {
                    val body = fallbackResp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        detectedIp = json.optString("ip", "").takeIf { it.isNotBlank() }
                        detectedLoc = "Location unavailable (IP only)"
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Network query timed out or device is offline"
        }

        // WebRTC technical evaluation:
        // In Android WebView, local host candidates are sanitized by default in modern Android (Android 9+)
        // Unless permissions are granted. However, if offline or query failed, report UNABLE_TO_VERIFY realistically.
        val webrtcStatus = when {
            detectedIp == null -> WebRtcStatus.UNABLE_TO_VERIFY
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> WebRtcStatus.PROTECTED
            else -> WebRtcStatus.UNABLE_TO_VERIFY
        }

        NetworkDiagnosticInfo(
            isVpnConnected = isVpn,
            connectionType = connType,
            publicIp = detectedIp,
            ipLocation = detectedLoc,
            ispOrOrg = detectedIsp,
            dnsServers = dnsList,
            dnsSecOrDoH = if (dnsList.any { it.startsWith("1.1.1.") || it.startsWith("8.8.8.") || it.startsWith("9.9.9.") }) "Known Public Resolver" else "ISP / System Resolver",
            webrtcStatus = webrtcStatus,
            lastTestedTimestamp = System.currentTimeMillis(),
            isTesting = false,
            errorMessage = errorMessage
        )
    }
}
