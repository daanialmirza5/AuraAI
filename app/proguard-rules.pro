# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.aura.ai.data.local.entity.** { *; }

# Google Tink (androidx.security.crypto's dependency, used by AndroidProviderCredentialStore for
# EncryptedSharedPreferences) references error-prone's annotations, which are compile-time-only
# (SOURCE/CLASS retention) and never actually needed at runtime — safe to silence, not to keep.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
