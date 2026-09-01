package com.aura.ai.presentation.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.core.designsystem.components.AuraGradientButton
import com.aura.ai.core.designsystem.components.AuraToggleSwitch
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.domain.model.AppPermission

private fun AppPermission.androidPermissionOrNull(): String? =
    when (this) {
        AppPermission.Microphone -> Manifest.permission.RECORD_AUDIO
        AppPermission.Notifications ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        AppPermission.Calendar -> Manifest.permission.READ_CALENDAR
        AppPermission.Location -> Manifest.permission.ACCESS_FINE_LOCATION
        AppPermission.Contacts -> Manifest.permission.READ_CONTACTS
    }

private val AppPermission.displayName: String
    get() =
        when (this) {
            AppPermission.Microphone -> "Microphone"
            AppPermission.Notifications -> "Notifications"
            AppPermission.Calendar -> "Calendar"
            AppPermission.Location -> "Location"
            AppPermission.Contacts -> "Contacts"
        }

private val AppPermission.description: String
    get() =
        when (this) {
            AppPermission.Microphone -> "Voice commands & live conversation"
            AppPermission.Notifications -> "Read & summarize incoming alerts"
            AppPermission.Calendar -> "Plan, reschedule, and brief your day"
            AppPermission.Location -> "Context-aware routines & reminders"
            AppPermission.Contacts -> "Draft messages & recognize callers"
        }

@Composable
fun PermissionsRoute(
    onFinished: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onFinished()
    }

    val context = LocalContext.current
    var pendingPermission by remember { mutableStateOf<AppPermission?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingPermission?.let { viewModel.setPermissionGranted(it, granted) }
            pendingPermission = null
        }

    val onToggle: (AppPermission) -> Unit = { permission ->
        val currentlyOn = uiState.granted[permission] == true
        if (currentlyOn) {
            viewModel.setPermissionGranted(permission, false)
        } else {
            val androidPermission = permission.androidPermissionOrNull()
            when {
                androidPermission == null -> viewModel.setPermissionGranted(permission, true)
                ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED ->
                    viewModel.setPermissionGranted(permission, true)
                else -> {
                    pendingPermission = permission
                    launcher.launch(androidPermission)
                }
            }
        }
    }

    PermissionsScreen(uiState = uiState, onToggle = onToggle, onActivate = viewModel::onActivate)
}

@Composable
private fun PermissionsScreen(
    uiState: PermissionsUiState,
    onToggle: (AppPermission) -> Unit,
    onActivate: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AuraColors.Background)
                .padding(horizontal = 22.dp)
                .padding(top = 88.dp, bottom = 40.dp),
    ) {
        Text(
            text = "SYSTEM ACCESS",
            style = AuraTextStyles.kicker,
            color = AuraColors.Accent,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Text(
            text = "Calibrate AURA",
            style = AuraTextStyles.headingXl,
            color = AuraColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "Grant only what you need — every permission is revocable in Settings at any time.",
            style = AuraTextStyles.bodyRelaxed,
            color = AuraColors.TextPrimary.copy(alpha = 0.55f),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(AppPermission.entries, key = { it.name }) { permission ->
                val isOn = uiState.granted[permission] == true
                PermissionRow(permission = permission, isOn = isOn, onToggle = { onToggle(permission) })
            }
        }

        AuraGradientButton(
            text = "Activate AURA",
            onClick = onActivate,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun PermissionRow(
    permission: AppPermission,
    isOn: Boolean,
    onToggle: () -> Unit,
) {
    val borderColor = if (isOn) AuraColors.Accent.copy(alpha = 0.4f) else AuraColors.TextPrimary.copy(alpha = 0.08f)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AuraColors.SurfaceGlass.copy(alpha = 0.55f))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuraColors.Accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            PermissionGlyph(permission = permission, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(text = permission.displayName, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
            Text(
                text = permission.description,
                style = AuraTextStyles.caption,
                color = AuraColors.TextPrimary.copy(alpha = 0.5f),
            )
        }
        AuraToggleSwitch(checked = isOn, onCheckedChange = onToggle, label = permission.displayName)
    }
}
