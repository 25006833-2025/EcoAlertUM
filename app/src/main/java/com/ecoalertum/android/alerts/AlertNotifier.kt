package com.ecoalertum.android.alerts

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ecoalertum.android.MainActivity
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.home.labelRes

/** FR2.1: shows a phone notification when an alert matches the alert rule. */
object AlertNotifier {

    private const val CHANNEL_ID = "hazard_alerts"
    private const val PREFS = "alert_notifier"
    private const val KEY_NOTIFIED = "notified_ids"

    /** Alert rule (Alpha): active alerts at Advisory or above. FR2.2 settings will change this later. */
    private val MIN_LEVEL = Level.ADVISORY

    /** Android 8+ needs a "channel" before showing notifications. Safe to call many times. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.alert_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /** Applies the alert rule and notifies each matching alert only once. */
    fun notifyNewAlerts(context: Context, alerts: List<Alert>) {
        if (!canNotify(context)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val notified = prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().toMutableSet()

        alerts
            .filter { it.status == AlertStatus.ACTIVE && it.level.isAtLeast(MIN_LEVEL) && it.id !in notified }
            .forEach { alert ->
                show(context, alert)
                notified += alert.id
            }
        prefs.edit().putStringSet(KEY_NOTIFIED, notified).apply()
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked in canNotify()
    private fun show(context: Context, alert: Alert) {
        // Tapping opens the alert detail screen through the deep link in EcoAlertNavHost.
        val intent = Intent(Intent.ACTION_VIEW, "ecoalertum://alert/${alert.id}".toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Severity word first, as in the wireframe: "ADVISORY · Heavy rain on campus"
        val levelWord = context.getString(alert.level.labelRes).uppercase()
        val message = alert.message.get("en")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$levelWord · ${alert.title.get("en")}")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
    }
}