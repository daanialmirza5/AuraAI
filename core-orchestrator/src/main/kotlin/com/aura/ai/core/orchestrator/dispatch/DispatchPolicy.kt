package com.aura.ai.core.orchestrator.dispatch

/** "Retries" and "Timeouts," made configurable rather than hardcoded — a caller that knows a
 *  particular agent is slow or flaky can override the defaults per [com.aura.ai.core.orchestrator.dispatch.TaskDispatcher.dispatch] call. */
data class DispatchPolicy(
    val maxAttempts: Int = 2,
    val retryDelayMillis: Long = 250,
    val timeoutMillis: Long = 15_000,
)
