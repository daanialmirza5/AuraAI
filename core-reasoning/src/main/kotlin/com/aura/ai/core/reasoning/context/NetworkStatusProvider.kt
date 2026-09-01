package com.aura.ai.core.reasoning.context

/** [Unknown] again distinct from [Offline] — see [PermissionChecker]'s doc for why that
 *  distinction matters to a caller deciding how cautious to be. */
enum class ConnectivityState { Online, Offline, Unknown }

/**
 * "Offline awareness" — whether the device currently has usable network connectivity.
 * Provider-agnostic the same way [PermissionChecker] is: this interface and its honest
 * [UnknownNetworkStatusProvider] default live in pure-JVM core-reasoning; a real
 * `ConnectivityManager`-backed implementation is bound at the `:app` layer.
 */
interface NetworkStatusProvider {
    fun currentState(): ConnectivityState
}

class UnknownNetworkStatusProvider : NetworkStatusProvider {
    override fun currentState(): ConnectivityState = ConnectivityState.Unknown
}
