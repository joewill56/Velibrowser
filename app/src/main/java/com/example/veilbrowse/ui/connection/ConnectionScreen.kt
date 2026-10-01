package com.example.veilbrowse.ui.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningYellow
import com.example.veilbrowse.data.model.NetworkDiagnosticInfo
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.model.ProxyConnectionState
import com.example.veilbrowse.data.model.ProxyStatusInfo
import com.example.veilbrowse.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    networkInfo: NetworkDiagnosticInfo,
    settings: PrivacySettingsState,
    proxyStatus: ProxyStatusInfo,
    proxyStatusMessage: String?,
    onSaveProxy: (enabled: Boolean, host: String, port: Int, type: String, username: String, password: String) -> Unit,
    onTestAndConnectProxy: (host: String, port: Int, type: String, username: String, password: String) -> Unit,
    onDisconnectProxy: () -> Unit,
    onRefresh: () -> Unit,
    onClearStatusMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var proxyHost by remember(settings.proxyHost) { mutableStateOf(settings.proxyHost) }
    var proxyPort by remember(settings.proxyPort) { mutableStateOf(settings.proxyPort.toString()) }
    var proxyType by remember(settings.proxyType) { mutableStateOf(settings.proxyType) }
    var proxyUsername by remember(settings.proxyUsername) { mutableStateOf(settings.proxyUsername) }
    var proxyPassword by remember(settings.proxyPassword) { mutableStateOf(settings.proxyPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var proxyTypeExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("connection_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Privacy Connection",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "VPN & Proxy routing configuration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_network_status_button")
                ) {
                    if (networkInfo.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        }

        // Section 1: Live Connection Status Card (Distinct VPN and Proxy reporting)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("live_connection_status_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = if (networkInfo.isVpnConnected) SafeGreen else WarningYellow,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONNECTION STATUS",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Explicit System VPN vs. Browser Proxy Status Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatusBadge(
                                text = if (networkInfo.isVpnConnected) "VPN CONNECTED" else "VPN NOT CONNECTED",
                                isSafe = networkInfo.isVpnConnected
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (proxyStatus.state) {
                                ProxyConnectionState.CONNECTED -> StatusBadge(text = "PROXY CONNECTED", isSafe = true)
                                ProxyConnectionState.FAILED -> StatusBadge(text = "PROXY FAILED", isSafe = false)
                                ProxyConnectionState.TESTING -> StatusBadge(text = "PROXY TESTING…", isSafe = false)
                                ProxyConnectionState.CONFIGURED_NOT_VERIFIED -> StatusBadge(text = "PROXY UNVERIFIED", isSafe = false)
                                ProxyConnectionState.DISCONNECTED -> StatusBadge(text = "PROXY DISCONNECTED", isSafe = false)
                                ProxyConnectionState.NOT_CONFIGURED -> StatusBadge(text = "PROXY NOT CONFIGURED", isSafe = false)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ConnectionMetricRow("Current Public IP", networkInfo.publicIp ?: "Offline / Detecting")
                    Spacer(modifier = Modifier.height(8.dp))
                    ConnectionMetricRow("Approx. IP Location", networkInfo.ipLocation ?: "Unavailable")
                    Spacer(modifier = Modifier.height(8.dp))
                    ConnectionMetricRow("Connection Type", networkInfo.connectionType)
                    Spacer(modifier = Modifier.height(8.dp))
                    ConnectionMetricRow("DNS Resolver", networkInfo.dnsSecOrDoH)
                    Spacer(modifier = Modifier.height(8.dp))
                    ConnectionMetricRow("WebRTC Leak Check", networkInfo.webrtcStatus.label)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Real telemetry: VeilBrowse reflects actual network probe responses and does not fabricate fake IP or location data.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 2: Browser Proxy Configuration & Verification Flow
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("proxy_configuration_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BROWSER PROXY",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        // State chip
                        val (stateColor, stateLabel) = when (proxyStatus.state) {
                            ProxyConnectionState.CONNECTED -> Pair(SafeGreen, "CONNECTED")
                            ProxyConnectionState.FAILED -> Pair(DangerRed, "CONNECTION FAILED")
                            ProxyConnectionState.TESTING -> Pair(CyanPrimary, "TESTING")
                            ProxyConnectionState.CONFIGURED_NOT_VERIFIED -> Pair(WarningYellow, "CONFIGURED / NOT VERIFIED")
                            ProxyConnectionState.DISCONNECTED -> Pair(MaterialTheme.colorScheme.outline, "DISCONNECTED")
                            ProxyConnectionState.NOT_CONFIGURED -> Pair(MaterialTheme.colorScheme.outline, "NOT CONFIGURED")
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(stateColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("proxy_state_badge")
                        ) {
                            Text(
                                text = stateLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = stateColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Routes VeilBrowse web traffic through an HTTP or SOCKS5 proxy server. To prevent false connections, VeilBrowse actively tests the proxy with real network requests before marking it connected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Proxy Type selector
                    ExposedDropdownMenuBox(
                        expanded = proxyTypeExpanded,
                        onExpandedChange = { proxyTypeExpanded = !proxyTypeExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (proxyType.equals("SOCKS", ignoreCase = true)) "SOCKS5 Proxy" else "HTTP Proxy",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Proxy Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = proxyTypeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("proxy_type_selector")
                        )
                        ExposedDropdownMenu(
                            expanded = proxyTypeExpanded,
                            onDismissRequest = { proxyTypeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("HTTP Proxy") },
                                onClick = {
                                    proxyType = "HTTP"
                                    proxyTypeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("SOCKS5 Proxy (e.g. Tor 127.0.0.1:9050)") },
                                onClick = {
                                    proxyType = "SOCKS"
                                    proxyTypeExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Host and Port
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = proxyHost,
                            onValueChange = { proxyHost = it },
                            label = { Text("Host / IP") },
                            placeholder = { Text("127.0.0.1") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("proxy_host_input")
                        )

                        OutlinedTextField(
                            value = proxyPort,
                            onValueChange = { proxyPort = it },
                            label = { Text("Port") },
                            placeholder = { Text("8080") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("proxy_port_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional Authentication
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = proxyUsername,
                            onValueChange = { proxyUsername = it },
                            label = { Text("Username (Optional)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("proxy_username_input")
                        )

                        OutlinedTextField(
                            value = proxyPassword,
                            onValueChange = { proxyPassword = it },
                            label = { Text("Password (Optional)") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("proxy_password_input")
                        )
                    }

                    // Verification Info Card (when tested or failed or connected)
                    if (proxyStatus.state != ProxyConnectionState.NOT_CONFIGURED) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("proxy_verification_details_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VERIFICATION DETAILS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CyanPrimary
                                        )
                                    )
                                    if (proxyStatus.testTimestamp > 0) {
                                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            .format(Date(proxyStatus.testTimestamp))
                                        Text(
                                            text = "Tested at $timeStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                ConnectionMetricRow(
                                    "WebView Override",
                                    if (proxyStatus.isAppliedToWebView) "Applied (Active)" else "Not Applied"
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ConnectionMetricRow(
                                    "Observed Public IP",
                                    proxyStatus.observedPublicIp ?: "None (Unverified)"
                                )
                                if (!proxyStatus.observedLocation.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ConnectionMetricRow("Observed Location", proxyStatus.observedLocation)
                                }
                                if (proxyStatus.responseTimeMs != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ConnectionMetricRow("Response Time", "${proxyStatus.responseTimeMs} ms")
                                }
                                ConnectionMetricRow(
                                    "Authentication",
                                    if (proxyStatus.hasUsername) "Configured (Protected)" else "None"
                                )

                                if (proxyStatus.state == ProxyConnectionState.FAILED && !proxyStatus.failureReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DangerRed.copy(alpha = 0.15f))
                                            .padding(10.dp)
                                            .testTag("proxy_failure_reason_box")
                                    ) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = DangerRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Connection Error",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = DangerRed
                                                    )
                                                )
                                                Text(
                                                    text = proxyStatus.failureReason,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = DangerRed)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (proxyStatusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = proxyStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons: Test & Connect (Primary) / Save Config / Disconnect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val portInt = proxyPort.toIntOrNull() ?: 8080
                                onTestAndConnectProxy(proxyHost, portInt, proxyType, proxyUsername, proxyPassword)
                            },
                            enabled = proxyStatus.state != ProxyConnectionState.TESTING && proxyHost.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("test_connect_proxy_button")
                        ) {
                            if (proxyStatus.state == ProxyConnectionState.TESTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verifying…")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test & Connect")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val portInt = proxyPort.toIntOrNull() ?: 8080
                                onSaveProxy(true, proxyHost, portInt, proxyType, proxyUsername, proxyPassword)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_proxy_button")
                        ) {
                            Text("Save Config")
                        }
                    }

                    if (proxyStatus.state == ProxyConnectionState.CONNECTED || proxyStatus.state == ProxyConnectionState.CONFIGURED_NOT_VERIFIED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onDisconnectProxy,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("disconnect_proxy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = DangerRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect Proxy", color = DangerRed)
                        }
                    }
                }
            }
        }

        // Section 3: Android System VPN vs. WebView Proxy Guide
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("system_vpn_guide_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VPN vs. Proxy Distinction",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• System VPN: Operates at the Android OS layer. It tunnels and encrypts all network activity for all applications on this device when connected.\n• WebView Proxy: In-app routing via Android WebView ProxyController. It applies exclusively to VeilBrowse web requests and does NOT provide device-wide VPN protection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConnectionMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
