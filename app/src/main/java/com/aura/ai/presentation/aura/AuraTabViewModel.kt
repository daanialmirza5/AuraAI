package com.aura.ai.presentation.aura

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.ai.runtime.AuraRuntimeFacade
import com.aura.ai.ai.runtime.ExecutionTrace
import com.aura.ai.ai.voice.VoiceListenResult
import com.aura.ai.ai.voice.VoiceRuntime
import com.aura.ai.domain.model.ChatMessage
import com.aura.ai.domain.model.Memory
import com.aura.ai.domain.repository.ChatRepository
import com.aura.ai.domain.repository.MemoryRepository
import com.aura.ai.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AuraTabUiState(
    val sub: AuraSub = AuraSub.Orb,
    val orbState: OrbState = OrbState.Idle,
    val messages: ImmutableList<ChatMessage> = persistentListOf(),
    val chatInput: String = "",
    val isTyping: Boolean = false,
    val memories: ImmutableList<Memory> = persistentListOf(),
    /** "The debug trace should remain optional" — off by default; the chat itself never renders
     *  differently based on this, only whether [selectedTrace] can ever be non-null. */
    val developerModeEnabled: Boolean = false,
    /** The trace for whichever assistant message the user last asked to inspect, or `null` when
     *  nothing is currently being inspected. See `com.aura.ai.ai.runtime.ExecutionTrace`. */
    val selectedTrace: ExecutionTrace? = null,
    /** A genuine voice-runtime problem worth surfacing (device has no speech recognizer, a real
     *  recognition error) — `null` for the expected, silent "user said nothing" case. Transient:
     *  cleared by [AuraTabViewModel.dismissVoiceError] or the next voice turn starting. */
    val voiceError: String? = null,
)

private const val KEY_CHAT_INPUT = "chatInput"

/**
 * "AuraTabViewModel must no longer contain business logic." Every method here does exactly one
 * of: relay a user action to [AuraRuntimeFacade]/[chatRepository]/[memoryRepository], or shape
 * their output into [AuraTabUiState] for Compose. Sending a message — recognizing intent,
 * retrieving memory, reasoning, planning, orchestrating agents, executing tools, falling back
 * honestly when a provider is needed and isn't connected — is entirely
 * [AuraRuntimeFacade]'s job; see `docs/AURA_RUNTIME.md`.
 */
@HiltViewModel
class AuraTabViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val chatRepository: ChatRepository,
        private val memoryRepository: MemoryRepository,
        private val preferencesRepository: PreferencesRepository,
        private val auraRuntimeFacade: AuraRuntimeFacade,
        private val voiceRuntime: VoiceRuntime,
    ) : ViewModel() {
        private val sub = MutableStateFlow(AuraSub.fromKey(savedStateHandle["sub"]))
        private val orbState = MutableStateFlow(OrbState.Idle)

        // SavedStateHandle-backed so an in-progress draft survives process death, not just
        // configuration changes — orbState/isTyping deliberately aren't (see onOrbTap/sendChat):
        // resuming a "typing…" state with no live coroutine behind it would hang forever.
        private val chatInput = savedStateHandle.getStateFlow(KEY_CHAT_INPUT, "")
        private val isTyping = MutableStateFlow(false)
        private val developerModeEnabled = MutableStateFlow(false)
        private val selectedTrace = MutableStateFlow<ExecutionTrace?>(null)
        private val voiceError = MutableStateFlow<String?>(null)

        private var orbJob: Job? = null
        private var replyJob: Job? = null

        private data class CoreFlags(
            val sub: AuraSub,
            val orbState: OrbState,
            val chatInput: String,
            val isTyping: Boolean,
            val developerModeEnabled: Boolean,
        )

        // combine() only has typed overloads up to 5 flows; selectedTrace is folded in with a second,
        // 2-flow combine rather than reaching for the untyped vararg overload, which would need an
        // unsafe cast per field instead of the compiler checking each one.
        private val coreFlags =
            combine(sub, orbState, chatInput, isTyping, developerModeEnabled) { s, o, input, typing, devMode ->
                CoreFlags(s, o, input, typing, devMode)
            }

        private data class LocalFlags(
            val core: CoreFlags,
            val selectedTrace: ExecutionTrace?,
            val voiceError: String?,
        )

        private val localFlags =
            combine(coreFlags, selectedTrace, voiceError) { core, trace, err ->
                LocalFlags(core, trace, err)
            }

        val uiState: StateFlow<AuraTabUiState> =
            combine(
                localFlags,
                chatRepository.observeMessages().map { it.toPersistentList() },
                memoryRepository.observeMemories().map { it.toPersistentList() },
            ) { flags, messages, memories ->
                AuraTabUiState(
                    sub = flags.core.sub,
                    orbState = flags.core.orbState,
                    messages = messages,
                    chatInput = flags.core.chatInput,
                    isTyping = flags.core.isTyping,
                    memories = memories,
                    voiceError = flags.voiceError,
                    developerModeEnabled = flags.core.developerModeEnabled,
                    selectedTrace = flags.selectedTrace,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuraTabUiState())

        fun setSub(value: AuraSub) {
            sub.value = value
        }

        /**
         * The orb is AURA's voice entry point — tap semantics depend on what's currently happening:
         * idle → start a turn; mid-listening → cancel it (the user changed their mind); mid-speaking →
         * interrupt (manual barge-in, see [VoiceRuntime.interruptSpeaking]) and immediately start a
         * new turn, since tapping while AURA is talking reads as "stop, I want to say something else."
         * A tap while [OrbState.Thinking] is ignored — a turn already fully committed to
         * [AuraRuntimeFacade] can't be un-sent.
         */
        fun onOrbTap() {
            when (orbState.value) {
                OrbState.Idle -> beginVoiceTurn()
                OrbState.Listening -> {
                    orbJob?.cancel()
                    orbState.value = OrbState.Idle
                }
                OrbState.Thinking -> Unit
                OrbState.Speaking -> {
                    orbJob?.cancel()
                    voiceRuntime.interruptSpeaking()
                    beginVoiceTurn()
                }
            }
        }

        private fun beginVoiceTurn() {
            voiceError.value = null
            orbJob?.cancel()
            orbJob = viewModelScope.launch { runVoiceTurn() }
        }

        /** One listen → think → speak cycle, looping back into itself for "conversation continuation"
         *  when [com.aura.ai.domain.model.UserPreferences.voiceContinuousConversationEnabled] is on
         *  and the previous turn produced a real response — recursion here is just "the next turn,"
         *  the same shape a `while` loop would have, chosen since each turn already needs its own
         *  local `when` branch on the listen result. */
        private suspend fun runVoiceTurn() {
            orbState.value = OrbState.Listening
            savedStateHandle[KEY_CHAT_INPUT] = ""
            val listened = voiceRuntime.listenOnce(onPartial = { savedStateHandle[KEY_CHAT_INPUT] = it })
            savedStateHandle[KEY_CHAT_INPUT] = ""
            when (listened) {
                is VoiceListenResult.Transcript -> {
                    orbState.value = OrbState.Thinking
                    isTyping.value = true
                    val result = auraRuntimeFacade.sendMessage(listened.text)
                    isTyping.value = false
                    val prefs = preferencesRepository.observePreferences().first()
                    orbState.value = OrbState.Speaking
                    voiceRuntime.speak(result.responseText, prefs.assistantVoice)
                    orbState.value = OrbState.Idle
                    if (prefs.voiceContinuousConversationEnabled) runVoiceTurn()
                }
                VoiceListenResult.NoSpeech -> orbState.value = OrbState.Idle
                VoiceListenResult.Unsupported -> {
                    voiceError.value = "Voice input isn't available on this device."
                    orbState.value = OrbState.Idle
                }
                is VoiceListenResult.Error -> {
                    voiceError.value = listened.message
                    orbState.value = OrbState.Idle
                }
            }
        }

        fun dismissVoiceError() {
            voiceError.value = null
        }

        fun onChatInputChange(value: String) {
            savedStateHandle[KEY_CHAT_INPUT] = value
        }

        fun sendChat() {
            val text = chatInput.value.trim()
            if (text.isEmpty()) return
            savedStateHandle[KEY_CHAT_INPUT] = ""
            replyJob?.cancel()
            isTyping.value = true
            replyJob =
                viewModelScope.launch {
                    auraRuntimeFacade.sendMessage(text)
                    isTyping.value = false
                }
        }

        fun forgetMemory(id: String) {
            viewModelScope.launch { memoryRepository.forget(id) }
        }

        fun toggleDeveloperMode() {
            developerModeEnabled.update { !it }
            if (!developerModeEnabled.value) selectedTrace.value = null
        }

        /** Called when the user asks to inspect one assistant message's trace — a no-op if
         *  developer mode is off or no trace was recorded for that message (e.g. it predates this
         *  session, since traces are in-memory only). */
        fun inspectTrace(messageId: Long) {
            if (!developerModeEnabled.value) return
            selectedTrace.value = auraRuntimeFacade.traceFor(messageId)
        }

        fun dismissTrace() {
            selectedTrace.value = null
        }
    }
