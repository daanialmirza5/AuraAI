package com.aura.ai.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * Thin wrapper around androidx.biometric so the Login screen can request a real
 * device authentication (fingerprint/face, falling back to PIN/pattern/password —
 * the system prompt itself offers that fallback, which is what the source design's
 * "Use passcode instead" link maps onto).
 */
class BiometricAuthenticator(
    private val activity: FragmentActivity,
) {
    fun isAvailable(): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        title: String = "Authenticate",
        subtitle: String = "Biometric handshake required to link AURA to this device",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    onError(errString.toString())
                }

                // A presented biometric didn't match — the system prompt stays open for retry,
                // this is not a terminal error and must not be conflated with onAuthenticationError.
                override fun onAuthenticationFailed() {
                    onFailed()
                }
            }
        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
