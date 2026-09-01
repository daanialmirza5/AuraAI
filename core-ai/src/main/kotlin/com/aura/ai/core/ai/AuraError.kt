package com.aura.ai.core.ai

/** The closed set of failure reasons any AURA core module can report via [AuraResult.Failure]. */
sealed class AuraError(
    open val message: String,
    open val cause: Throwable? = null,
) {
    data class Network(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AuraError(message, cause)

    data class Authentication(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AuraError(message, cause)

    data class RateLimited(
        override val message: String,
        val retryAfterMillis: Long? = null,
    ) : AuraError(message)

    data class InvalidRequest(
        override val message: String,
    ) : AuraError(message)

    /** A provider exists and is configured, but is temporarily unreachable/erroring. */
    data class ProviderUnavailable(
        val providerId: String,
        override val message: String,
    ) : AuraError(message)

    /** The honest state of every provider in this phase — scaffolded, but no client is wired yet. */
    data class ProviderNotConnected(
        val providerId: String,
        override val message: String,
    ) : AuraError(message)

    /** The capability exists conceptually (e.g. "Coding") but has no local execution path —
     *  it genuinely requires a connected AI provider, which this phase intentionally has none of. */
    data class RequiresProvider(
        override val message: String,
    ) : AuraError(message)

    data class NotSupported(
        override val message: String,
    ) : AuraError(message)

    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AuraError(message, cause)
}
