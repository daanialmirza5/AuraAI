package com.aura.ai.core.reasoning.context

/** [Unknown] is a distinct, honest outcome from [Denied] — core-reasoning is pure Kotlin/JVM and
 *  has no Android `PackageManager` access itself (see [UnknownPermissionChecker]); "I couldn't
 *  check" and "I checked and it's off" are different facts and callers should treat them
 *  differently (the former is a reason to be cautious, not a confirmed blocker). */
enum class PermissionState { Granted, Denied, Unknown }

/**
 * Checks whether an Android permission is currently granted. Provider-agnostic by the same
 * pattern as `com.aura.ai.core.memory.embedding.EmbeddingProvider`: this interface lives in
 * pure-JVM core-reasoning, [UnknownPermissionChecker] is the only implementation this module
 * ships, and a real Android-backed implementation (reading `ContextCompat.checkSelfPermission`)
 * is bound at the `:app` layer, which is the one place in this codebase allowed to hold a
 * `Context`.
 */
interface PermissionChecker {
    fun check(permission: String): PermissionState

    fun checkAll(permissions: Collection<String>): Map<String, PermissionState> = permissions.associateWith { check(it) }
}

/** The honest default: no Android context is reachable from core-reasoning itself, so every
 *  permission is reported [PermissionState.Unknown] rather than guessed at. */
class UnknownPermissionChecker : PermissionChecker {
    override fun check(permission: String): PermissionState = PermissionState.Unknown
}
