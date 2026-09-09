@file:JvmName("OnboardingScreenPremiumKt")

package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.VpnColors
import com.example.ui.theme.VpnTypography

@Composable
fun OnboardingScreenPremium(onFinished: () -> Unit) {
    var panelIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ─── Header (Логотип) ───
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(VpnColors.premiumGradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "RedShift",
                color = VpnColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // ─── Content (Анимированная смена слайдов) ───
        AnimatedContent(
            targetState = panelIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "onboarding_animation",
            modifier = Modifier.fillMaxWidth()
        ) { targetIndex ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Визуальная часть (Иконка/Картинка)
                if (targetIndex == 0) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(VpnColors.surfaceGlass)
                            .border(2.dp, VpnColors.borderLight, CircleShape)
                            .padding(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_cyber_hero),
                            contentDescription = "Awakening Eye Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    val icon = if (targetIndex == 1) Icons.AutoMirrored.Filled.AltRoute else Icons.Default.Security
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(VpnColors.surfaceGlass)
                            .border(2.dp, VpnColors.borderLight, CircleShape)
                            .shadow(24.dp, CircleShape, ambientColor = VpnColors.premiumGradient.first(), spotColor = VpnColors.premiumGradient.last()),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(VpnColors.surfaceInner),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = VpnColors.TextPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }

                // Текстовая часть
                val title = when (targetIndex) {
                    0 -> Trans.get("onboard_1_title")
                    1 -> Trans.get("onboard_2_title")
                    else -> Trans.get("onboard_3_title")
                }

                val desc = when (targetIndex) {
                    0 -> Trans.get("onboard_1_desc")
                    1 -> Trans.get("onboard_2_desc")
                    else -> Trans.get("onboard_3_desc")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        style = VpnTypography.statusMain.copy(fontSize = 28.sp, color = VpnColors.TextPrimary),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = desc,
                        style = VpnTypography.cardSubtitle.copy(fontSize = 15.sp, color = VpnColors.TextSecondary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // ─── Footer (Индикаторы и Кнопка) ───
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            // Пагинация (Точки)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isSelected = index == panelIndex
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isSelected) 24.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Brush.horizontalGradient(VpnColors.premiumGradient)
                                else Brush.horizontalGradient(listOf(VpnColors.surfaceInner, VpnColors.surfaceInner))
                            )
                    )
                }
            }

            // Кнопка
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(Brush.horizontalGradient(VpnColors.premiumGradient))
                    .clickable {
                        if (panelIndex < 2) {
                            panelIndex++
                        } else {
                            onFinished()
                        }
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (panelIndex == 2) Trans.get("get_started") else "Next",
                    style = VpnTypography.buttonText.copy(fontSize = 16.sp, color = Color.White)
                )
            }
        }
    }
}