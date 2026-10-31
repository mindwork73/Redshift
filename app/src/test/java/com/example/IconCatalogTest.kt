package com.example

import com.example.ui.icons.AppIcons
import org.junit.Assert.assertEquals
import org.junit.Test

class IconCatalogTest {
    @Test fun everyIconCanBeParsedAndUsesOneViewport() {
        val icons = listOf(AppIcons.Home, AppIcons.Public, AppIcons.Settings, AppIcons.Person,
            AppIcons.Add, AppIcons.ArrowForward, AppIcons.ArrowBack, AppIcons.Check,
            AppIcons.ChevronRight, AppIcons.Cloud, AppIcons.ContentPaste, AppIcons.DataUsage,
            AppIcons.Delete, AppIcons.Info, AppIcons.Lock, AppIcons.Logout, AppIcons.NetworkPing,
            AppIcons.Notifications, AppIcons.PowerSettingsNew, AppIcons.Refresh, AppIcons.SdStorage,
            AppIcons.Share, AppIcons.Shield, AppIcons.Sort, AppIcons.VerifiedUser, AppIcons.ServerPin)
        icons.forEach { assertEquals(24f, it.viewportWidth, 0f); assertEquals(24f, it.viewportHeight, 0f) }
    }
}
