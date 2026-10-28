package com.example.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** A consistent Lucide line icon family. Source and ISC licence are in assets/licenses/lucide. */
object AppIcons {
    private fun icon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { path ->
            builder.addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = null,
                stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
        return builder.build()
    }
    val Home: ImageVector by lazy { icon("Home", "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z") }
    val Public: ImageVector by lazy { icon("Public", "M21.54 15H17a2 2 0 0 0-2 2v4.54", "M7 3.34V5a3 3 0 0 0 3 3a2 2 0 0 1 2 2c0 1.1.9 2 2 2a2 2 0 0 0 2-2c0-1.1.9-2 2-2h3.17", "M11 21.95V18a2 2 0 0 0-2-2a2 2 0 0 1-2-2v-1a2 2 0 0 0-2-2H2.05", "M 2.0 12.0 A 10.0 10.0 0 1 0 22.0 12.0 A 10.0 10.0 0 1 0 2.0 12.0") }
    val Settings: ImageVector by lazy { icon("Settings", "M14 17H5", "M19 7h-9", "M 14.0 17.0 A 3.0 3.0 0 1 0 20.0 17.0 A 3.0 3.0 0 1 0 14.0 17.0", "M 4.0 7.0 A 3.0 3.0 0 1 0 10.0 7.0 A 3.0 3.0 0 1 0 4.0 7.0") }
    val Person: ImageVector by lazy { icon("Person", "M 7.0 8.0 A 5.0 5.0 0 1 0 17.0 8.0 A 5.0 5.0 0 1 0 7.0 8.0", "M20 21a8 8 0 0 0-16 0") }
    val Add: ImageVector by lazy { icon("Add", "M 2.0 12.0 A 10.0 10.0 0 1 0 22.0 12.0 A 10.0 10.0 0 1 0 2.0 12.0", "M8 12h8", "M12 8v8") }
    val ArrowForward: ImageVector by lazy { icon("ArrowForward", "M5 12h14", "m12 5 7 7-7 7") }
    val ArrowBack: ImageVector by lazy { icon("ArrowBack", "m12 19-7-7 7-7", "M19 12H5") }
    val Check: ImageVector by lazy { icon("Check", "M20 6 9 17l-5-5") }
    val ChevronRight: ImageVector by lazy { icon("ChevronRight", "m9 18 6-6-6-6") }
    val Cloud: ImageVector by lazy { icon("Cloud", "M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z") }
    val ContentPaste: ImageVector by lazy { icon("ContentPaste", "M11 14h10", "M16 4h2a2 2 0 0 1 2 2v1.344", "m17 18 4-4-4-4", "M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 1.793-1.113", "M 9.0 2.0 H 15.0 Q 16.0 2.0 16.0 3.0 V 5.0 Q 16.0 6.0 15.0 6.0 H 9.0 Q 8.0 6.0 8.0 5.0 V 3.0 Q 8.0 2.0 9.0 2.0 Z") }
    val DataUsage: ImageVector by lazy { icon("DataUsage", "M12 16v5", "M16 14.639V21", "M20 10.656V21", "m22 3-8.646 8.646a.5.5 0 0 1-.708 0L9.354 8.354a.5.5 0 0 0-.707 0L2 15", "M4 18.463V21", "M8 14.656V21") }
    val Delete: ImageVector by lazy { icon("Delete", "M10 11v6", "M14 11v6", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M3 6h18", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2") }
    val Info: ImageVector by lazy { icon("Info", "M 2.0 12.0 A 10.0 10.0 0 1 0 22.0 12.0 A 10.0 10.0 0 1 0 2.0 12.0", "M12 16v-4", "M12 8h.01") }
    val Lock: ImageVector by lazy { icon("Lock", "M 11.0 16.0 A 1.0 1.0 0 1 0 13.0 16.0 A 1.0 1.0 0 1 0 11.0 16.0", "M 5.0 10.0 H 19.0 Q 21.0 10.0 21.0 12.0 V 20.0 Q 21.0 22.0 19.0 22.0 H 5.0 Q 3.0 22.0 3.0 20.0 V 12.0 Q 3.0 10.0 5.0 10.0 Z", "M7 10V7a5 5 0 0 1 10 0v3") }
    val Logout: ImageVector by lazy { icon("Logout", "m16 17 5-5-5-5", "M21 12H9", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4") }
    val NetworkPing: ImageVector by lazy { icon("NetworkPing", "M16.247 7.761a6 6 0 0 1 0 8.478", "M19.075 4.933a10 10 0 0 1 0 14.134", "M4.925 19.067a10 10 0 0 1 0-14.134", "M7.753 16.239a6 6 0 0 1 0-8.478", "M 10.0 12.0 A 2.0 2.0 0 1 0 14.0 12.0 A 2.0 2.0 0 1 0 10.0 12.0") }
    val Notifications: ImageVector by lazy { icon("Notifications", "M10.268 21a2 2 0 0 0 3.464 0", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326") }
    val PowerSettingsNew: ImageVector by lazy { icon("PowerSettingsNew", "M12 2v10", "M18.4 6.6a9 9 0 1 1-12.77.04") }
    val Refresh: ImageVector by lazy { icon("Refresh", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5") }
    val SdStorage: ImageVector by lazy { icon("SdStorage", "M 3.0 5.0 A 9.0 3.0 0 1 0 21.0 5.0 A 9.0 3.0 0 1 0 3.0 5.0", "M3 5V19A9 3 0 0 0 21 19V5", "M3 12A9 3 0 0 0 21 12") }
    val Share: ImageVector by lazy { icon("Share", "M 15.0 5.0 A 3.0 3.0 0 1 0 21.0 5.0 A 3.0 3.0 0 1 0 15.0 5.0", "M 3.0 12.0 A 3.0 3.0 0 1 0 9.0 12.0 A 3.0 3.0 0 1 0 3.0 12.0", "M 15.0 19.0 A 3.0 3.0 0 1 0 21.0 19.0 A 3.0 3.0 0 1 0 15.0 19.0", "M 8.59 13.51 L 15.42 17.49", "M 15.41 6.51 L 8.59 10.49") }
    val Shield: ImageVector by lazy { icon("Shield", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z") }
    val Sort: ImageVector by lazy { icon("Sort", "M2 5h20", "M6 12h12", "M9 19h6") }
    val VerifiedUser: ImageVector by lazy { icon("VerifiedUser", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z", "m9 12 2 2 4-4") }
    val ServerPin: ImageVector by lazy { icon("ServerPin", "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0", "M 9.0 10.0 A 3.0 3.0 0 1 0 15.0 10.0 A 3.0 3.0 0 1 0 9.0 10.0") }
}
