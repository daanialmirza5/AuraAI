package com.aura.ai.plugin.api

/**
 * The plugin SDK's own result type — deliberately not `com.aura.ai.core.ai.AuraResult`. This
 * module has zero dependency on `core-ai` (or anything else internal) on purpose: a plugin
 * author's compiled code should never see, and can never accidentally import, an internal AURA
 * type. `PluginResult` mirrors `AuraResult`'s shape exactly (a closed `Success`/`Failure` pair)
 * because it's a good shape, not because it's shared code — see `docs/PLUGIN_SECURITY.md` for why
 * this duplication is the point, not an oversight.
 */
sealed interface PluginResult<out T> {
    data class Success<out T>(
        val value: T,
    ) : PluginResult<T>

    data class Failure(
        val error: PluginError,
    ) : PluginResult<Nothing>
}

/** The closed set of failure reasons a plugin (or the host, on a plugin's behalf) can report.
 *  Deliberately smaller than `core-ai.AuraError` — a plugin never needs to know about
 *  provider-connection states or rate limits; it only needs to know whether its own request was
 *  valid, whether it was allowed to do something, and a catch-all for everything else. */
sealed class PluginError(
    open val message: String,
) {
    data class InvalidRequest(
        override val message: String,
    ) : PluginError(message)

    data class PermissionDenied(
        val permission: PluginPermission,
        override val message: String,
    ) : PluginError(message)

    data class NotSupported(
        override val message: String,
    ) : PluginError(message)

    data class Unknown(
        override val message: String,
        val cause: Throwable? = null,
    ) : PluginError(message)
}

inline fun <T, R> PluginResult<T>.map(transform: (T) -> R): PluginResult<R> =
    when (this) {
        is PluginResult.Success -> PluginResult.Success(transform(value))
        is PluginResult.Failure -> this
    }

fun <T> PluginResult<T>.getOrNull(): T? = (this as? PluginResult.Success)?.value
