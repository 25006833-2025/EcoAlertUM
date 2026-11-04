package com.ecoalertum.android

import android.os.Bundle
import android.content.pm.ApplicationInfo
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import android.app.AlertDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ecoalertum.android.core.ui.EcoAlertApp
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        }

        enableEdgeToEdge()

        setContent {
            EcoAlertUMTheme {
                EcoAlertApp()
            }
        }

        showTemporarySignIn()
    }

    private fun showTemporarySignIn() {
        val passwordInput = EditText(this).apply {
            hint = "Enter emulator test password"
            inputType = 129
        }

        AlertDialog.Builder(this)
            .setTitle("Temporary Firebase Test")
            .setMessage("Sign in with test@siswa.um.edu.my")
            .setView(passwordInput)
            .setPositiveButton("Sign in") { _, _ ->
                val password = passwordInput.text.toString()

                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        "test@siswa.um.edu.my",
                        password
                    )
                    .addOnSuccessListener {
                        Log.d("AuthEmulatorTest", "Sign-in successful")

                        FirebaseFirestore.getInstance()
                            .collection("alerts")
                            .document("alert_001")
                            .get()
                            .addOnSuccessListener { document ->
                                Log.d(
                                    "FirestoreEmulatorTest",
                                    "Read successful. Document exists: ${document.exists()}"
                                )
                                Toast.makeText(
                                    this,
                                    "Firestore read successful!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener { exception ->
                                Log.e(
                                    "FirestoreEmulatorTest",
                                    "Firestore read failed: ${exception.message}",
                                    exception
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            "AuthEmulatorTest",
                            "Sign-in failed: ${exception.message}",
                            exception
                        )
                        Toast.makeText(
                            this,
                            "Sign-in failed. Check Logcat.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}