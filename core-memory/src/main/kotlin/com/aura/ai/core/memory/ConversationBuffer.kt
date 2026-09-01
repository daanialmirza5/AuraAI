package com.aura.ai.core.memory

import com.aura.ai.core.ai.AiMessage

/**
 * The short-term rolling window of raw dialogue turns fed into the next
 * [com.aura.ai.core.ai.AIProvider] call — the first of three distinct memory layers this module
 * provides, each with a different lifetime and shape:
 *
 * 1. **`ConversationBuffer`** (this) — raw [AiMessage] turns, trimmed to the last ~20, gone once
 *    the buffer rolls past them.
 * 2. **`com.aura.ai.core.memory.store.WorkingMemoryStore`** — structured [MemoryEntry]-shaped
 *    facts salient *right now* ("the user just mentioned Monday's exam"), TTL-expiring.
 * 3. **`com.aura.ai.core.memory.store.LongTermMemoryStore`** — durable, searchable, ranked
 *    facts meant to outlive the session.
 */
interface ConversationBuffer {
    fun append(turn: AiMessage)

    fun current(): List<AiMessage>

    fun clear()
}
