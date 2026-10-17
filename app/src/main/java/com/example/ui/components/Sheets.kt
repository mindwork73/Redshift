@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.AppLanguage
import com.example.ui.LocalizationState
import com.example.ui.RedShiftState
import com.example.ui.t
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/**
 * Glass wrapper around [ModalBottomSheet] (REDESIGN.md §6 / §9): dark rounded sheet used for
 * subscription import, language choice and server actions.
 */
@Composable
fun GlassSheet(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = RedRadius.XLarge, topEnd = RedRadius.XLarge),
        containerColor = Color(0xFF0C0C10),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = VpnColors.GlassBorderTop) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = RedSpace.Xl)
                .padding(bottom = RedSpace.Xl)
        ) {
            Text(
                text = title,
                style = RedType.Title,
                color = VpnColors.TextPrimary
            )
            Spacer(Modifier.height(RedSpace.M))
            content()
        }
    }
}

/**
 * Subscription import sheet (REDESIGN.md §8.2 / §8.3.1): URL field → `importSubscription`,
 * live `isImporting` spinner and `importError` in the danger colour. No local stubs.
 */
@Composable
fun ImportSubscriptionSheet(
    onDismissRequest: () -> Unit,
    initialUrl: String = ""
) {
    var url by remember { mutableStateOf(initialUrl) }
    var submitted by remember { mutableStateOf(false) }

    val importing = RedShiftState.isImporting
    val importError = RedShiftState.importError
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(submitted, importing, importError) {
        if (submitted && !importing && importError == null) {
            onDismissRequest()
        }
    }

    GlassSheet(onDismissRequest = onDismissRequest, title = t("import_title")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RedSpace.S)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = t("import_hint"),
                        style = RedType.Caption,
                        color = VpnColors.TextTertiary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(RedRadius.Medium),
                isError = importError != null
            )
            GlassIconButton(
                icon = Icons.Filled.ContentPaste,
                contentDescription = t("paste"),
                onClick = {
                    val text = clipboard.getText()?.text?.trim()
                    if (!text.isNullOrBlank()) {
                        url = text
                    }
                },
                size = 52.dp
            )
        }

        if (importError != null) {
            Spacer(Modifier.height(RedSpace.S))
            Text(
                text = importError,
                style = RedType.Caption,
                color = VpnColors.Danger
            )
        }

        Spacer(Modifier.height(RedSpace.M))

        if (importing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = VpnColors.Accent,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(RedSpace.S))
                Text(
                    text = t("importing"),
                    style = RedType.Caption,
                    color = VpnColors.TextSecondary
                )
            }
            Spacer(Modifier.height(RedSpace.M))
        } else {
            GradientButton(
                text = t("import_action"),
                enabled = url.isNotBlank(),
                onClick = {
                    submitted = true
                    RedShiftState.importSubscription(url.trim())
                }
            )
        }
    }
}

/** Language picker sheet (REDESIGN.md §8.3.4): every [AppLanguage], persisted through the state. */
@Composable
fun LanguageSheet(
    onDismissRequest: () -> Unit
) {
    val current = LocalizationState.currentLanguage

    GlassSheet(onDismissRequest = onDismissRequest, title = t("language")) {
        AppLanguage.entries.forEach { language ->
            val isSelected = language == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        RedShiftState.setLanguage(language)
                        onDismissRequest()
                    }
                    .padding(vertical = RedSpace.S),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = language.nativeName,
                        style = RedType.Body,
                        color = if (isSelected) VpnColors.Accent else VpnColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = VpnColors.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
