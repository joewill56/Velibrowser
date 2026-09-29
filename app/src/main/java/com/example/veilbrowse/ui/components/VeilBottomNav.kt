package com.example.veilbrowse.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

enum class VeilNavigationItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val testTag: String) {
    HOME("Home", Icons.Default.Home, "nav_home"),
    BROWSER("Browser", Icons.Default.Language, "nav_browser"),
    PRIVACY("Privacy", Icons.Default.Security, "nav_privacy"),
    CONNECTION("Connection", Icons.Default.VpnKey, "nav_connection"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

@Composable
fun VeilBottomNavBar(
    selectedItem: VeilNavigationItem,
    onItemSelected: (VeilNavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("veil_bottom_nav"),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        VeilNavigationItem.values().forEach { item ->
            val isSelected = selectedItem == item
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
