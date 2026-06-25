package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.service.RedShiftVpnService
import com.example.ui.MainAppContainer
import com.example.ui.RedShiftState
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        RedShiftState.init(this)

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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == RedShiftVpnService.ACTION_DISCONNECT) {
            RedShiftState.connectionState = com.example.ui.ConnectionState.DISCONNECTED
        }
    }
}
