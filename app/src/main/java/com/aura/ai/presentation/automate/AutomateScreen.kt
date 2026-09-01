package com.aura.ai.presentation.automate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.core.designsystem.components.AuraToggleSwitch
import com.aura.ai.core.designsystem.components.GlassSurface
import com.aura.ai.core.designsystem.components.SubTabChipRow
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.InterFontFamily
import com.aura.ai.domain.model.Automation
import com.aura.ai.domain.model.Device
import com.aura.ai.domain.model.Workflow
import com.aura.ai.domain.model.WorkflowPresets
import com.aura.ai.domain.model.WorkflowTrigger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import java.io.File

private val CONTENT_PADDING = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 140.dp)

@Composable
fun AutomateRoute(viewModel: AutomateViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The camera app needs somewhere to write the photo before it can hand control back — a
    // fresh file + FileProvider URI is created right before each capture, not held across
    // recompositions, so a stale capture can never be reused by accident.
    var pendingCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val captureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = pendingCaptureUri
            pendingCaptureUri = null
            if (success && uri != null) {
                viewModel.runQuickAction("ocr", mapOf("imageUri" to uri.toString()))
            }
        }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val uri = context.createCaptureUri()
                pendingCaptureUri = uri
                captureLauncher.launch(uri)
            }
        }
    val onScanText: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = context.createCaptureUri()
            pendingCaptureUri = uri
            captureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AutomateScreen(
        uiState = uiState,
        onSetSub = viewModel::setSub,
        onToggleAutomation = viewModel::toggleAutomation,
        onToggleDevice = viewModel::toggleDevice,
        onRunQuickAction = viewModel::runQuickAction,
        onDismissQuickActionResult = viewModel::dismissQuickActionResult,
        onScanText = onScanText,
        onAddWorkflow = viewModel::addWorkflow,
        onSetWorkflowEnabled = viewModel::setWorkflowEnabled,
        onDeleteWorkflow = viewModel::deleteWorkflow,
        onRunWorkflowNow = viewModel::runWorkflowNow,
    )
}

private fun android.content.Context.createCaptureUri(): android.net.Uri {
    val dir = File(cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

@Composable
private fun AutomateScreen(
    uiState: AutomateUiState,
    onSetSub: (AutomateSub) -> Unit,
    onToggleAutomation: (String) -> Unit,
    onToggleDevice: (String) -> Unit,
    onRunQuickAction: (String, Map<String, String>) -> Unit,
    onDismissQuickActionResult: () -> Unit,
    onScanText: () -> Unit,
    onAddWorkflow: (Workflow) -> Unit,
    onSetWorkflowEnabled: (Workflow, Boolean) -> Unit,
    onDeleteWorkflow: (Workflow) -> Unit,
    onRunWorkflowNow: (Workflow) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubTabChipRow(items = AutomateSub.entries, selected = uiState.sub, label = { it.label }, onSelect = onSetSub)
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.sub) {
                    AutomateSub.Automation -> AutomationsView(uiState.automations, onToggleAutomation, onRunQuickAction, onScanText)
                    AutomateSub.Workflows ->
                        WorkflowsView(
                            uiState.workflows,
                            onAddWorkflow,
                            onSetWorkflowEnabled,
                            onDeleteWorkflow,
                            onRunWorkflowNow,
                        )
                    AutomateSub.SmartHome -> SmartHomeView(uiState.devices, onToggleDevice)
                    AutomateSub.Plugins -> PluginsView()
                    AutomateSub.Marketplace -> MarketplaceView()
                }
            }
        }
        uiState.quickActionResult?.let { result ->
            QuickActionResultBanner(message = result, onDismiss = onDismissQuickActionResult)
        }
    }
}

@Composable
private fun AutomationsView(
    automations: ImmutableList<Automation>,
    onToggle: (String) -> Unit,
    onRunQuickAction: (String, Map<String, String>) -> Unit,
    onScanText: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "Automation Center",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "Routines AURA runs on your Android system without asking twice.",
                style = AuraTextStyles.caption,
                color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
        item {
            Text(
                text = "QUICK ACTIONS",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            QuickActionsRow(onRunQuickAction)
            VisionQuickAction(onScanText)
        }
        items(automations, key = { it.id }) { automation ->
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (automation.isOn) AuraColors.Accent.copy(alpha = 0.3f) else AuraColors.TextPrimary.copy(alpha = 0.06f),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = automation.name,
                            style = AuraTextStyles.cardTitle.copy(fontSize = 13.5.sp),
                            color = AuraColors.TextPrimary,
                        )
                        Text(
                            text = automation.description,
                            style = AuraTextStyles.caption,
                            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    AuraToggleSwitch(
                        checked = automation.isOn,
                        onCheckedChange = { onToggle(automation.id) },
                        label = automation.name,
                    )
                }
            }
        }
        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, AuraColors.Accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+ New Automation", style = AuraTextStyles.bodySmall, color = AuraColors.Accent)
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val toolName: String,
    val arguments: Map<String, String> = emptyMap(),
)

private val QUICK_ACTIONS =
    listOf(
        QuickAction("Read Screen", "read_screen"),
        QuickAction("Play/Pause", "media_control", mapOf("action" to "play_pause")),
        QuickAction("Wi-Fi Settings", "open_settings", mapOf("screen" to "wifi")),
    )

/** Direct, tap-triggered tool execution — the one real call site for
 *  [com.aura.ai.core.actions.ActionEngine.executeConfirmed] in this build, including "Read
 *  Screen" ([com.aura.ai.ai.ReadScreenTool], `requiresConfirmation = true`): the tap here *is*
 *  the confirmation. See `docs/ANDROID_AUTOMATION.md` §3. */
@Composable
private fun QuickActionsRow(onRun: (String, Map<String, String>) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        QUICK_ACTIONS.forEach { action ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraColors.SurfaceGlass.copy(alpha = 0.55f))
                        .border(1.dp, AuraColors.TextPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { onRun(action.toolName, action.arguments) }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = action.label,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = AuraColors.TextPrimary,
                )
            }
        }
    }
}

/** Camera capture (Vision Runtime, `docs/VISION_RUNTIME.md`) is its own button, not folded into
 *  [QuickActionsRow]'s data-driven list — it needs to launch the system camera app first and only
 *  then run a tool with the result, unlike every other quick action's direct, immediate tap. */
@Composable
private fun VisionQuickAction(onScanText: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AuraColors.Accent.copy(alpha = 0.12f))
                .border(1.dp, AuraColors.Accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable(onClick = onScanText)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Scan Text (Camera)",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = AuraColors.Accent,
        )
    }
}

@Composable
private fun BoxScope.QuickActionResultBanner(
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
                .clickable { onDismiss() },
    ) {
        Text(text = message, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary, modifier = Modifier.padding(14.dp))
    }
}

/** "Reusable workflows" (`docs/WORKFLOW_ENGINE.md`) — the five named brief examples as one-tap
 *  presets (built from real, existing tools; see `WorkflowPresets`), plus whatever the user has
 *  actually added: trigger summary, enable/disable, run-now, delete. No custom step-by-step
 *  builder UI — editing today means presets plus enable/disable/delete, not composing arbitrary
 *  new step sequences from this screen. */
@Composable
private fun WorkflowsView(
    workflows: ImmutableList<Workflow>,
    onAdd: (Workflow) -> Unit,
    onSetEnabled: (Workflow, Boolean) -> Unit,
    onDelete: (Workflow) -> Unit,
    onRunNow: (Workflow) -> Unit,
) {
    val addedNames = workflows.map { it.name }.toSet()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "Workflows",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "Reusable routines — triggered on a schedule or run on demand.",
                style = AuraTextStyles.caption,
                color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
        val availablePresets = WorkflowPresets.all().filterNot { it.name in addedNames }
        if (availablePresets.isNotEmpty()) {
            item {
                Text(
                    text = "STARTER WORKFLOWS",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(availablePresets, key = { "preset-${it.name}" }) { preset ->
                GlassSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), cornerRadius = 12.dp) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = AuraTextStyles.cardTitle.copy(fontSize = 13.5.sp),
                                color = AuraColors.TextPrimary,
                            )
                            Text(
                                text = preset.description,
                                style = AuraTextStyles.caption,
                                color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, AuraColors.Accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { onAdd(preset) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(text = "Add", style = AuraTextStyles.caption, color = AuraColors.Accent)
                        }
                    }
                }
            }
        }
        if (workflows.isNotEmpty()) {
            item {
                Text(
                    text = "YOUR WORKFLOWS",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                )
            }
            items(workflows, key = { it.id }) { workflow ->
                GlassSurface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    cornerRadius = 12.dp,
                    borderColor =
                        if (workflow.enabled) {
                            AuraColors.Accent.copy(
                                alpha = 0.3f,
                            )
                        } else {
                            AuraColors.TextPrimary.copy(alpha = 0.06f)
                        },
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = workflow.name,
                                    style = AuraTextStyles.cardTitle.copy(fontSize = 13.5.sp),
                                    color = AuraColors.TextPrimary,
                                )
                                Text(
                                    text = workflow.triggerSummary(),
                                    style = AuraTextStyles.caption,
                                    color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                                )
                            }
                            AuraToggleSwitch(
                                checked = workflow.enabled,
                                onCheckedChange = { onSetEnabled(workflow, !workflow.enabled) },
                                label = "${workflow.name} enabled",
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AuraColors.Accent.copy(alpha = 0.14f))
                                        .clickable { onRunNow(workflow) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(text = "Run Now", style = AuraTextStyles.caption, color = AuraColors.Accent)
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, AuraColors.TextPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .clickable { onDelete(workflow) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(text = "Delete", style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Workflow.triggerSummary(): String =
    when (trigger) {
        WorkflowTrigger.Manual -> "Runs on demand"
        WorkflowTrigger.Daily -> "Daily at %02d:%02d".format(triggerHour, triggerMinute)
        WorkflowTrigger.Weekly -> "Weekly, day %d at %02d:%02d".format(triggerDayOfWeek, triggerHour, triggerMinute)
    }

@Composable
private fun SmartHomeView(
    devices: ImmutableList<Device>,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text(
            text = "Smart Home Dashboard",
            style = AuraTextStyles.headingMd,
            color = AuraColors.TextPrimary,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "${devices.size} devices linked · Away Mode ready",
            style = AuraTextStyles.caption,
            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(devices, key = { it.id }) { device ->
                val bg = if (device.isOn) AuraColors.Accent.copy(alpha = 0.1f) else AuraColors.SurfaceGlass.copy(alpha = 0.5f)
                val border = if (device.isOn) AuraColors.Accent.copy(alpha = 0.3f) else AuraColors.TextPrimary.copy(alpha = 0.06f)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .border(1.dp, border, RoundedCornerShape(16.dp))
                            .clickable { onToggle(device.id) }
                            .padding(14.dp),
                ) {
                    Column {
                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(AuraColors.Accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            DeviceGlyph(icon = device.icon, modifier = Modifier.size(15.dp))
                        }
                        Text(
                            text = device.name,
                            style = AuraTextStyles.caption.copy(fontSize = 12.5.sp),
                            color = AuraColors.TextPrimary,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Text(
                            text = device.meta,
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = if (device.isOn) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.45f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginsView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                text = "Plugins",
                style = AuraTextStyles.headingMd,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "Extend AURA with connected services.",
                style = AuraTextStyles.caption,
                color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
        items(AutomateSampleContent.plugins, key = { it.name }) { plugin ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(plugin.status.dotColor()),
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = plugin.name, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(text = plugin.description, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.45f))
                    }
                    Text(
                        text = plugin.status.label(),
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp,
                        color = plugin.status.textColor(),
                    )
                }
            }
        }
    }
}

private fun PluginStatus.dotColor(): Color =
    when (this) {
        PluginStatus.Active -> AuraColors.Success
        PluginStatus.Limited -> AuraColors.Warning
        PluginStatus.Connect -> AuraColors.TextPrimary.copy(alpha = 0.25f)
    }

private fun PluginStatus.textColor(): Color =
    when (this) {
        PluginStatus.Active -> AuraColors.Success
        PluginStatus.Limited -> AuraColors.Warning
        PluginStatus.Connect -> AuraColors.TextPrimary.copy(alpha = 0.4f)
    }

private fun PluginStatus.label(): String =
    when (this) {
        PluginStatus.Active -> "ACTIVE"
        PluginStatus.Limited -> "LIMITED"
        PluginStatus.Connect -> "CONNECT"
    }

@Composable
private fun MarketplaceView() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text(
            text = "AI Marketplace",
            style = AuraTextStyles.headingMd,
            color = AuraColors.TextPrimary,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "Skills and personas built by the AURA community.",
            style = AuraTextStyles.caption,
            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(AutomateSampleContent.marketplace, key = { it.name }) { skill ->
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = skill.name, style = AuraTextStyles.caption.copy(fontSize = 12.5.sp), color = AuraColors.TextPrimary)
                        Text(
                            text = skill.description,
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(9.dp))
                                    .border(1.dp, AuraColors.Accent.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
                                    .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Install",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = AuraColors.Accent,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
