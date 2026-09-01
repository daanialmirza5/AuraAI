package com.aura.ai.domain.model

enum class ThemePreview { Dark, Light }

enum class AssistantVoice { Jarvis, Friday, Neutral }

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val isAuthenticated: Boolean = false,
    val grantedPermissions: Set<AppPermission> = emptySet(),
    val themePreview: ThemePreview = ThemePreview.Dark,
    val assistantVoice: AssistantVoice = AssistantVoice.Jarvis,
    /** After AURA finishes speaking a response, automatically listen for a follow-up instead of
     *  requiring another orb tap. Bounded by [android.speech.SpeechRecognizer]'s own silence
     *  timeout — there is no separate, hand-rolled timer for this. See `docs/VOICE_RUNTIME.md`. */
    val voiceContinuousConversationEnabled: Boolean = true,
)
