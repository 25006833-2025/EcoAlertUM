package com.ecoalertum.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ecoalertum.android.core.ui.EcoAlertApp
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoAlertUMTheme {
                EcoAlertApp()
            }
        }
    }
}
