package com.example.veilbrowse.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningYellow
import com.example.veilbrowse.data.model.BlockedTrackerEntity
import com.example.veilbrowse.data.model.InspectedResource
import com.example.veilbrowse.data.model.ResourceDisposition
import com.example.veilbrowse.data.model.TrackerProtectionLevel

@Composable
fun ClearDataDialog(
    onDismiss: () -> Unit,
    onConfirmClear: (cookies: Boolean, cache: Boolean, storage: Boolean, trackerLogs: Boolean) -> Unit
) {
    var clearCookies by remember { mutableStateOf(true) }
    var clearCache by remember { mutableStateOf(true) }
    var clearStorage by remember { mutableStateOf(true) }
    var clearTrackers by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Clear Browsing Data",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Select session data to purge from device storage:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().testTag("checkbox_cookies")
                ) {
                    Checkbox(
                        checked = clearCookies,
                        onCheckedChange = { clearCookies = it },
                        colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                    )
                    Text("Cookies & Site Permissions")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().testTag("checkbox_cache")
                ) {
                    Checkbox(
                        checked = clearCache,
                        onCheckedChange = { clearCache = it },
                        colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                    )
                    Text("Cached web images and files")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().testTag("checkbox_storage")
                ) {
                    Checkbox(
                        checked = clearStorage,
                        onCheckedChange = { clearStorage = it },
                        colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                    )
                    Text("Web Storage & DOM data")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().testTag("checkbox_trackers")
                ) {
                    Checkbox(
                        checked = clearTrackers,
                        onCheckedChange = { clearTrackers = it },
                        colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                    )
                    Text("Blocked tracker session history")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmClear(clearCookies, clearCache, clearStorage, clearTrackers)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_clear_data_button")
            ) {
                Text("Clear Now")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_clear_data_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedTrackersSheet(
    trackers: List<BlockedTrackerEntity>,
    inspectedResources: List<InspectedResource> = emptyList(),
    protectionLevel: TrackerProtectionLevel = TrackerProtectionLevel.BALANCED,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Convert legacy tracker entities if inspected list is empty
    val effectiveResources = remember(trackers, inspectedResources) {
        if (inspectedResources.isNotEmpty()) {
            inspectedResources
        } else {
            trackers.map {
                InspectedResource(
                    id = it.id,
                    domain = it.domain,
                    category = it.category,
                    disposition = ResourceDisposition.TRACKER_BLOCKED,
                    timestamp = it.timestamp,
                    url = it.blockedOnUrl
                )
            }
        }
    }

    val blockedCount = effectiveResources.count { it.disposition == ResourceDisposition.TRACKER_BLOCKED }
    val adsAllowedCount = effectiveResources.count { it.disposition == ResourceDisposition.AD_ALLOWED }
    val trackerAllowedCount = effectiveResources.count { it.disposition == ResourceDisposition.TRACKER_ALLOWED }
    val siteAllowedCount = effectiveResources.count { it.disposition == ResourceDisposition.WEBSITE_ALLOWED }

    val filteredList = remember(effectiveResources, selectedFilter) {
        when (selectedFilter) {
            "BLOCKED" -> effectiveResources.filter { it.disposition == ResourceDisposition.TRACKER_BLOCKED }
            "ADS_ALLOWED" -> effectiveResources.filter { it.disposition == ResourceDisposition.AD_ALLOWED }
            "TRACKERS_ALLOWED" -> effectiveResources.filter { it.disposition == ResourceDisposition.TRACKER_ALLOWED }
            "SITE_ALLOWED" -> effectiveResources.filter { it.disposition == ResourceDisposition.WEBSITE_ALLOWED }
            else -> effectiveResources
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("blocked_trackers_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Column {
                        Text(
                            text = "Tracker & Resource Inspector",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Level: ${protectionLevel.displayName} • Blocked: $blockedCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips horizontally scrollable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${effectiveResources.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanPrimary
                    )
                )
                FilterChip(
                    selected = selectedFilter == "BLOCKED",
                    onClick = { selectedFilter = "BLOCKED" },
                    label = { Text("Blocked ($blockedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DangerRed.copy(alpha = 0.2f),
                        selectedLabelColor = DangerRed
                    )
                )
                FilterChip(
                    selected = selectedFilter == "ADS_ALLOWED",
                    onClick = { selectedFilter = "ADS_ALLOWED" },
                    label = { Text("Ads Allowed ($adsAllowedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanPrimary
                    )
                )
                FilterChip(
                    selected = selectedFilter == "TRACKERS_ALLOWED",
                    onClick = { selectedFilter = "TRACKERS_ALLOWED" },
                    label = { Text("Allowed Trackers ($trackerAllowedCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "SITE_ALLOWED",
                    onClick = { selectedFilter = "SITE_ALLOWED" },
                    label = { Text("Site Resources ($siteAllowedCount)") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Text(
                    text = "No inspected resources match the selected filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(360.dp)
                ) {
                    items(filteredList) { item ->
                        val badgeColor = when (item.disposition) {
                            ResourceDisposition.TRACKER_BLOCKED -> DangerRed
                            ResourceDisposition.AD_ALLOWED -> CyanPrimary
                            ResourceDisposition.TRACKER_ALLOWED -> WarningYellow
                            ResourceDisposition.WEBSITE_ALLOWED -> SafeGreen
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.domain.ifBlank { "Host" },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.disposition.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = badgeColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                if (item.url.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.url,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.outline
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
