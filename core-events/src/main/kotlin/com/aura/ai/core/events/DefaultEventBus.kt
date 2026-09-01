package com.aura.ai.core.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only [EventBus] this phase ships — a [MutableSharedFlow] with no replay (subscribers only
 * see events from the moment they start collecting) and a bounded extra buffer so a burst of
 * events from a parallel `ExecutionCoordinator` wave never blocks the agent that published them;
 * if a subscriber falls far enough behind to fill the buffer, the oldest buffered event is
 * dropped rather than backpressuring publishers — an event bus for coordination/telemetry should
 * never be able to stall the thing it's just observing.
 */
@Singleton
class DefaultEventBus
    @Inject
    constructor() : EventBus {
        private companion object {
            const val EXTRA_BUFFER_CAPACITY = 128
        }

        private val mutableEvents =
            MutableSharedFlow<AuraEvent>(
                replay = 0,
                extraBufferCapacity = EXTRA_BUFFER_CAPACITY,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        override suspend fun publish(event: AuraEvent) {
            mutableEvents.emit(event)
        }

        override fun events(): Flow<AuraEvent> = mutableEvents.asSharedFlow()
    }
