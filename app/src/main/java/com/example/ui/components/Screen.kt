package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/**
 * Vertical screen container (REDESIGN.md §7.2): gradient background, inset-aware,
 * 24dp horizontal / 20dp vertical content padding, scrolling when the content is tall.
 */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    statusBarPadding: Boolean = true,
    navigationBarPadding: Boolean = false,
    horizontalPadding: Dp = RedSpace.Xl,
    verticalPadding: Dp = RedSpace.L,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        VpnColors.BackgroundGradientTop,
                        VpnColors.BackgroundGradientBottom
                    )
                )
            )
    ) {
        val insets = Modifier
            .then(if (statusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .then(if (navigationBarPadding) Modifier.navigationBarsPadding() else Modifier)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(insets)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            content = content
        )
    }
}

/** Screen title row (REDESIGN.md §7.3): [RedType.Display] title with optional trailing actions. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = RedType.Display,
            color = VpnColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(RedSpace.Xs),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

