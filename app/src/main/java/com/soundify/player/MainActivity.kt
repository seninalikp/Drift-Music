package com.soundify.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.soundify.player.ui.DarkDriftColors
import com.soundify.player.ui.LightDriftColors
import com.soundify.player.ui.LocalDriftColors
import com.soundify.player.ui.MainScreen
import com.soundify.player.ui.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val driftColors = if (isDarkMode) DarkDriftColors else LightDriftColors

            CompositionLocalProvider(LocalDriftColors provides driftColors) {
                MaterialTheme(
                    colorScheme = if (isDarkMode) {
                        darkColorScheme(
                            primary = Color(0xFF38BDF8),
                            background = Color(0xFF121212),
                            surface = Color(0xFF181818),
                            onBackground = Color.White,
                            onSurface = Color.White
                        )
                    } else {
                        lightColorScheme(
                            primary = Color(0xFF0284C7),
                            background = Color(0xFFF8FAFC),
                            surface = Color(0xFFFFFFFF),
                            onBackground = Color(0xFF0F172A),
                            onSurface = Color(0xFF0F172A)
                        )
                    }
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
