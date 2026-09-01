package com.aura.ai.core.reasoning.capability

import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.tools.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The default [CapabilityResolver]. [PERMISSION_REQUIREMENTS] and [NETWORK_REQUIREMENTS] are a
 * hand-maintained mirror of what core-actions' `Tool` implementations actually do — not a
 * guess. `notify` needs `POST_NOTIFICATIONS` unconditionally; every other tool either hands off
 * to another app via an `Intent` — which needs nothing from AURA itself — or writes to the app's
 * own private storage, which is unrestricted on every supported API level. Three tools need live
 * connectivity to be useful (`web_search`, `shopping_search`, `navigate`); the rest are fully
 * on-device. A tool name absent from either map is assumed to need nothing — the safe default for
 * a tool that doesn't exist yet.
 *
 * Known, deliberate gap: `file_search` (`FileSearchTool`) needs `READ_EXTERNAL_STORAGE`, but only
 * on API 26–28 — API 29+ queries the same `MediaStore.Downloads` collection without it (scoped
 * storage's own exception). This map has no per-API-level dimension, so modeling it here would
 * make either range wrong; `file_search` is left out and the tool itself is the source of truth
 * for that one narrow, version-conditional case (it checks and reports honestly at call time).
 *
 * This map is the one place that would need a new entry when core-actions gains a tool with a
 * real, unconditional permission or network requirement — nothing about `ConstraintEngine`,
 * `DecisionEngine`, or `ActionValidator` would need to change, since they all read through this
 * interface.
 */
@Singleton
class DefaultCapabilityResolver
    @Inject
    constructor(
        private val toolRegistry: ToolRegistry,
    ) : CapabilityResolver {
        private companion object {
            val PERMISSION_REQUIREMENTS: Map<String, List<String>> =
                mapOf(
                    "notify" to listOf("android.permission.POST_NOTIFICATIONS"),
                )

            val NETWORK_REQUIREMENTS: Set<String> = setOf("web_search", "shopping_search", "navigate")
        }

        override fun availableTools(): List<ToolDescriptor> = toolRegistry.descriptors()

        override fun requiredPermissions(toolName: String): List<String> = PERMISSION_REQUIREMENTS[toolName] ?: emptyList()

        override fun requiresNetwork(toolName: String): Boolean = toolName in NETWORK_REQUIREMENTS
    }
