package com.example.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.ui.ConnectionState
import com.example.ui.RedShiftState

/**
 * Quick Settings tile that toggles the VPN from the notification shade.
 * The tile reads [RedShiftVpnService.tunReady] to show its state; tapping it
 * disconnects an active tunnel directly or asks the app to start one.
 */
class RedShiftTileService : TileService() {

    companion object {
        const val EXTRA_TOGGLE_FROM_TILE = "extra_toggle_from_tile"
        const val ACTION_TOGGLE_VPN = "com.example.action.TOGGLE_VPN"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (RedShiftVpnService.tunReady) {
            // Live tunnel -> disconnect straight from the tile.
            startService(
                Intent(this, RedShiftVpnService::class.java)
                    .setAction(RedShiftVpnService.ACTION_DISCONNECT)
            )
            updateTile()
            return
        }
        // Not connected -> open the app and start the VPN there (VpnService
        // consent and the full sing-box setup run inside the app).
        val launch = Intent(this, MainActivity::class.java).apply {
            action = ACTION_TOGGLE_VPN
            putExtra(EXTRA_TOGGLE_FROM_TILE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(launch)
    }

    /** Keeps the tile visuals in sync with the real tunnel state. */
    fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (RedShiftVpnService.tunReady ||
            RedShiftState.connectionState == ConnectionState.CONNECTED
        ) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.label = if (tile.state == Tile.STATE_ACTIVE) "RedShift: ON" else "RedShift"
        tile.updateTile()
    }
}