package com.aura.ai.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.core.designsystem.components.AuraToggleSwitch
import com.aura.ai.core.designsystem.components.ChevronRightIcon
import com.aura.ai.core.designsystem.components.CircularGauge
import com.aura.ai.core.designsystem.components.GlassSurface
import com.aura.ai.core.designsystem.components.SubTabChipRow
import com.aura.ai.core.designsystem.components.ThinProgressBar
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.InterFontFamily
import com.aura.ai.core.providers.ProviderId
import com.aura.ai.core.util.rememberBatteryState
import com.aura.ai.domain.model.AssistantVoice
import com.aura.ai.domain.model.ThemePreview

private val CONTENT_PADDING = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 140.dp)

@Composable
fun ProfileRoute(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) onSignOut()
    }
    ProfileScreen(
        uiState = uiState,
        onSetSub = viewModel::setSub,
        onSetTheme = viewModel::setThemePreview,
        onSetVoice = viewModel::setAssistantVoice,
        onSetVoiceContinuousConversationEnabled = viewModel::setVoiceContinuousConversationEnabled,
        onSignOut = viewModel::signOut,
        onSetProviderApiKey = viewModel::setProviderApiKey,
        onSetProviderBaseUrl = viewModel::setProviderBaseUrl,
        onSetActiveProvider = viewModel::setActiveProvider,
        onClearAllMemory = viewModel::clearAllMemory,
    )
}

@Composable
private fun ProfileScreen(
    uiState: ProfileUiState,
    onSetSub: (ProfileSub) -> Unit,
    onSetTheme: (ThemePreview) -> Unit,
    onSetVoice: (AssistantVoice) -> Unit,
    onSetVoiceContinuousConversationEnabled: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onSetProviderApiKey: (ProviderId, String) -> Unit,
    onSetProviderBaseUrl: (ProviderId, String) -> Unit,
    onSetActiveProvider: (ProviderId) -> Unit,
    onClearAllMemory: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTabChipRow(items = ProfileSub.entries, selected = uiState.sub, label = { it.label }, onSelect = onSetSub)
        Box(modifier = Modifier.weight(1f)) {
            when (uiState.sub) {
                ProfileSub.Profile ->
                    ProfileMainView(
                        onNavigateSettings = { onSetSub(ProfileSub.Settings) },
                        onNavigateAbout = { onSetSub(ProfileSub.About) },
                        onSignOut = onSignOut,
                    )
                ProfileSub.Settings ->
                    SettingsView(
                        providerRows = uiState.providerRows,
                        onSetProviderApiKey = onSetProviderApiKey,
                        onSetProviderBaseUrl = onSetProviderBaseUrl,
                        onSetActiveProvider = onSetActiveProvider,
                    )
                ProfileSub.Personalization ->
                    PersonalizationView(
                        theme = uiState.themePreview,
                        voice = uiState.assistantVoice,
                        voiceContinuousConversationEnabled = uiState.voiceContinuousConversationEnabled,
                        onSetTheme = onSetTheme,
                        onSetVoice = onSetVoice,
                        onSetVoiceContinuousConversationEnabled = onSetVoiceContinuousConversationEnabled,
                    )
                ProfileSub.MemoryManager -> MemoryManagerView(onClearAllMemory = onClearAllMemory)
                ProfileSub.Analytics -> AnalyticsView()
                ProfileSub.DeviceHealth -> DeviceHealthView()
                ProfileSub.Battery -> BatteryView()
                ProfileSub.Storage -> StorageView()
                ProfileSub.About -> AboutView()
            }
        }
    }
}

@Composable
private fun ProfileMainView(
    onNavigateSettings: () -> Unit,
    onNavigateAbout: () -> Unit,
    onSignOut: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(AuraColors.AccentLight, AuraColors.Accent, AuraColors.AccentDeep))),
                )
                Text(
                    text = "Operator",
                    style = AuraTextStyles.headingLg,
                    color = AuraColors.TextPrimary,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = "operator@aura.ai · Linked since Mar 2026",
                    style = AuraTextStyles.caption,
                    color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                ProfileSampleContent.stats.forEach { stat ->
                    GlassSurface(modifier = Modifier.weight(1f)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp),
                        ) {
                            Text(
                                text = stat.value,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                color = AuraColors.Accent,
                            )
                            Text(
                                text = stat.label,
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                color = AuraColors.TextPrimary.copy(alpha = 0.45f),
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
        }
        items(ProfileSampleContent.profileLinks, key = { it }) { link ->
            GlassSurface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onClick = {
                    when (link) {
                        "Sign Out" -> onSignOut()
                        "Privacy & Data" -> onNavigateAbout()
                        else -> onNavigateSettings()
                    }
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = link,
                        style = AuraTextStyles.bodySmall,
                        color = if (link == "Sign Out") AuraColors.Danger else AuraColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    ChevronRightIcon(modifier = Modifier.size(14.dp), tint = AuraColors.TextPrimary.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun SettingsView(
    providerRows: List<ProviderSettingsRow>,
    onSetProviderApiKey: (ProviderId, String) -> Unit,
    onSetProviderBaseUrl: (ProviderId, String) -> Unit,
    onSetActiveProvider: (ProviderId) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING) {
        item {
            Text(
                text = "Settings",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
        }
        item {
            Text(
                text = "AI PROVIDERS",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
            )
        }
        items(providerRows, key = { it.id }) { row ->
            ProviderSettingsCard(
                row = row,
                onSetApiKey = { key -> onSetProviderApiKey(row.id, key) },
                onSetBaseUrl = { url -> onSetProviderBaseUrl(row.id, url) },
                onSetActive = { onSetActiveProvider(row.id) },
            )
        }
        item {
            Text(
                text = "AUTOMATION",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
            )
        }
        item { AccessibilityDisclosureCard() }
        ProfileSampleContent.settingsGroups.forEach { group ->
            item {
                Text(
                    text = group.label.uppercase(),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                )
            }
            items(group.rows, key = { it.label }) { row ->
                GlassSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), cornerRadius = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = row.label, style = AuraTextStyles.cardTitle.copy(fontSize = 13.sp), color = AuraColors.TextPrimary)
                        Text(text = row.value, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

/** One provider's card in Settings → AI Providers. [ProviderId.LocalModel] gets no input at all —
 *  it's still fully scaffolded (see docs/TODO_V1.md §3, High-priority item 2.11), so showing a key
 *  field for it would be dishonest UI. [ProviderId.Ollama] gets a base-URL field instead of a key
 *  field (self-hosted, no credential). Every other provider gets a masked API-key field. */
@Composable
private fun ProviderSettingsCard(
    row: ProviderSettingsRow,
    onSetApiKey: (String) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetActive: () -> Unit,
) {
    var keyInput by remember(row.id) { mutableStateOf("") }
    var urlInput by remember(row.id) { mutableStateOf(row.baseUrl.orEmpty()) }
    GlassSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), cornerRadius = 14.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = row.displayName, style = AuraTextStyles.cardTitle.copy(fontSize = 13.sp), color = AuraColors.TextPrimary)
                    Text(
                        text = if (row.connected) "Connected" else "Not connected",
                        style = AuraTextStyles.caption,
                        color = if (row.connected) AuraColors.Success else AuraColors.TextPrimary.copy(alpha = 0.45f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onSetActive)
                            .background(if (row.isActive) AuraColors.Accent.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = if (row.isActive) "ACTIVE" else "SET ACTIVE",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = if (row.isActive) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.45f),
                    )
                }
            }
            if (row.id == ProviderId.LocalModel) {
                Text(
                    text = "On-device inference isn't wired up yet — see the roadmap.",
                    style = AuraTextStyles.caption,
                    color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuraColors.SurfaceGlass.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        if (row.id == ProviderId.Ollama) {
                            BasicTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, color = AuraColors.TextPrimary),
                                cursorBrush = SolidColor(AuraColors.Accent),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (urlInput.isEmpty()) {
                                        Text(
                                            text = "http://10.0.2.2:11434",
                                            style =
                                                TextStyle(
                                                    fontFamily = InterFontFamily,
                                                    fontSize = 13.sp,
                                                    color = AuraColors.TextPrimary.copy(alpha = 0.35f),
                                                ),
                                        )
                                    }
                                    inner()
                                },
                            )
                        } else {
                            BasicTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it },
                                textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, color = AuraColors.TextPrimary),
                                cursorBrush = SolidColor(AuraColors.Accent),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (keyInput.isEmpty()) {
                                        Text(
                                            text = if (row.connected) "•••••••••••••••• (key set)" else "Paste API key",
                                            style =
                                                TextStyle(
                                                    fontFamily = InterFontFamily,
                                                    fontSize = 13.sp,
                                                    color = AuraColors.TextPrimary.copy(alpha = 0.35f),
                                                ),
                                        )
                                    }
                                    inner()
                                },
                            )
                        }
                    }
                    Text(
                        text = "SAVE",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = AuraColors.Accent,
                        modifier =
                            Modifier
                                .padding(start = 12.dp)
                                .clickable {
                                    if (row.id == ProviderId.Ollama) {
                                        onSetBaseUrl(urlInput)
                                    } else {
                                        onSetApiKey(keyInput)
                                        keyInput = ""
                                    }
                                },
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalizationView(
    theme: ThemePreview,
    voice: AssistantVoice,
    voiceContinuousConversationEnabled: Boolean,
    onSetTheme: (ThemePreview) -> Unit,
    onSetVoice: (AssistantVoice) -> Unit,
    onSetVoiceContinuousConversationEnabled: (Boolean) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING) {
        item {
            Text(
                text = "Personalization",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "Tune how AURA looks and sounds.",
                style = AuraTextStyles.caption,
                color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            SmallHeader("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                ThemeSwatch(
                    label = "Dark HUD",
                    selected = theme == ThemePreview.Dark,
                    gradient = Brush.linearGradient(listOf(Color(0xFF171B33), Color(0xFF05060B))),
                    onClick = { onSetTheme(ThemePreview.Dark) },
                    modifier = Modifier.weight(1f),
                )
                ThemeSwatch(
                    label = "Light HUD",
                    selected = theme == ThemePreview.Light,
                    gradient = Brush.linearGradient(listOf(Color(0xFFF3F5FE), Color(0xFFCFD3E5))),
                    onClick = { onSetTheme(ThemePreview.Light) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (theme == ThemePreview.Light) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF3F5FE))
                            .padding(16.dp),
                ) {
                    Column {
                        Text(
                            text = "Preview — Home, Light HUD",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF2B2741),
                        )
                        Row(
                            modifier =
                                Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE4E7F5))
                                    .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(AuraColors.Accent, Color(0xFF5D5294)))),
                            )
                            Text(
                                text = "Ask AURA anything",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF2B2741),
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }
            }
            SmallHeader("Voice")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ProfileSampleContent.voices.forEach { label ->
                    val selected = voice.name.equals(label, ignoreCase = true)
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) AuraColors.Accent.copy(alpha = 0.16f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (selected) AuraColors.Accent.copy(alpha = 0.4f) else AuraColors.TextPrimary.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp),
                                ).clickable { onSetVoice(AssistantVoice.entries.first { it.name.equals(label, ignoreCase = true) }) }
                                .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (selected) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AuraColors.SurfaceGlass.copy(alpha = 0.55f))
                        .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {
                    Text(text = "Conversation continuation", style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                    Text(
                        text = "After AURA finishes speaking, keep listening for a follow-up.",
                        style = AuraTextStyles.caption,
                        color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                    )
                }
                AuraToggleSwitch(
                    checked = voiceContinuousConversationEnabled,
                    onCheckedChange = { onSetVoiceContinuousConversationEnabled(!voiceContinuousConversationEnabled) },
                    label = "Conversation continuation",
                )
            }
        }
    }
}

@Composable
private fun SmallHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        color = AuraColors.TextPrimary.copy(alpha = 0.4f),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ThemeSwatch(
    label: String,
    selected: Boolean,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0B12))
                .border(2.dp, if (selected) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(gradient),
        )
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            color = AuraColors.TextPrimary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MemoryManagerView(onClearAllMemory: () -> Unit) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear all memory?") },
            text = { Text("This permanently deletes every stored memory. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    onClearAllMemory()
                }) {
                    Text("Clear", color = AuraColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                text = "Memory Manager",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "Storage and controls for AURA's memory graph.",
                style = AuraTextStyles.caption,
                color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Memory used", style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.6f))
                        Text(text = "48 of 500 entries", style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.6f))
                    }
                    ThinProgressBar(progress = 0.096f, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        items(ProfileSampleContent.memoryActions, key = { it.label }) { action ->
            GlassSurface(
                modifier =
                    Modifier.fillMaxWidth().let {
                        if (action.danger) {
                            it.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showClearConfirmation = true
                            }
                        } else {
                            it
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(text = action.label, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(text = action.description, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.45f))
                    }
                    Text(
                        text = action.action,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (action.danger) AuraColors.Danger else AuraColors.TextPrimary.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING) {
        item {
            Text(
                text = "Analytics Dashboard",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                GlassSurface(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Focus time", style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
                        Text(
                            text = "5.2h/day",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = AuraColors.TextPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                GlassSurface(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Tasks closed", style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
                        Text(
                            text = "86%",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = AuraColors.TextPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly AURA interactions",
                        style = AuraTextStyles.caption,
                        color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        ProfileSampleContent.weekBars.forEach { bar ->
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height((80 * bar.fraction).dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(Brush.verticalGradient(listOf(AuraColors.AccentLight, AuraColors.Accent))),
                                )
                                Text(
                                    text = bar.day,
                                    fontFamily = InterFontFamily,
                                    fontSize = 9.5.sp,
                                    color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceHealthView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING) {
        item {
            Text(
                text = "Device Health",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
        }
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                CircularGauge(progress = 0.92f, activeColor = AuraColors.Success) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "92",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            color = AuraColors.Success,
                        )
                        Text(
                            text = "HEALTH SCORE",
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                            color = AuraColors.TextPrimary.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }
        items(ProfileSampleContent.healthRows, key = { it.label }) { row ->
            GlassSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = row.label, style = AuraTextStyles.cardTitle.copy(fontSize = 13.sp), color = AuraColors.TextPrimary)
                    Text(
                        text = row.value,
                        style = AuraTextStyles.caption,
                        color = if (row.positive) AuraColors.Success else AuraColors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryView() {
    val battery = rememberBatteryState()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING) {
        item {
            Text(
                text = "Battery Monitor",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .width(54.dp)
                                .height(96.dp)
                                .border(2.dp, AuraColors.TextPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp)
                                    .height((92 * (battery.percent / 100f)).dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Brush.verticalGradient(listOf(AuraColors.AccentLight, AuraColors.Accent))),
                        )
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = "${battery.percent}%",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 30.sp,
                            color = AuraColors.TextPrimary,
                        )
                        Text(
                            text = if (battery.isCharging) "Charging" else "Not charging",
                            style = AuraTextStyles.caption,
                            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
        items(ProfileSampleContent.batteryApps, key = { it.name }) { app ->
            GlassSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = app.name, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary, modifier = Modifier.weight(1f))
                    Box(
                        modifier =
                            Modifier
                                .width(
                                    60.dp,
                                ).height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AuraColors.TextPrimary.copy(alpha = 0.1f)),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(
                                        app.percent / 100f,
                                    ).height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AuraColors.Accent),
                        )
                    }
                    Text(
                        text = "${app.percent}%",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = AuraColors.TextPrimary.copy(alpha = 0.45f),
                        modifier = Modifier.width(36.dp).padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING) {
        item {
            Text(
                text = "Storage Manager",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
        }
        item {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .padding(bottom = 16.dp),
            ) {
                ProfileSampleContent.storageCategories.forEach { category ->
                    Box(modifier = Modifier.weight(category.fraction).fillMaxSize().background(categoryColor(category)))
                }
            }
        }
        items(ProfileSampleContent.storageCategories, key = { it.name }) { category ->
            GlassSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(categoryColor(category)))
                    Text(
                        text = category.name,
                        style = AuraTextStyles.bodySmall,
                        color = AuraColors.TextPrimary,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                    Text(text = category.size, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private fun categoryColor(category: StorageCategory): Color =
    when (category.name) {
        "Photos & Media" -> AuraColors.Accent
        "AURA Memory & Cache" -> AuraColors.AccentButtonEnd
        "Apps" -> AuraColors.AccentDeep
        "Documents" -> AuraColors.Accent2
        else -> AuraColors.TextPrimary.copy(alpha = 0.12f)
    }

/** Real version info ([BuildConfig]) and a condensed, accurate privacy summary — not sample
 *  content, unlike most of this screen's other sub-tabs. The full text lives in
 *  `docs/PRIVACY_POLICY.md` and `docs/THIRD_PARTY_LICENSES.md`; this is what a user can actually
 *  reach without leaving the app, kept honest by staying in sync with what `docs/SECURITY.md`'s
 *  review actually found true. */
@Composable
private fun AboutView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "About AURA",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Version", style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
                    Text(
                        text = "${com.aura.ai.BuildConfig.VERSION_NAME} (build ${com.aura.ai.BuildConfig.VERSION_CODE})",
                        style = AuraTextStyles.bodySmall,
                        color = AuraColors.TextPrimary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Privacy", style = AuraTextStyles.cardTitle.copy(fontSize = 13.sp), color = AuraColors.TextPrimary)
                    Text(
                        text =
                            "Conversations, memories, and workflows stay on this device in AURA's own local " +
                                "database. AI provider API keys are stored encrypted (Android Keystore-backed) and " +
                                "excluded from device backup/transfer. Nothing is sent anywhere except to the AI " +
                                "provider you connect and configure yourself — AURA has no analytics or crash " +
                                "reporting of its own. Memory Manager (in this Profile tab) can permanently delete " +
                                "all stored memory. Full policy: docs/PRIVACY_POLICY.md in the project repository.",
                        style = AuraTextStyles.caption,
                        color = AuraColors.TextPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Open-source licenses", style = AuraTextStyles.cardTitle.copy(fontSize = 13.sp), color = AuraColors.TextPrimary)
                    Text(
                        text =
                            "AURA is built on open-source libraries including Jetpack Compose, Hilt, Room, " +
                                "Kotlin Coroutines, and OkHttp. Full list with license terms: " +
                                "docs/THIRD_PARTY_LICENSES.md in the project repository.",
                        style = AuraTextStyles.caption,
                        color = AuraColors.TextPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/** The explicit disclosure required before sending anyone to system Accessibility settings — says
 *  plainly what AURA's service does and doesn't do (`docs/ANDROID_AUTOMATION.md` §4) before the
 *  user leaves the app to enable it; enabling it is never requestable via a normal permission
 *  dialog, so this card + system settings is the entire consent flow. */
@Composable
private fun AccessibilityDisclosureCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    GlassSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), cornerRadius = 12.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "Accessibility Service", style = AuraTextStyles.cardTitle.copy(fontSize = 13.sp), color = AuraColors.TextPrimary)
            Text(
                text =
                    "Lets AURA read visible screen text when you ask, and press back/home/recents. " +
                        "AURA never simulates taps or typing in other apps. Off by default — enabling it opens system Settings.",
                style = AuraTextStyles.caption,
                color = AuraColors.TextPrimary.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, AuraColors.Accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable {
                            context.startActivity(
                                android.content
                                    .Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(text = "Open Accessibility Settings", style = AuraTextStyles.caption, color = AuraColors.Accent)
            }
        }
    }
}
