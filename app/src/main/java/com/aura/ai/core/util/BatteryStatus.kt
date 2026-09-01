package com.aura.ai.core.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class BatteryState(
    val percent: Int = 100,
    val isCharging: Boolean = false,
    val chargeTimeRemainingMillis: Long? = null,
)

/** Reads the real device battery via the sticky ACTION_BATTERY_CHANGED broadcast — no
 *  fabricated numbers, used by both Home's Battery tile and Profile's Battery Monitor. */
@Composable
fun rememberBatteryState(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BatteryState()) }

    DisposableEffect(context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val charging =
                        status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 100

                    val remaining =
                        if (charging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                            manager?.computeChargeTimeRemaining()?.takeIf { it > 0 }
                        } else {
                            null
                        }

                    state = BatteryState(percent = percent, isCharging = charging, chargeTimeRemainingMillis = remaining)
                }
            }
        val sticky = context.registerReceiver(receiver, filter)
        sticky?.let { receiver.onReceive(context, it) }
        onDispose { context.unregisterReceiver(receiver) }
    }

    return state
}
