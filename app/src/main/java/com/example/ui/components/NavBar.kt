package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.MainTab
import com.example.ui.t
import com.example.ui.theme.RedSize
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/**
 * Bottom navigation (REDESIGN.md §7.4 / §6): 80dp tall plus navigation-bar insets,
 * translucent [VpnColors.NavBarFill], hairline divider on top, four tabs.
 */
@Composable
fun NavBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VpnColors.NavBarFill)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(VpnColors.Divider)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(RedSize.NavBar),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MainTab.entries.forEach { tab ->
                NavItem(
                    tab = tab,
                    selected = tab == selected,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: MainTab,
    selected: Boolean,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (tab) {
        MainTab.Home -> Icons.Filled.Shield
        MainTab.Servers -> Icons.Filled.Cloud
        MainTab.Settings -> Icons.Filled.Settings
        MainTab.Profile -> Icons.Filled.Person
    }
    val tint: Color = if (selected) VpnColors.Accent else VpnColors.TextTertiary

    Column(
        modifier = modifier
            .clickable { onSelect(tab) }
            .height(RedSize.NavBar),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(RedSpace.Xxs))
        Text(
            text = t(tab.labelKey),
            style = RedType.CaptionMedium,
            color = if (selected) VpnColors.TextPrimary else VpnColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
