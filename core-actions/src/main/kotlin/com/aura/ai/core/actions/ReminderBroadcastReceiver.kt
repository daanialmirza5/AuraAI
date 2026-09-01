package com.aura.ai.core.actions

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Fires when a [ReminderTool]-scheduled alarm goes off — shows the reminder as a notification.
 *  Declared in this module's own AndroidManifest.xml ([android:exported]="false": it only ever
 *  needs to receive the [PendingIntent] this module itself creates). */
class ReminderBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_MESSAGE = "com.aura.ai.core.actions.EXTRA_MESSAGE"
        const val EXTRA_REQUEST_CODE = "com.aura.ai.core.actions.EXTRA_REQUEST_CODE"
    }

    // See NotifyTool.kt for why this is a verified Lint false positive, not a suppressed real risk.
    @SuppressLint("MissingPermission")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, message.hashCode())

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationChannels.ensureChannel(context)
            val notification =
                NotificationCompat
                    .Builder(context, NotificationChannels.AURA_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_aura_notification)
                    .setContentTitle("AURA reminder")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        }
    }
}
