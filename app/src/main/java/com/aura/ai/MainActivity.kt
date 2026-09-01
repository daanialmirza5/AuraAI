package com.aura.ai

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.aura.ai.core.designsystem.theme.AuraAppTheme
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.navigation.AuraNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * A single-activity host, extending FragmentActivity (not plain ComponentActivity) because
 * androidx.biometric.BiometricPrompt requires one to show the system authentication sheet.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val auraBackground = AuraColors.Background.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(auraBackground),
            navigationBarStyle = SystemBarStyle.dark(auraBackground),
        )

        setContent {
            AuraAppTheme {
                AuraNavHost()
            }
        }
    }
}
