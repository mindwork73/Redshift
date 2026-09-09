package com.example.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassSurface
import com.example.ui.components.GradientButton
import com.example.ui.components.Screen
import com.example.ui.t
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors
import kotlinx.coroutines.launch

/** One onboarding slide (REDESIGN.md §8.0). */
private data class OnboardingSlide(
    val icon: ImageVector,
    val titleKey: String,
    val descriptionKey: String
)

private val Slides = listOf(
    OnboardingSlide(Icons.Filled.Shield, "onboarding_1_title", "onboarding_1_desc"),
    OnboardingSlide(Icons.Filled.Public, "onboarding_2_title", "onboarding_2_desc"),
    OnboardingSlide(Icons.Filled.PowerSettingsNew, "onboarding_3_title", "onboarding_3_desc")
)

/**
 * First-run onboarding (REDESIGN.md §8.0): three swipeable slides over the dark gradient,
 * dot indicator, "Next" on the first two slides and "Start" on the last one.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { Slides.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage >= Slides.size - 1

    Screen(
        scrollable = false,
        statusBarPadding = true,
        navigationBarPadding = true,
        horizontalPadding = RedSpace.Xl,
        verticalPadding = RedSpace.Xl
    ) {
        Text(
            text = t("app_title"),
            style = RedType.CaptionMedium,
            color = VpnColors.TextTertiary
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            SlideContent(Slides[page])
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slides.forEachIndexed { index, _ ->
                val isSelected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) VpnColors.Accent else VpnColors.GlassBorder
                        )
                )
            }
        }

        Spacer(Modifier.height(RedSpace.Xl))

        GradientButton(
            text = if (isLastPage) t("onboarding_start") else t("onboarding_next"),
            onClick = {
                if (isLastPage) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            }
        )
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RedSpace.Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(VpnColors.GlassFillStrong),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = slide.icon,
                contentDescription = null,
                tint = VpnColors.Accent,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(Modifier.height(RedSpace.Xxl))

        Text(
            text = t(slide.titleKey),
            style = RedType.Display,
            color = VpnColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(RedSpace.M))

        Text(
            text = t(slide.descriptionKey),
            style = RedType.Body,
            color = VpnColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(RedSpace.Xxl))

        // Keeps the copy on glass, matching the rest of the app.
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(RedSpace.M)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RedSpace.S)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(VpnColors.Accent)
                )
                Text(
                    text = t("powered_by"),
                    style = RedType.Caption,
                    color = VpnColors.TextSecondary
                )
            }
        }
    }
}
