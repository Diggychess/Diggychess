package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.KnightScopeRepository
import com.example.ui.KnightScopeApp
import com.example.ui.theme.KnightScopeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.audio.ChessSoundPlayer.ensureInit(applicationContext)
        val repository = KnightScopeRepository(applicationContext)
        setContent {
            val isDarkMode by repository.darkModeFlow.collectAsStateWithLifecycle(initialValue = true)
            val pieceAnimations by repository.pieceAnimationsFlow.collectAsStateWithLifecycle(initialValue = true)
            val pieceStyle by repository.pieceStyleFlow.collectAsStateWithLifecycle(initialValue = com.example.ui.theme.PieceStyle.NEO)
            val board3dEnabled by repository.board3dEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
            val soundEnabled by repository.soundEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
            val hapticsEnabled by repository.hapticsEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
            LaunchedEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
            }
            val repo = remember { repository }
            KnightScopeTheme(
                darkTheme = isDarkMode,
                pieceAnimations = pieceAnimations,
                pieceStyle = pieceStyle,
                board3dEnabled = board3dEnabled,
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled
            ) {
                KnightScopeApp(repo = repo)
            }
        }
    }
}
