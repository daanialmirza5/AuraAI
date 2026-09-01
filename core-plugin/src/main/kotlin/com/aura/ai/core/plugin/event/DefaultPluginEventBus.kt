package com.aura.ai.core.plugin.event

import com.aura.ai.plugin.api.PluginEvent
import com.aura.ai.plugin.api.PluginEventBus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One shared bus every plugin's [com.aura.ai.plugin.api.PluginContext.events] points at — the
 * same no-replay, bounded-drop-oldest design as `core-events.DefaultEventBus`, for the same
 * reason: publishing must never block on a slow subscriber. `plugin-runtime` publishes every
 * lifecycle transition here (`PluginEvent.Loaded`/`Enabled`/`Disabled`/`Unloaded`/`Failed`) in
 * addition to whatever `PluginEvent.Custom` events plugins publish themselves.
 */
@Singleton
class DefaultPluginEventBus
    @Inject
    constructor() : PluginEventBus {
        private companion object {
            const val EXTRA_BUFFER_CAPACITY = 128
        }

        private val mutableEvents =
            MutableSharedFlow<PluginEvent>(
                replay = 0,
                extraBufferCapacity = EXTRA_BUFFER_CAPACITY,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        override suspend fun publish(event: PluginEvent.Custom) {
            mutableEvents.emit(event)
        }

        /** Internal — `plugin-runtime` publishes lifecycle events through this, not the plugin-facing
         *  [publish] (which only accepts [PluginEvent.Custom]). */
        suspend fun publishLifecycleEvent(event: PluginEvent) {
            mutableEvents.emit(event)
        }

        override fun events(): Flow<PluginEvent> = mutableEvents.asSharedFlow()
    }
