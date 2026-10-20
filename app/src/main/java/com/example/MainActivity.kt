package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.service.RedShiftTileService
import com.example.service.RedShiftVpnService
import com.example.ui.MainAppContainer
import com.example.ui.RedShiftState
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        RedShiftState.init(this)
        RedShiftState.currentActivity = this

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current

                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            if (intent.action == "com.example.VPN_DISCONNECTED") {
                                RedShiftState.connectionState = com.example.ui.ConnectionState.DISCONNECTED
                            }
                        }
                    }
                    context.registerReceiver(receiver, IntentFilter("com.example.VPN_DISCONNECTED"), Context.RECEIVER_NOT_EXPORTED)

                    onDispose {
                        context.unregisterReceiver(receiver)
                    }
                }

                MainAppContainer()
            }
        }

        handleTileToggleIfNeeded(intent, savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RedShiftState.VPN_CONSENT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d("RedShiftVPN", "consent granted, resuming connect")
                RedShiftState.toggleVpn()
            } else {
                RedShiftState.connectionState = com.example.ui.ConnectionState.DISCONNECTED
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == RedShiftVpnService.ACTION_DISCONNECT) {
            RedShiftState.connectionState = com.example.ui.ConnectionState.DISCONNECTED
        }
        handleTileToggleIfNeeded(intent, null)
    }

    /**
     * A Quick Settings tile tap arrives as this activity's intent. When no tunnel
     * is live we kick off the normal connect sequence right away.
     */
    private fun handleTileToggleIfNeeded(intent: Intent?, savedInstanceState: Bundle?) {
        if (intent == null) return
        if (intent.action != RedShiftTileService.ACTION_TOGGLE_VPN) return
        if (!intent.getBooleanExtra(RedShiftTileService.EXTRA_TOGGLE_FROM_TILE, false)) return
        if (savedInstanceState != null) return
        RedShiftState.toggleVpn()
    }
}
