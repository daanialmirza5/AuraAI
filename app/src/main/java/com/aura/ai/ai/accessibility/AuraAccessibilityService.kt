package com.aura.ai.ai.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deliberately narrow: reads the visible screen's text and can dispatch the three global system
 * actions (back/home/recents) — nothing else. No gesture dispatch, no simulated taps on other
 * apps' UI elements. That's the highest-risk, hardest-to-verify-safe capability an
 * `AccessibilityService` can have, and this codebase has no physical device to verify it against;
 * see `docs/ANDROID_AUTOMATION.md` §4 for the full reasoning — the same "implement what's
 * verifiable, document what isn't" judgment call as acoustic voice barge-in in
 * `docs/VOICE_RUNTIME.md` §3.
 *
 * Companion object exposes state because Android instantiates and destroys this service itself
 * (never through Hilt/DI) — [ReadScreenTool] and [DeviceNavigationTool] read the live instance
 * (or its absence) through here, the same pattern any Android app uses to bridge a system-owned
 * service to the rest of its own code.
 */
class AuraAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val text = rootInActiveWindow?.let { collectText(it) }?.takeIf { it.isNotBlank() }
        _latestScreenText.value = text
    }

    override fun onInterrupt() {
        _latestScreenText.value = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
        _latestScreenText.value = null
    }

    private fun collectText(
        node: AccessibilityNodeInfo,
        depth: Int = 0,
    ): String {
        if (depth > 40) return "" // guards against a pathologically deep view tree
        val own = node.text?.toString().orEmpty()
        val children =
            (0 until node.childCount)
                .mapNotNull { i -> node.getChild(i) }
                .joinToString(" ") { collectText(it, depth + 1) }
        return listOf(own, children).filter { it.isNotBlank() }.joinToString(" ")
    }

    companion object {
        private var instance: AuraAccessibilityService? = null

        private val _latestScreenText = MutableStateFlow<String?>(null)
        val latestScreenText: StateFlow<String?> = _latestScreenText.asStateFlow()

        val isEnabled: Boolean get() = instance != null

        /** `false` if the service isn't currently enabled — the honest failure every other
         *  Android-integration point in this codebase reports rather than silently no-op-ing. */
        fun performGlobalAction(action: Int): Boolean = instance?.performGlobalAction(action) ?: false
    }
}
