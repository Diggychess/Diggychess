package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.KnightScopeRepository
import com.example.ui.KnightScopeApp
import com.example.ui.theme.KnightScopeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = KnightScopeRepository(applicationContext)
        setContent {
            KnightScopeTheme {
                KnightScopeApp(repo = repository)
            }
        }
    }
}
