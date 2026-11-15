package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/**
 * Metric cell (REDESIGN.md §7.11): value in [RedType.BodyMedium], caption below in tertiary.
 * Used for download / upload / latency on Home.
 */
@Composable
fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueColor: Color = VpnColors.TextPrimary
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VpnColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(RedSpace.Xxs))
        }
        Text(
            text = value,
            style = RedType.BodyMedium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = RedType.Caption,
            color = VpnColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
