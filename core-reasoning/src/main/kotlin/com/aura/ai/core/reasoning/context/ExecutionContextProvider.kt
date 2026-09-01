package com.aura.ai.core.reasoning.context

import com.aura.ai.core.providers.AIProviderManager
import com.aura.ai.core.tools.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assembles one [ExecutionContext] snapshot from every underlying source — [ToolRegistry] for
 * what's callable, [PermissionChecker]/[NetworkStatusProvider]/[BatteryStatusProvider] for device
 * state, [AIProviderManager] for whether AI generation is currently possible. The one place these
 * four otherwise-unrelated sources are read together.
 */
interface ExecutionContextProvider {
    /** [relevantPermissions] is supplied by the caller (in practice, `CapabilityResolver`'s
     *  answer for this specific goal's tools) rather than checked exhaustively — no reason to ask
     *  about `READ_CONTACTS` for a goal that will never touch a contacts-reading tool. */
    suspend fun capture(relevantPermissions: Set<String> = emptySet()): ExecutionContext
}

@Singleton
class DefaultExecutionContextProvider
    @Inject
    constructor(
        private val toolRegistry: ToolRegistry,
        private val permissionChecker: PermissionChecker,
        private val networkStatusProvider: NetworkStatusProvider,
        private val batteryStatusProvider: BatteryStatusProvider,
        private val aiProviderManager: AIProviderManager,
    ) : ExecutionContextProvider {
        override suspend fun capture(relevantPermissions: Set<String>): ExecutionContext =
            ExecutionContext(
                availableTools = toolRegistry.descriptors(),
                connectivity = networkStatusProvider.currentState(),
                battery = batteryStatusProvider.currentStatus(),
                permissionState = permissionChecker.checkAll(relevantPermissions),
                aiProviderAvailable = aiProviderManager.activeProvider.value.isAvailable(),
                capturedAtMillis = System.currentTimeMillis(),
            )
    }
