package com.aura.ai.core.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Shared by [ReminderBroadcastReceiver] and `NotifyTool` — one channel, created lazily,
 *  idempotently. No `SDK_INT >= O` guard needed: `minSdk` is already 26 (`O`) project-wide. */
internal object NotificationChannels {
    const val AURA_CHANNEL_ID = "aura_core_actions"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(AURA_CHANNEL_ID) != null) return
        val channel =
            NotificationChannel(
                AURA_CHANNEL_ID,
                "AURA",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Reminders and alerts AURA schedules on your behalf."
            }
        manager.createNotificationChannel(channel)
    }
}
