package com.ecoalertum.android

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.ecoalertum.android.alerts.AlertNotifier
import com.ecoalertum.android.core.data.repository.DataState
import com.ecoalertum.android.core.ui.EcoAlertApp
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // FR2.1: asks for notification permission (Android 13+), then checks the alerts.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) checkAlertsForNotifications()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
            FirebaseFunctions.getInstance().useEmulator("10.0.2.2", 5001)
        }

        // FR2.1: notifications
        AlertNotifier.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            checkAlertsForNotifications()
        }

        enableEdgeToEdge()

        setContent {
            EcoAlertUMTheme {
                EcoAlertApp()
            }
        }
    }

    /** FR2.1 (Alpha): reads the alerts once and notifies any new ones that match the rule. */
    private fun checkAlertsForNotifications() {
        val repository = (application as EcoAlertApplication).container.alertRepository
        lifecycleScope.launch {
            val state = repository.observeAlerts().first { it !is DataState.Loading }
            if (state is DataState.Success) {
                AlertNotifier.notifyNewAlerts(this@MainActivity, state.data)
            }
        }
    }
}