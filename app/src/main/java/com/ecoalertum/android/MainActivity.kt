package com.ecoalertum.android

import android.os.Bundle
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ecoalertum.android.core.ui.EcoAlertApp
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
            FirebaseFunctions.getInstance().useEmulator("10.0.2.2", 5001)
        }

        enableEdgeToEdge()

        setContent {
            EcoAlertUMTheme {
                EcoAlertApp()
            }
        }
    }
}
