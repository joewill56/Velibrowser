package com.example.veilbrowse.ui.privacy

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningYellow
import com.example.veilbrowse.data.model.NetworkDiagnosticInfo
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.model.WebRtcStatus
import com.example.veilbrowse.ui.components.StatusBadge

@Composable
fun PrivacyDiagnosticsScreen(
    networkInfo: NetworkDiagnosticInfo,
    settings: PrivacySettingsState,
    totalTrackersBlocked: Int,
    onRunDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("privacy_diagnostics_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Privacy & Leak Test",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Real-time network and device telemetry audit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onRunDiagnostics,
                    enabled = !networkInfo.isTesting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("run_audit_button")
                ) {
                    if (networkInfo.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Audit Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 1: IP & VPN Leak Test
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("ip_leak_test_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NETWORK & IP AUDIT",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    DiagnosticRow(
                        label = "Public IP Address",
                        value = networkInfo.publicIp ?: if (networkInfo.isTesting) "Testing…" else "Unable to verify",
                        badge = if (networkInfo.publicIp != null) "Detected" else "Offline / Blocked",
                        isSafe = networkInfo.publicIp != null
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "VPN Active Status",
                        value = if (networkInfo.isVpnConnected) "VPN tunnel detected" else "VPN not connected",
                        badge = if (networkInfo.isVpnConnected) "Encrypted" else "Direct Connection",
                        isSafe = networkInfo.isVpnConnected
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "Approx. IP Location",
                        value = networkInfo.ipLocation ?: "Unable to verify",
                        badge = if (networkInfo.ipLocation != null) "GeoIP" else "Undetermined",
                        isSafe = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        label = "Network Operator / ISP",
                        value = networkInfo.ispOrOrg ?: "Unable to verify",
                        badge = "Autonomous System",
                        isSafe = true
                    )
                }
            }
        }

        // Section 2: DNS & WebRTC Leak Test
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("webrtc_dns_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DNS & WEBRTC LEAK AUDIT",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // WebRTC
                    DiagnosticRow(
                        label = "WebRTC Exposure",
                        value = when (networkInfo.webrtcStatus) {
                            WebRtcStatus.PROTECTED -> "Local private IPs sanitized by WebView sandbox"
                            WebRtcStatus.POTENTIAL_LEAK -> "Potential local IP exposure"
                            WebRtcStatus.UNABLE_TO_VERIFY -> "Unable to verify network STUN response"
                        },
                        badge = networkInfo.webrtcStatus.label,
                        isSafe = networkInfo.webrtcStatus.isSafe
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // DNS Servers
                    val dnsSummary = if (networkInfo.dnsServers.isNotEmpty()) {
                        networkInfo.dnsServers.joinToString(", ")
                    } else {
                        "System default resolver"
                    }

                    DiagnosticRow(
                        label = "DNS Resolvers",
                        value = dnsSummary,
                        badge = networkInfo.dnsSecOrDoH,
                        isSafe = true
                    )
                }
            }
        }

        // Section 3: Device Telemetry Minimization Audit
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("device_telemetry_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DEVICE TELEMETRY AUDIT",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TelemetryAuditItem(
                        identifier = "IMEI & Hardware Serial",
                        status = "Blocked / Not Accessible",
                        description = "VeilBrowse never queries or discloses hardware identifiers."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TelemetryAuditItem(
                        identifier = "Precise GPS Location",
                        status = "Strict Deny",
                        description = "Web geolocation permission prompts are automatically rejected."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TelemetryAuditItem(
                        identifier = "Device Model User-Agent",
                        status = "Minimized (${settings.privacyProfile.displayName})",
                        description = "Hardware specifics stripped to reduce browser fingerprinting."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TelemetryAuditItem(
                        identifier = "Advertising ID & App List",
                        status = "Zero Access Requested",
                        description = "No ad SDKs or package inspection installed."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TelemetryAuditItem(
                        identifier = "Remote Browsing History Logs",
                        status = "Zero Remote Storage",
                        description = "All tabs, cache, and history remain strictly on your local device."
                    )
                }
            }
        }

        // Section 4: Technical Limitation Realism Note
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("privacy_test_disclaimer")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = WarningYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Technical Limitation Notice",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WarningYellow
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Do not assume any browser can make your phone 100% anonymous or undetectable. Websites may identify you via authenticated logins, browser canvas rendering quirks, TLS handshakes, or cookies. VeilBrowse implements defensible, honest privacy safeguards without misleading claims.",
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
private fun DiagnosticRow(
    label: String,
    value: String,
    badge: String,
    isSafe: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            StatusBadge(text = badge, isSafe = isSafe)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TelemetryAuditItem(
    identifier: String,
    status: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SafeGreen,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = identifier,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SafeGreen
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
