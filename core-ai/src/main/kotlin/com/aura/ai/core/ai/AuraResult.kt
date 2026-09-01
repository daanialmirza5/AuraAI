package com.aura.ai.core.ai

/**
 * The result type used at every boundary across the AURA core modules — every
 * suspend/async operation in core-ai, core-memory, core-intent, core-planner,
 * core-actions, core-providers and core-tools returns one of these rather than
 * throwing, so callers (eventually the app's ViewModels) can handle failure
 * uniformly regardless of which layer or provider produced it.
 */
sealed interface AuraResult<out T> {
    data class Success<out T>(
        val value: T,
    ) : AuraResult<T>

    data class Failure(
        val error: AuraError,
    ) : AuraResult<Nothing>
}

inline fun <T, R> AuraResult<T>.map(transform: (T) -> R): AuraResult<R> =
    when (this) {
        is AuraResult.Success -> AuraResult.Success(transform(value))
        is AuraResult.Failure -> this
    }

inline fun <T> AuraResult<T>.onSuccess(action: (T) -> Unit): AuraResult<T> {
    if (this is AuraResult.Success) action(value)
    return this
}

inline fun <T> AuraResult<T>.onFailure(action: (AuraError) -> Unit): AuraResult<T> {
    if (this is AuraResult.Failure) action(error)
    return this
}

fun <T> AuraResult<T>.getOrNull(): T? = (this as? AuraResult.Success)?.value

fun <T> AuraResult<T>.getOrElse(default: (AuraError) -> T): T =
    when (this) {
        is AuraResult.Success -> value
        is AuraResult.Failure -> default(error)
    }

val AuraResult<*>.isSuccess: Boolean get() = this is AuraResult.Success
val AuraResult<*>.isFailure: Boolean get() = this is AuraResult.Failure

fun <T> success(value: T): AuraResult<T> = AuraResult.Success(value)

fun failure(error: AuraError): AuraResult<Nothing> = AuraResult.Failure(error)
