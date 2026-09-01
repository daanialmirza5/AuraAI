package com.aura.ai.core.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * The internal Event Bus — "every module communicates using events." A single, process-wide
 * publish/subscribe channel: [publish] never fails and never blocks the publisher on a slow
 * subscriber (see `DefaultEventBus`'s buffering policy); [events] is a cold view any number of
 * subscribers can collect independently, each seeing only events published *after* they start
 * collecting — this is a live bus, not a replay log.
 */
interface EventBus {
    suspend fun publish(event: AuraEvent)

    fun events(): Flow<AuraEvent>
}

/** Subscribe to exactly one event type — the common case, so callers don't have to
 *  `events().filterIsInstance<T>()` themselves at every call site. */
inline fun <reified T : AuraEvent> EventBus.on(): Flow<T> = events().filterIsInstance()
