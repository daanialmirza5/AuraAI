package com.aura.ai.ai.reasoning

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.aura.ai.core.reasoning.context.BatteryStatus
import com.aura.ai.core.reasoning.context.BatteryStatusProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The real [BatteryStatusProvider] — "battery awareness" made concrete. Reads the same sticky
 * `ACTION_BATTERY_CHANGED` broadcast `com.aura.ai.core.util.rememberBatteryState` uses for the
 * Home/Profile battery UI, but as a plain on-demand read (registering a `null` receiver returns
 * the current sticky intent synchronously) rather than a `@Composable` — this needs to run from
 * plain reasoning code, not the UI layer.
 */
class AndroidBatteryStatusProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BatteryStatusProvider {
        private companion object {
            const val LOW_BATTERY_THRESHOLD_PERCENT = 20
        }

        override fun currentStatus(): BatteryStatus? {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return null

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val percent = (level * 100) / scale

            return BatteryStatus(
                percent = percent,
                isCharging = isCharging,
                isLow = percent <= LOW_BATTERY_THRESHOLD_PERCENT,
            )
        }
    }
