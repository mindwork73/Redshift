package com.example.ui

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import com.example.ui.components.NavBar
import com.example.ui.home.HomeScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.servers.ServersScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SplitTunnelAppsScreen
import com.example.ui.theme.VpnColors

/** The four bottom-navigation tabs (REDESIGN.md section 6). */
enum class MainTab(val labelKey: String) {
    Home("nav_home"),
    Servers("nav_servers"),
    Settings("nav_settings"),
    Profile("nav_profile")
}

/**
 * Root composable of the app - the signature is frozen, MainActivity calls it (REDESIGN.md section 2).
 *
 * Shows onboarding while the user has not completed it yet, otherwise a [Scaffold] with the
 * four-tab [NavBar] and a 250ms cross-fade between tabs. There are no nested navigation stacks:
 * every tab owns its whole screen.
 *
 * The onboarding "done" flag is persisted in the `redshift_ui_prefs` SharedPreferences
 * (ui layer only, service code is untouched), so it survives restarts.
 */
@Composable
fun MainAppContainer() {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var splitAppsOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("redshift_ui_prefs", Context.MODE_PRIVATE)
    }
    var isOnboarded by rememberSaveable {
        mutableStateOf(prefs.getBoolean("onboarded", RedShiftState.isOnboarded))
    }

    // Android 13+ needs a runtime grant before the expiry reminder can post.
    // Ask once; the worker silently skips if the user declines.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            !prefs.getBoolean("notif_prompted", false) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            prefs.edit().putBoolean("notif_prompted", true).apply()
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LocalizationState.getLayoutDirection()
    ) {
        if (!isOnboarded) {
            OnboardingScreen(onFinished = {
                prefs.edit().putBoolean("onboarded", true).apply()
                RedShiftState.isOnboarded = true
                isOnboarded = true
            })
        } else {
            // System back returns to Home from any other tab; sheets close themselves.
            BackHandler(enabled = splitAppsOpen || currentTab != MainTab.Home) {
                if (splitAppsOpen) {
                    splitAppsOpen = false
                } else {
                    currentTab = MainTab.Home
                }
            }

            Scaffold(
                bottomBar = {
                    if (!splitAppsOpen) {
                        NavBar(
                            selected = currentTab,
                            onSelect = { currentTab = it }
                        )
                    }
                },
                containerColor = VpnColors.Background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { paddingValues ->
                if (splitAppsOpen) {
                    SplitTunnelAppsScreen(onBack = { splitAppsOpen = false })
                } else {
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
                            MainTab.Settings -> SettingsScreen(
                                onOpenSplitApps = { splitAppsOpen = true }
                            )
                            MainTab.Profile -> ProfileScreen()
                        }
                    }
                }
                }
            }
        }
    }
}