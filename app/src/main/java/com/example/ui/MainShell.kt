package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.ui.components.NavBar
import com.example.ui.home.HomeScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.servers.ServersScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.VpnColors

/** The four bottom-navigation tabs (REDESIGN.md §6). */
enum class MainTab(val labelKey: String) {
    Home("nav_home"),
    Servers("nav_servers"),
    Settings("nav_settings"),
    Profile("nav_profile")
}

/**
 * Root composable of the app — the signature is frozen, `MainActivity` calls it (REDESIGN.md §2).
 *
 * Shows onboarding while `RedShiftState.isOnboarded` is false, otherwise a [Scaffold] with the
 * four-tab [NavBar] and a 250ms cross-fade between tabs. There are no nested navigation stacks:
 * every tab owns its whole screen.
 */
@Composable
fun MainAppContainer() {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    val isOnboarded = RedShiftState.isOnboarded

    CompositionLocalProvider(
        LocalLayoutDirection provides LocalizationState.getLayoutDirection()
    ) {
        if (!isOnboarded) {
            OnboardingScreen(onFinished = { RedShiftState.isOnboarded = true })
        } else {
            // System back returns to Home from any other tab; sheets close themselves.
            BackHandler(enabled = currentTab != MainTab.Home) {
                currentTab = MainTab.Home
            }

            Scaffold(
                bottomBar = {
                    NavBar(
                        selected = currentTab,
                        onSelect = { currentTab = it }
                    )
                },
                containerColor = VpnColors.Background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 250)) togetherWith
                                fadeOut(tween(durationMillis = 250))
                        },
                        label = "tab_switch"
                    ) { tab ->
                        when (tab) {
                            MainTab.Home -> HomeScreen(
                                onOpenServers = { currentTab = MainTab.Servers }
                            )

                            MainTab.Servers -> ServersScreen()
                            MainTab.Settings -> SettingsScreen()
                            MainTab.Profile -> ProfileScreen()
                        }
                    }
                }
            }
        }
    }
}
