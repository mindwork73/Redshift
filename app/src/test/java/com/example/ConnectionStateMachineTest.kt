package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.ui.ConnectionState
import com.example.ui.RedShiftState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the connect/disconnect state machine - the code paths behind the
 * reported bug: "turned the VPN off, then picked the Telemost (olcRTC) server and the app
 * hung / would not connect again".
 *
 * What they pin down:
 *  * [RedShiftState.restartConnection] must never be a no-op. Before the fix, tapping a
 *    server row did `selectServer` + `toggleVpn` only while CONNECTED was false, so while
 *    CONNECTING it *disconnected* and while CONNECTED it did nothing at all.
 *  * A tap during an in-flight connect must tear the attempt down (state DISCONNECTED)
 *    instead of leaving the UI stuck in CONNECTING.
 *
 * Deliberately shallow: no service, no native sing-box, no network. The service side (olcRTC
 * readiness window, runtime teardown, port 8788) cannot be exercised without a device and is
 * verified on hardware instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionStateMachineTest {

    // RedShiftState launches on Dispatchers.Main; Robolectric has no Android main looper
    // dispatcher wired up by default, hence the test dispatcher.
    @Before
    fun setUp() = Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun freshState(): RedShiftState {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // RedShiftState.init schedules the expiry reminder worker; WorkManager has no
        // default initializer under Robolectric, so seed the test instance first.
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        RedShiftState.init(context)
        return RedShiftState
    }

    @Test
    fun restartConnection_whileConnecting_tearsTheAttemptDown() {
        val s = freshState()
        s.connectionState = ConnectionState.CONNECTING

        s.restartConnection()

        assertEquals(
            "a server tap during an in-flight connect must tear the old attempt down",
            ConnectionState.DISCONNECTED,
            s.connectionState
        )
    }

    @Test
    fun toggleVpn_whileConnecting_disconnects() {
        val s = freshState()
        s.connectionState = ConnectionState.CONNECTING

        s.toggleVpn()

        assertEquals(ConnectionState.DISCONNECTED, s.connectionState)
    }

    @Test
    fun selectServer_persistsTheChoiceBeforeTheConnectStarts() {
        val s = freshState()

        s.selectServer("olcrtc_telemost")

        assertEquals(
            "the server must be selected before the tunnel is (re)started",
            "olcrtc_telemost",
            s.selectedServerId
        )
    }
}
