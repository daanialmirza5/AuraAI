@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.presentation.home

import androidx.compose.foundation.background
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.core.designsystem.components.BellIcon
import com.aura.ai.core.designsystem.components.ChevronLeftIcon
import com.aura.ai.core.designsystem.components.ChevronRightIcon
import com.aura.ai.core.designsystem.components.GlassBackButton
import com.aura.ai.core.designsystem.components.GlassIconButton
import com.aura.ai.core.designsystem.components.GlassSurface
import com.aura.ai.core.designsystem.components.SectionLabel
import com.aura.ai.core.designsystem.components.StatTile
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.InterFontFamily
import com.aura.ai.core.designsystem.theme.rememberPulse
import com.aura.ai.core.navigation.AppTabRoute
import com.aura.ai.core.util.rememberBatteryState

@Composable
fun HomeRoute(
    onNavigateToTab: (String, String?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onOpenNotifications = { viewModel.setSub(HomeSub.Notifications) },
        onBackToDashboard = { viewModel.setSub(HomeSub.Dashboard) },
        onOpenAura = { onNavigateToTab(AppTabRoute.AURA, "orb") },
        onOpenDeviceHealth = { onNavigateToTab(AppTabRoute.PROFILE, "devicehealth") },
        onOpenBattery = { onNavigateToTab(AppTabRoute.PROFILE, "battery") },
        onOpenSmartHome = { onNavigateToTab(AppTabRoute.AUTOMATE, "smarthome") },
        onOpenAutomations = { onNavigateToTab(AppTabRoute.AUTOMATE, "automation") },
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onOpenNotifications: () -> Unit,
    onBackToDashboard: () -> Unit,
    onOpenAura: () -> Unit,
    onOpenDeviceHealth: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenSmartHome: () -> Unit,
    onOpenAutomations: () -> Unit,
) {
    when (uiState.sub) {
        HomeSub.Dashboard ->
            DashboardContent(
                uiState = uiState,
                onOpenNotifications = onOpenNotifications,
                onOpenAura = onOpenAura,
                onOpenDeviceHealth = onOpenDeviceHealth,
                onOpenBattery = onOpenBattery,
                onOpenSmartHome = onOpenSmartHome,
                onOpenAutomations = onOpenAutomations,
            )
        HomeSub.Notifications -> NotificationsContent(uiState = uiState, onBack = onBackToDashboard)
    }
}

@Composable
private fun DashboardContent(
    uiState: HomeUiState,
    onOpenNotifications: () -> Unit,
    onOpenAura: () -> Unit,
    onOpenDeviceHealth: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenSmartHome: () -> Unit,
    onOpenAutomations: () -> Unit,
) {
    val battery = rememberBatteryState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "GOOD EVENING",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.14f.em,
                        color = AuraColors.TextPrimary.copy(alpha = 0.45f),
                    )
                    Text(
                        text = "Operator",
                        style = AuraTextStyles.headingLg,
                        color = AuraColors.TextPrimary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box {
                    GlassIconButton(onClick = onOpenNotifications, contentDescription = "Notifications") {
                        BellIcon(modifier = Modifier.size(18.dp), tint = AuraColors.TextPrimary)
                    }
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 9.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AuraColors.Accent),
                    )
                }
            }
        }

        item {
            val interactionSource = remember { MutableInteractionSource() }
            val (scaleState, _) = rememberPulse(3000)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(AuraColors.AccentDeep.copy(alpha = 0.5f), AuraColors.SurfaceGlass.copy(alpha = 0.5f)),
                            ),
                        ).clickable(
                            interactionSource = interactionSource,
                            indication = rememberRipple(color = AuraColors.Accent),
                            onClick = onOpenAura,
                        ).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .scale(scaleState.value)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(AuraColors.AccentLight, AuraColors.Accent, AuraColors.AccentDeep)),
                            ),
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(text = "Ask AURA anything", style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                    Text(
                        text = "Tap to talk, or type in Chat",
                        style = AuraTextStyles.caption,
                        color = AuraColors.TextPrimary.copy(alpha = 0.55f),
                    )
                }
                ChevronRightIcon(modifier = Modifier.size(16.dp))
            }
        }

        item { SectionLabel(text = "Daily Timeline") }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                    uiState.timeline.forEachIndexed { index, entry ->
                        TimelineRow(entry = entry, isLast = index == uiState.timeline.lastIndex)
                    }
                }
            }
        }

        item { SectionLabel(text = "Systems", modifier = Modifier.padding(top = 4.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Device Health",
                        value = "Optimal",
                        valueColor = AuraColors.Success,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDeviceHealth,
                    )
                    StatTile(
                        label = "Battery",
                        value = "${battery.percent}%",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenBattery,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Smart Home",
                        value = "${uiState.onlineDeviceCount} online",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSmartHome,
                    )
                    StatTile(
                        label = "Automations",
                        value = "${uiState.activeAutomationCount} active",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAutomations,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(
    entry: TimelineItem,
    isLast: Boolean,
) {
    val dotColor = if (entry.dot == TimelineDot.Accent) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.3f)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
            text = entry.time,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = AuraColors.TextPrimary.copy(alpha = 0.4f),
            modifier = Modifier.width(52.dp).padding(top = 2.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
            Box(
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
            )
            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 4.dp)
                            .width(1.dp)
                            .height(28.dp)
                            .background(AuraColors.TextPrimary.copy(alpha = 0.12f)),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)) {
            Text(text = entry.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
            Text(text = entry.subtitle, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun NotificationsContent(
    uiState: HomeUiState,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(onClick = onBack) { ChevronLeftIcon(modifier = Modifier.size(15.dp)) }
                Text(
                    text = "Notification Summary",
                    style = AuraTextStyles.headingMd,
                    color = AuraColors.TextPrimary,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
        item {
            Text(
                text = "AURA condensed 14 alerts from the last 3 hours into 4 things worth your attention.",
                style = AuraTextStyles.body,
                color = AuraColors.TextPrimary.copy(alpha = 0.55f),
            )
        }
        items(uiState.notifications, key = { it.app }) { notification ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuraColors.Accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = notification.count.toString(),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = AuraColors.Accent,
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = notification.app, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(
                            text = notification.summary,
                            style = AuraTextStyles.caption,
                            color = AuraColors.TextPrimary.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
