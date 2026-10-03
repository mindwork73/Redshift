package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// sdk 34 rather than 36: Robolectric refuses to sandbox SDK 36 unless the JVM is Java 21 and
// this machine builds on JDK 17, so the pinned 36 made the whole class fail instead of test
// anything. The assertion is also the real app name, not the template placeholder.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RedShift", appName)
  }
}
