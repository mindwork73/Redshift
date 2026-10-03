package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainTab
import com.example.ui.t
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSpace
import com.example.ui.theme.VpnColors

/**
 * Bar geometry, shared by the container and the items so they cannot drift apart.
 *
 * One tab is a stack of: icon frame (52dp, the active tab fills it with the green disc) +
 * 2dp gap + label line (14dp) = 68dp, and [BarHeight] leaves 6dp above and below it. Two
 * rules keep the four labels on one baseline and inside the bar: the frame is the same size
 * in every tab, and the stack is top-aligned inside [ItemHeight] instead of centred —
 * centring let the taller active stack push its label below the others, and a bar shorter
 * than the stack made Compose centre the whole stack and spill the labels past the edge.
 */
private val BarHeight = 80.dp
private val ItemHeight = 68.dp
private val ActiveCircle = CircleShape
private val ActiveCircleSize = 46.dp
private val ActiveHaloSize = 52.dp
private val IconFrameSize = 52.dp
private val IconSize = 24.dp
private val BarSideMargin = 16.dp
private val BarBottomMargin = 10.dp

/**
 * Label style of a tab: one step below [com.example.ui.theme.RedType.CaptionMedium] so a
 * long translation ("Настройки", "Impostazioni") still fits the 48dp active pill.
 */
private val TabLabelStyle = TextStyle(
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.1.sp
)

/**
 * Bottom navigation (REDESIGN.md §6 / §7.4), restyled as a floating "pill" bar: it is
 * detached from the screen edges, floats over the backdrop on a soft shadow and carries a
 * glass edge + top glint, so it reads as a glass object rather than a docked strip.
 *
 * Every tab keeps **icon + label** (REDESIGN.md §6 requires both, unlike the mockup, where
 * only the active tab is labelled), and the active tab gets a green [VpnColors.Success]
 * disc — the same green the UI already uses for "connected" — with a soft halo.
 *
 * The bar is expected to be the [androidx.compose.material3.Scaffold] `bottomBar`; the
 * shadows are not clipped by the compact height because the paddings live inside the bar.
 */
@Composable
fun NavBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = BarSideMargin,
                end = BarSideMargin,
                bottom = BarBottomMargin
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(RedRadius.XLarge),
                    ambientColor = VpnColors.NavBarFloatingShadow,
                    spotColor = VpnColors.NavBarFloatingShadow
                )
                .clip(RoundedCornerShape(RedRadius.XLarge))
                .background(VpnColors.NavBarFloatingFill)
                .border(
                    width = 1.dp,
                    color = VpnColors.NavBarFloatingBorder,
                    shape = RoundedCornerShape(RedRadius.XLarge)
                )
        ) {
            // Top-edge glint: the same liquid-glass highlight the cards carry, kept
            // inside the pill so it never pokes over the rounded corners.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(VpnColors.GlassGlint, Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .padding(horizontal = RedSpace.Xxs),
                verticalAlignment = Alignment.CenterVertically
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

    Box(
        modifier = modifier
            .height(ItemHeight)
            .padding(horizontal = RedSpace.Xxs)
            // Clipped so the tap ripple stays inside the tab instead of squaring off the
            // rounded top corners of the bar.
            .clip(RoundedCornerShape(RedRadius.Medium))
            .clickable { onSelect(tab) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Top-aligned inside a fixed-height item: the frame is identical in every tab,
            // so all four labels land on the same baseline.
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .height(ItemHeight)
                .padding(horizontal = RedSpace.Xxs)
        ) {
            // One fixed frame for all four tabs keeps every label on the same baseline.
            Box(
                modifier = Modifier.size(IconFrameSize),
                contentAlignment = Alignment.Center
            ) {
                // Only the active tab gets the disc. Unselected tabs are a bare icon: an
                // outline on all four read as four rings instead of one highlighted point.
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(ActiveHaloSize)
                            .clip(ActiveCircle)
                            .background(VpnColors.NavBarItemActiveHalo)
                    )
                    Box(
                        modifier = Modifier
                            .size(ActiveCircleSize)
                            .clip(ActiveCircle)
                            .background(VpnColors.NavBarItemActiveFill)
                            .border(
                                width = 1.dp,
                                // Lighter edge on the top-left: at this size that is what
                                // reads as "glossy" instead of a flat tinted circle.
                                brush = Brush.linearGradient(
                                    listOf(VpnColors.NavBarItemActiveEdge, VpnColors.NavBarItemActiveFill)
                                ),
                                shape = ActiveCircle
                            )
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) VpnColors.NavBarItemActive else VpnColors.TextTertiary,
                    modifier = Modifier.size(IconSize)
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = t(tab.labelKey),
                style = TabLabelStyle,
                color = if (selected) VpnColors.TextPrimary else VpnColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
