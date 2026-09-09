package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSize
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/** Indent of the divider when a row has a leading glass icon (40dp icon + 12dp gap). */
private val DividerIndentWithIcon: Dp = 52.dp

/** Section heading (REDESIGN.md §7.6): small caps in the tertiary colour. */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = RedType.CaptionMedium,
        color = VpnColors.TextTertiary,
        modifier = modifier.padding(start = RedSpace.Xxs)
    )
}

/** Hairline separator between list rows (§7.5). */
@Composable
fun RowDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = RedSpace.M
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(0.5.dp)
            .background(VpnColors.Divider)
    )
}

/**
 * List row (REDESIGN.md §7.5): 64–72dp tall, optional glass icon on the left,
 * title + subtitle in the middle, value and chevron on the right, divider underneath.
 */
@Composable
fun ListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = VpnColors.TextPrimary,
    trailingText: String? = null,
    trailingColor: Color = VpnColors.TextSecondary,
    showChevron: Boolean = false,
    showDivider: Boolean = false,
    minHeight: Dp = RedSize.ListRow,
    titleColor: Color = VpnColors.TextPrimary,
    enabled: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickable = if (onClick != null && enabled) {
        Modifier.clickable { onClick.invoke() }
    } else {
        Modifier
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .then(clickable)
                .padding(vertical = RedSpace.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                GlassIconBubble(
                    icon = leadingIcon,
                    tint = leadingTint,
                    contentDescription = null
                )
                Spacer(Modifier.width(RedSpace.S))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = RedType.Body,
                    color = if (enabled) titleColor else VpnColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = RedType.Caption,
                        color = VpnColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(RedSpace.S))

            if (trailing != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpace.Xs),
                    content = trailing
                )
            } else if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = RedType.BodyMedium,
                    color = trailingColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showChevron) {
                Spacer(Modifier.width(RedSpace.Xs))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = VpnColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDivider) {
            RowDivider(startIndent = if (leadingIcon != null) DividerIndentWithIcon else RedSpace.M)
        }
    }
}

/**
 * iOS-like switch row (REDESIGN.md §7.7) — the platform Material switch with an accent track
 * and a white thumb. The system look is kept on purpose.
 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    showDivider: Boolean = false,
    enabled: Boolean = true,
    checkedTrackColor: Color = VpnColors.Accent
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RedSize.ListRow)
                .padding(vertical = RedSpace.Xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                GlassIconBubble(icon = leadingIcon, contentDescription = null)
                Spacer(Modifier.width(RedSpace.S))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = RedType.Body,
                    color = if (enabled) VpnColors.TextPrimary else VpnColors.TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = RedType.Caption,
                        color = VpnColors.TextTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(RedSpace.S))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = checkedTrackColor,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = VpnColors.GlassFillStrong,
                    uncheckedBorderColor = VpnColors.GlassBorder
                )
            )
        }

        if (showDivider) {
            RowDivider(startIndent = if (leadingIcon != null) DividerIndentWithIcon else RedSpace.M)
        }
    }
}

/** Segmented control for the routing mode picker (§8.3): glass track, accent-filled segment. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RedRadius.Small))
            .background(VpnColors.GlassFill)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(RedRadius.Small))
                    .background(if (isSelected) VpnColors.Accent else Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = RedType.CaptionMedium,
                    color = if (isSelected) Color.White else VpnColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
