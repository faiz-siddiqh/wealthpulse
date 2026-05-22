package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.AppThemeState
import com.example.ui.theme.MidnightBack
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WealthPulse", appName)
  }

  @Test
  fun `test dynamic theme switching mechanics`() {
    // 1. Arrange & Act: Switch to Airbnb Style mode
    AppThemeState.themeName = "Airbnb Style"
    
    // Assert that the theme color resolves to soft warm white
    assertEquals(Color(0xFFFDFDFD), MidnightBack)

    // 2. Act: Switch to Midnight Blue mode
    AppThemeState.themeName = "Midnight Blue"
    
    // Assert that the theme color resolves to deep oceanic navy
    assertEquals(Color(0xFF0D1B2A), MidnightBack)

    // 3. Act: Switch to Charcoal Dark mode
    AppThemeState.themeName = "Charcoal Dark"
    
    // Assert that the theme color resolves to carbon dark gray
    assertEquals(Color(0xFF121214), MidnightBack)

    // 4. Act: Switch back to default Cosmic Slate
    AppThemeState.themeName = "Cosmic Slate"
    
    // Assert that the theme color resolves to default soft light green background
    assertEquals(Color(0xFFFBFDF8), MidnightBack)
  }
}
