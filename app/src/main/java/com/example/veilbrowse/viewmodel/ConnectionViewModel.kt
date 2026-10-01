package com.example.veilbrowse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veilbrowse.data.model.NetworkDiagnosticInfo
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.model.ProxyConnectionState
import com.example.veilbrowse.data.model.ProxyStatusInfo
import com.example.veilbrowse.data.repository.NetworkRepository
import com.example.veilbrowse.data.repository.PrivacySettingsRepository
import com.example.veilbrowse.engine.ProxyManager
import com.example.veilbrowse.engine.ProxyTestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val networkRepo = NetworkRepository(application)
    private val settingsRepo = PrivacySettingsRepository(application)

    val settings: StateFlow<PrivacySettingsState> = settingsRepo.settings

    private val _networkInfo = MutableStateFlow(NetworkDiagnosticInfo())
    val networkInfo: StateFlow<NetworkDiagnosticInfo> = _networkInfo.asStateFlow()

    private val _proxyStatus = MutableStateFlow(initProxyStatus())
    val proxyStatus: StateFlow<ProxyStatusInfo> = _proxyStatus.asStateFlow()

    private val _proxyStatusMessage = MutableStateFlow<String?>(null)
    val proxyStatusMessage: StateFlow<String?> = _proxyStatusMessage.asStateFlow()

    init {
        runNetworkDiagnostics()
    }

    private fun initProxyStatus(): ProxyStatusInfo {
        val currentSettings = settingsRepo.settings.value
        return if (currentSettings.proxyEnabled && currentSettings.proxyHost.isNotBlank()) {
            ProxyStatusInfo(
                state = ProxyConnectionState.CONFIGURED_NOT_VERIFIED,
                proxyType = currentSettings.proxyType,
                host = currentSettings.proxyHost,
                port = currentSettings.proxyPort,
                hasUsername = currentSettings.proxyUsername.isNotBlank(),
                isAppliedToWebView = false
            )
        } else {
            ProxyStatusInfo(state = ProxyConnectionState.NOT_CONFIGURED)
        }
    }

    fun runNetworkDiagnostics() {
        viewModelScope.launch {
            _networkInfo.value = _networkInfo.value.copy(isTesting = true, errorMessage = null)
            val info = networkRepo.runNetworkDiagnostics()
            _networkInfo.value = info
        }
    }

    /**
     * Saves proxy configuration without considering it "connected" yet.
     * Transitions state to CONFIGURED_NOT_VERIFIED.
     */
    fun saveProxyConfiguration(
        enabled: Boolean,
        host: String,
        port: Int,
        type: String,
        username: String = "",
        password: String = ""
    ) {
        val trimmedHost = host.trim()
        settingsRepo.updateSettings { current ->
            current.copy(
                proxyEnabled = enabled,
                proxyHost = trimmedHost,
                proxyPort = port,
                proxyType = type,
                proxyUsername = username.trim(),
                proxyPassword = password
            )
        }

        if (!enabled || trimmedHost.isBlank()) {
            ProxyManager.clearProxy {
                _proxyStatus.value = ProxyStatusInfo(state = ProxyConnectionState.NOT_CONFIGURED)
                _proxyStatusMessage.value = "Proxy cleared and disabled."
                runNetworkDiagnostics()
            }
        } else {
            _proxyStatus.value = ProxyStatusInfo(
                state = ProxyConnectionState.CONFIGURED_NOT_VERIFIED,
                proxyType = type,
                host = trimmedHost,
                port = port,
                hasUsername = username.isNotBlank(),
                isAppliedToWebView = false
            )
            _proxyStatusMessage.value = "Configuration saved. Press 'Test & Connect' to verify."
        }
    }

    /**
     * Executes the full:
     * TESTING → APPLY TO WEBVIEW → REAL NETWORK REQUEST → VERIFY → CONNECTED / FAILED flow.
     */
    fun testAndConnectProxy(
        host: String,
        port: Int,
        type: String,
        username: String = "",
        password: String = ""
    ) {
        val trimmedHost = host.trim()
        if (trimmedHost.isBlank()) {
            _proxyStatus.value = ProxyStatusInfo(
                state = ProxyConnectionState.FAILED,
                failureReason = "Host cannot be empty"
            )
            _proxyStatusMessage.value = "Please enter a valid proxy host."
            return
        }

        viewModelScope.launch {
            // 1. Transition to TESTING
            _proxyStatus.value = _proxyStatus.value.copy(
                state = ProxyConnectionState.TESTING,
                proxyType = type,
                host = trimmedHost,
                port = port,
                hasUsername = username.isNotBlank(),
                failureReason = null
            )
            _proxyStatusMessage.value = "Testing proxy connection through real network probe..."

            // 2. Apply proxy override to WebView
            ProxyManager.applyProxy(
                host = trimmedHost,
                port = port,
                type = type
            )

            // 3. Make real network request through the proxy to verify actual connectivity
            val result = ProxyManager.testProxyConnection(
                host = trimmedHost,
                port = port,
                type = type,
                username = username.takeIf { it.isNotBlank() },
                password = password.takeIf { it.isNotBlank() }
            )

            when (result) {
                is ProxyTestResult.Success -> {
                    // 4. Verification succeeded: transition to CONNECTED
                    _proxyStatus.value = ProxyStatusInfo(
                        state = ProxyConnectionState.CONNECTED,
                        proxyType = type,
                        host = trimmedHost,
                        port = port,
                        hasUsername = username.isNotBlank(),
                        testTimestamp = System.currentTimeMillis(),
                        observedPublicIp = result.observedIp,
                        observedLocation = result.observedLocation,
                        responseTimeMs = result.responseTimeMs,
                        failureReason = null,
                        isAppliedToWebView = true
                    )
                    _proxyStatusMessage.value = "Verified & Connected! Public IP: ${result.observedIp} (${result.responseTimeMs} ms)"

                    // Update settings repository
                    settingsRepo.updateSettings { current ->
                        current.copy(
                            proxyEnabled = true,
                            proxyHost = trimmedHost,
                            proxyPort = port,
                            proxyType = type,
                            proxyUsername = username.trim(),
                            proxyPassword = password
                        )
                    }

                    // Update live network info display
                    _networkInfo.value = _networkInfo.value.copy(
                        publicIp = result.observedIp,
                        ipLocation = result.observedLocation ?: _networkInfo.value.ipLocation
                    )
                }

                is ProxyTestResult.Failure -> {
                    // 5. Verification failed: clear from WebView so user is not blocked
                    ProxyManager.clearProxy()

                    _proxyStatus.value = ProxyStatusInfo(
                        state = ProxyConnectionState.FAILED,
                        proxyType = type,
                        host = trimmedHost,
                        port = port,
                        hasUsername = username.isNotBlank(),
                        testTimestamp = System.currentTimeMillis(),
                        failureReason = result.errorMessage,
                        responseTimeMs = result.responseTimeMs,
                        isAppliedToWebView = false
                    )
                    _proxyStatusMessage.value = "Connection Failed: ${result.errorMessage}"
                }
            }
        }
    }

    /**
     * Disconnects active proxy, removes override from WebView, and marks state as DISCONNECTED.
     */
    fun disconnectProxy() {
        ProxyManager.clearProxy {
            settingsRepo.updateSettings { it.copy(proxyEnabled = false) }
            _proxyStatus.value = _proxyStatus.value.copy(
                state = ProxyConnectionState.DISCONNECTED,
                isAppliedToWebView = false
            )
            _proxyStatusMessage.value = "Proxy disconnected. Restoring direct connection."
            runNetworkDiagnostics()
        }
    }

    fun clearProxyStatusMessage() {
        _proxyStatusMessage.value = null
    }
}
