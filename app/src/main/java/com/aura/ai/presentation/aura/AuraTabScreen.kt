@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.presentation.aura

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.ai.runtime.ExecutionTrace
import com.aura.ai.core.designsystem.components.AccentCircleButton
import com.aura.ai.core.designsystem.components.AuraOrbStage
import com.aura.ai.core.designsystem.components.CloseIcon
import com.aura.ai.core.designsystem.components.EqualizerBars
import com.aura.ai.core.designsystem.components.GlassSurface
import com.aura.ai.core.designsystem.components.SendIcon
import com.aura.ai.core.designsystem.components.SubTabChipRow
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.InterFontFamily
import com.aura.ai.domain.model.ChatMessage
import com.aura.ai.domain.model.Memory
import com.aura.ai.domain.model.MessageSender
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay

@Composable
fun AuraTabRoute(viewModel: AuraTabViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Mic permission may have been declined at onboarding (see PermissionsScreen.kt) or revoked
    // since — checked fresh at the point of use, exactly like PermissionsRoute's own toggle does,
    // rather than trusting a possibly-stale PreferencesRepository snapshot from onboarding time.
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.onOrbTap()
        }
    val onOrbTap: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onOrbTap()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    AuraTabScreen(
        uiState = uiState,
        onSetSub = viewModel::setSub,
        onOrbTap = onOrbTap,
        onChatInputChange = viewModel::onChatInputChange,
        onSend = viewModel::sendChat,
        onForgetMemory = viewModel::forgetMemory,
        onToggleDeveloperMode = viewModel::toggleDeveloperMode,
        onInspectTrace = viewModel::inspectTrace,
        onDismissTrace = viewModel::dismissTrace,
        onDismissVoiceError = viewModel::dismissVoiceError,
    )
}

@Composable
private fun AuraTabScreen(
    uiState: AuraTabUiState,
    onSetSub: (AuraSub) -> Unit,
    onOrbTap: () -> Unit,
    onChatInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onForgetMemory: (String) -> Unit,
    onToggleDeveloperMode: () -> Unit,
    onInspectTrace: (Long) -> Unit,
    onDismissTrace: () -> Unit,
    onDismissVoiceError: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubTabChipRow(
                    items = AuraSub.entries,
                    selected = uiState.sub,
                    label = { it.name },
                    onSelect = onSetSub,
                    modifier = Modifier.weight(1f),
                )
                if (uiState.sub == AuraSub.Chat) {
                    DeveloperModeToggle(
                        enabled = uiState.developerModeEnabled,
                        onToggle = onToggleDeveloperMode,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.sub) {
                    AuraSub.Orb -> OrbContent(orbState = uiState.orbState, onOrbTap = onOrbTap)
                    AuraSub.Chat ->
                        ChatContent(
                            messages = uiState.messages,
                            chatInput = uiState.chatInput,
                            isTyping = uiState.isTyping,
                            developerModeEnabled = uiState.developerModeEnabled,
                            onInputChange = onChatInputChange,
                            onSend = onSend,
                            onInspectTrace = onInspectTrace,
                        )
                    AuraSub.Memory -> MemoryContent(memories = uiState.memories, onForget = onForgetMemory)
                }
            }
        }

        uiState.selectedTrace?.let { trace ->
            TraceOverlay(trace = trace, onDismiss = onDismissTrace)
        }

        uiState.voiceError?.let { message ->
            VoiceErrorBanner(message = message, onDismiss = onDismissVoiceError)
        }
    }
}

/** A transient, non-blocking notice for a genuine voice-runtime problem (device has no speech
 *  recognizer, a real recognition error) — the expected "user said nothing" case never reaches
 *  here, see [VoiceListenResult.NoSpeech][com.aura.ai.ai.voice.VoiceListenResult.NoSpeech].
 *  Auto-dismisses so it never blocks the orb the way [TraceOverlay] deliberately does. */
@Composable
private fun BoxScope.VoiceErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(message) {
        delay(4000)
        onDismiss()
    }
    GlassSurface(
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Text(
            text = message,
            style = AuraTextStyles.bodySmall,
            color = AuraColors.TextPrimary,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun DeveloperModeToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .padding(end = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) AuraColors.Accent.copy(alpha = 0.18f) else Color.Transparent)
                .border(1.dp, AuraColors.Accent.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = AuraColors.Accent),
                    onClick = onToggle,
                ).padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = "Dev",
            fontFamily = InterFontFamily,
            fontSize = 10.sp,
            color = if (enabled) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.4f),
        )
    }
}

/** A single, compact, always-optional view of an [ExecutionTrace] — every field the runtime
 *  pipeline produces, rendered as plain readable text rather than bespoke widgets per field,
 *  since this exists for debugging, not as a polished product surface. */
@Composable
private fun TraceOverlay(
    trace: ExecutionTrace,
    onDismiss: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        ) {
            val lines = remember(trace) { formatTrace(trace) }
            LazyColumn(
                modifier = Modifier.padding(18.dp).height(480.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Text(text = "Execution Trace", style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                }
                itemsIndexed(lines, key = { index, _ -> index }) { _, line ->
                    Text(
                        text = line,
                        fontFamily = InterFontFamily,
                        fontSize = 11.5.sp,
                        color = AuraColors.TextPrimary.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

private fun formatTrace(trace: ExecutionTrace): List<String> =
    buildList {
        add("Message: \"${trace.userMessage}\"")
        add("Intent: ${trace.recognizedIntent.type} (confidence ${"%.2f".format(trace.recognizedIntent.confidence)})")
        add("Verdict: ${trace.verdict}")
        add("Requires AI: ${trace.providerRequired} · Provider connected: ${trace.providerConnected}")
        add("Confidence (overall): ${"%.2f".format(trace.confidence.overall)}")
        add("Selected agents: ${trace.selectedAgents.ifEmpty { listOf("none") }.joinToString()}")
        add("Selected tools: ${trace.selectedTools.ifEmpty { listOf("none") }.joinToString()}")
        add("Retrieved memories: ${trace.retrievedMemories.size}")
        add("Execution plan steps: ${trace.executionPlan?.steps?.size ?: 0}")
        add("Total time: ${trace.timing.totalMillis}ms")
        trace.timing.stageMillis.forEach { (stage, millis) -> add("  · $stage: ${millis}ms") }
        add("Reasoning trace:")
        trace.reasoningTrace.explain().forEach { add("  · $it") }
        if (trace.alternatives.isNotEmpty()) {
            add("Alternatives:")
            trace.alternatives.forEach { add("  · ${it.description}") }
        }
        if (trace.recoveryPlan.actions.isNotEmpty()) {
            add("Recovery actions:")
            trace.recoveryPlan.actions.forEach { add("  · ${it.description}") }
        }
    }

@Composable
private fun OrbContent(
    orbState: OrbState,
    onOrbTap: () -> Unit,
) {
    val idle = orbState == OrbState.Idle
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = orbState.statusLabel,
            fontFamily = InterFontFamily,
            fontSize = 11.sp,
            letterSpacing = 0.2f.em,
            color = AuraColors.TextPrimary.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 24.dp),
        )

        AuraOrbStage(
            size = 220.dp,
            showScan = true,
            ringDuration = if (idle) 12000 else 4000,
            ringReverseDuration = if (idle) 16000 else 5000,
            scanDuration = if (idle) 5000 else 1600,
            pulseDuration =
                when (orbState) {
                    OrbState.Listening -> 1000
                    OrbState.Idle -> 3400
                    else -> 1800
                },
            glowAlpha = if (idle) 0.4f else 0.7f,
        )

        EqualizerBars(active = !idle, modifier = Modifier.padding(top = 34.dp))

        Text(
            text = orbState.caption,
            style = AuraTextStyles.body,
            color = AuraColors.TextPrimary.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 22.dp).width(260.dp),
        )

        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .padding(top = 26.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AuraColors.Accent.copy(alpha = 0.12f))
                    .border(1.dp, AuraColors.Accent.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(color = AuraColors.Accent),
                        onClick = onOrbTap,
                    ).padding(horizontal = 26.dp, vertical = 12.dp),
        ) {
            Text(text = orbState.buttonLabel, style = AuraTextStyles.micro, color = AuraColors.Accent)
        }
    }
}

@Composable
private fun ChatContent(
    messages: ImmutableList<ChatMessage>,
    chatInput: String,
    isTyping: Boolean,
    developerModeEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onInspectTrace: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            listState.animateScrollToItem((messages.size - 1 + if (isTyping) 1 else 0).coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    inspectable = developerModeEnabled && message.sender == MessageSender.Ai,
                    onInspectTrace = { onInspectTrace(message.id) },
                )
            }
            if (isTyping) {
                item { TypingIndicator() }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, AuraColors.Background)))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .padding(bottom = 96.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(AuraColors.SurfaceGlass.copy(alpha = 0.75f))
                        .border(1.dp, AuraColors.Accent.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (chatInput.isEmpty()) {
                    Text(
                        text = "Message AURA…",
                        style = AuraTextStyles.body,
                        color = AuraColors.TextPrimary.copy(alpha = 0.35f),
                    )
                }
                BasicTextField(
                    value = chatInput,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 14.sp, color = AuraColors.TextPrimary),
                    cursorBrush = SolidColor(AuraColors.Accent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AccentCircleButton(onClick = onSend, contentDescription = "Send message") {
                SendIcon(modifier = Modifier.size(17.dp))
            }
        }
    }
}

/** [inspectable] is true only for an assistant message while developer mode is on — "the UI
 *  should display only User Message, Assistant Response; the debug trace should remain
 *  optional" — a user message is never inspectable (it has no trace of its own), and neither is
 *  any message when developer mode is off. */
@Composable
private fun ChatBubble(
    message: ChatMessage,
    inspectable: Boolean,
    onInspectTrace: () -> Unit,
) {
    val isUser = message.sender == MessageSender.User
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        val shape =
            if (isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
            }
        val bg = if (isUser) AuraColors.Accent.copy(alpha = 0.16f) else AuraColors.SurfaceGlass.copy(alpha = 0.7f)
        val borderColor = if (isUser) AuraColors.Accent.copy(alpha = 0.3f) else AuraColors.TextPrimary.copy(alpha = 0.08f)
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.76f)
                    .clip(shape)
                    .background(bg)
                    .border(1.dp, if (inspectable) AuraColors.Accent.copy(alpha = 0.35f) else borderColor, shape)
                    .then(
                        if (inspectable) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = rememberRipple(color = AuraColors.Accent),
                                onClick = onInspectTrace,
                            )
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Text(text = message.text, style = AuraTextStyles.body, color = AuraColors.TextPrimary)
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                    .background(AuraColors.SurfaceGlass.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) { index ->
                val alpha = rememberTypingDotAlpha(delayMillis = index * 150)
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AuraColors.Accent.copy(alpha = alpha.value)),
                )
            }
        }
    }
}

/** auraPulse staggered per-dot via animation-delay, matching the source's .15s offsets. */
@Composable
private fun rememberTypingDotAlpha(delayMillis: Int): State<Float> {
    val transition = rememberInfiniteTransition(label = "typingDot")
    return transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delayMillis, StartOffsetType.Delay),
            ),
        label = "typingDotAlpha",
    )
}

@Composable
private fun MemoryContent(
    memories: ImmutableList<Memory>,
    onForget: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "AURA's long-term memory — what it has learned about you, editable and revocable at any time.",
                style = AuraTextStyles.body,
                color = AuraColors.TextPrimary.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (memories.isEmpty()) {
            item {
                Text(
                    text = "No memories yet — AURA will learn as you use it.",
                    style = AuraTextStyles.caption,
                    color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(memories, key = { it.id }) { memory ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuraColors.Accent.copy(alpha = 0.14f)),
                    )
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(text = memory.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(
                            text = memory.detail,
                            style = AuraTextStyles.caption,
                            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            text = memory.tag,
                            fontFamily = InterFontFamily,
                            fontSize = 10.5.sp,
                            color = AuraColors.TextPrimary.copy(alpha = 0.35f),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .semantics { contentDescription = "Forget this memory" }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = rememberRipple(color = AuraColors.TextPrimary, bounded = false),
                                    onClick = { onForget(memory.id) },
                                ).padding(4.dp),
                    ) {
                        CloseIcon(modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
