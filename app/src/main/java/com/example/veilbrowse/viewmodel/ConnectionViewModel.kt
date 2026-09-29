package com.example.veilbrowse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veilbrowse.data.model.NetworkDiagnosticInfo
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.repository.NetworkRepository
import com.example.veilbrowse.data.repository.PrivacySettingsRepository
import com.example.veilbrowse.engine.ProxyManager
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

    private val _proxyStatusMessage = MutableStateFlow<String?>(null)
    val proxyStatusMessage: StateFlow<String?> = _proxyStatusMessage.asStateFlow()

    init {
        runNetworkDiagnostics()
    }

    fun runNetworkDiagnostics() {
        viewModelScope.launch {
            _networkInfo.value = _networkInfo.value.copy(isTesting = true, errorMessage = null)
            val info = networkRepo.runNetworkDiagnostics()
            _networkInfo.value = info
        }
    }

    fun saveProxyConfiguration(
        enabled: Boolean,
        host: String,
        port: Int,
        type: String
    ) {
        settingsRepo.updateSettings { current ->
            current.copy(
                proxyEnabled = enabled,
                proxyHost = host.trim(),
                proxyPort = port,
                proxyType = type
            )
        }

        if (enabled && host.isNotBlank()) {
            ProxyManager.applyProxy(
                host = host.trim(),
                port = port,
                type = type,
                onSuccess = {
                    _proxyStatusMessage.value = "Proxy successfully applied to browser engine."
                    runNetworkDiagnostics()
                },
                onError = { err ->
                    _proxyStatusMessage.value = "Proxy error: $err"
                }
            )
        } else {
            ProxyManager.clearProxy {
                _proxyStatusMessage.value = "Proxy cleared."
                runNetworkDiagnostics()
            }
        }
    }

    fun clearProxyStatusMessage() {
        _proxyStatusMessage.value = null
    }
}
