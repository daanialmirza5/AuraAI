@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.presentation.workspace

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.core.designsystem.components.CheckIcon
import com.aura.ai.core.designsystem.components.GlassSurface
import com.aura.ai.core.designsystem.components.StatTile
import com.aura.ai.core.designsystem.components.SubTabChipRow
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.InterFontFamily
import com.aura.ai.domain.model.Todo
import com.aura.ai.domain.model.TodoPriority
import kotlinx.collections.immutable.ImmutableList

private val CONTENT_PADDING = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 140.dp)

private fun DotTone.color(): Color =
    when (this) {
        DotTone.Accent -> AuraColors.Accent
        DotTone.Neutral -> AuraColors.TextPrimary.copy(alpha = 0.3f)
        DotTone.Warning -> AuraColors.Warning
    }

@Composable
fun WorkspaceRoute(viewModel: WorkspaceViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkspaceScreen(uiState = uiState, onSetSub = viewModel::setSub, onToggleTodo = viewModel::toggleTodo)
}

@Composable
private fun WorkspaceScreen(
    uiState: WorkspaceUiState,
    onSetSub: (WorkspaceSub) -> Unit,
    onToggleTodo: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTabChipRow(items = WorkspaceSub.entries, selected = uiState.sub, label = { it.label }, onSelect = onSetSub)
        Box(modifier = Modifier.weight(1f)) {
            when (uiState.sub) {
                WorkspaceSub.Timeline -> TimelineView()
                WorkspaceSub.Planner -> PlannerView()
                WorkspaceSub.Calendar -> CalendarView()
                WorkspaceSub.Todo -> TodoView(todos = uiState.todos, onToggle = onToggleTodo)
                WorkspaceSub.Notes -> NotesView()
                WorkspaceSub.Reminders -> RemindersView()
                WorkspaceSub.Files -> FilesView()
                WorkspaceSub.Docgen -> DocgenView()
                WorkspaceSub.Coding -> CodingView()
                WorkspaceSub.Research -> ResearchView()
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
) {
    Text(text = title, style = AuraTextStyles.headingMd, color = AuraColors.TextPrimary, modifier = Modifier.padding(top = 6.dp))
    subtitle?.let {
        Text(
            text = it,
            style = AuraTextStyles.caption,
            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun TimelineView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING) {
        item {
            SectionHeader("Daily Timeline", "Auto-built from your calendar, tasks, and behavior patterns.")
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)) {
                    WorkspaceSampleContent.timeline.forEachIndexed { index, entry ->
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
                                            .background(entry.dot.color()),
                                )
                                if (index != WorkspaceSampleContent.timeline.lastIndex) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .padding(top = 4.dp)
                                                .width(1.dp)
                                                .height(30.dp)
                                                .background(AuraColors.TextPrimary.copy(alpha = 0.12f)),
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)) {
                                Text(text = entry.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                                Text(
                                    text = entry.subtitle,
                                    style = AuraTextStyles.caption,
                                    color = AuraColors.TextPrimary.copy(alpha = 0.5f),
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
private fun PlannerView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Smart Planner", "AURA drafted tomorrow's plan around your energy and deadlines.") }
        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AuraColors.Accent.copy(alpha = 0.1f))
                        .border(1.dp, AuraColors.Accent.copy(alpha = 0.28f), RoundedCornerShape(16.dp)),
            ) {
                Text(
                    text = "3 focus blocks, 1 buffer, and Halcyon review moved before your 2pm energy dip.",
                    style = AuraTextStyles.body,
                    color = AuraColors.TextPrimary,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
        items(WorkspaceSampleContent.planBlocks, key = { it.title }) { block ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .width(6.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(block.color.color()),
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = block.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(
                            text = "${block.time} · ${block.tag}",
                            style = AuraTextStyles.caption,
                            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Calendar") }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WorkspaceSampleContent.calendarDays.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.dow,
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                            color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .padding(top = 6.dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (day.isSelected) AuraColors.Accent else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day.num.toString(),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (day.isSelected) AuraColors.OnAccent else AuraColors.TextPrimary.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
        item { Box(modifier = Modifier.height(6.dp)) }
        items(WorkspaceSampleContent.calendarEvents, key = { it.title }) { event ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.time,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (event.conflict) AuraColors.Warning else AuraColors.Accent,
                        modifier = Modifier.width(46.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = event.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(text = event.meta, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
                    }
                    if (event.conflict) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AuraColors.Warning.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(text = "Conflict", fontFamily = InterFontFamily, fontSize = 10.sp, color = AuraColors.Warning)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoView(
    todos: ImmutableList<Todo>,
    onToggle: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("To-Do") }
        items(todos, key = { it.id }) { todo -> TodoRow(todo = todo, onToggle = { onToggle(todo.id) }) }
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    onToggle: () -> Unit,
) {
    val priColor =
        when (todo.priority) {
            TodoPriority.High -> AuraColors.Danger
            TodoPriority.Medium -> AuraColors.Warning
            TodoPriority.Low -> AuraColors.TextPrimary.copy(alpha = 0.4f)
            TodoPriority.Done -> AuraColors.TextPrimary.copy(alpha = 0.35f)
        }
    val priLabel =
        when (todo.priority) {
            TodoPriority.High -> "HIGH"
            TodoPriority.Medium -> "MED"
            TodoPriority.Low -> "LOW"
            TodoPriority.Done -> "DONE"
        }
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (todo.done) AuraColors.Accent else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (todo.done) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp),
                        ).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(color = AuraColors.Accent, bounded = false),
                            onClick = onToggle,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (todo.done) CheckIcon(modifier = Modifier.size(12.dp), tint = AuraColors.OnAccent)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = todo.title,
                    style = AuraTextStyles.bodySmall,
                    color = AuraColors.TextPrimary.copy(alpha = if (todo.done) 0.45f else 1f),
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(text = todo.meta, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.45f))
            }
            Text(text = priLabel, fontFamily = InterFontFamily, fontSize = 10.sp, color = priColor)
        }
    }
}

@Composable
private fun NotesView() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        SectionHeader("Notes")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(WorkspaceSampleContent.notes, key = { it.title }) { note ->
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = note.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(
                            text = note.body,
                            style = AuraTextStyles.caption,
                            color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Reminders") }
        items(WorkspaceSampleContent.reminders, key = { it.title }) { reminder ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AuraColors.Accent.copy(alpha = 0.12f)),
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = reminder.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(text = reminder.whenText, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("File Manager") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                WorkspaceSampleContent.storageStats.forEach { stat ->
                    StatTile(label = stat.label, value = stat.value, modifier = Modifier.weight(1f))
                }
            }
        }
        items(WorkspaceSampleContent.files, key = { it.name }) { file ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(file.tone.color()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = file.ext,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            color = AuraColors.OnAccent,
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = file.name, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary, maxLines = 1)
                        Text(text = file.meta, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DocgenView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("AI Document Generator", "Describe it — AURA drafts, formats, and cites.") }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), fillAlpha = 0.6f, borderColor = AuraColors.Accent.copy(alpha = 0.22f)) {
                Text(
                    text = "\"Draft a project brief for Halcyon's Q3 rollout, exec tone, one page.\"",
                    style = AuraTextStyles.body,
                    color = AuraColors.TextPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                WorkspaceSampleContent.docTypes.forEach { type ->
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuraColors.SurfaceGlass.copy(alpha = 0.5f))
                                .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = type, fontFamily = InterFontFamily, fontSize = 11.sp, color = AuraColors.TextPrimary.copy(alpha = 0.7f))
                    }
                }
            }
        }
        item {
            Text(
                text = "RECENT DRAFTS",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing =
                    androidx.compose.ui.unit
                        .TextUnit(0.08f, androidx.compose.ui.unit.TextUnitType.Em),
                color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
        items(WorkspaceSampleContent.drafts, key = { it.title }) { draft ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = draft.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                    Text(text = draft.meta, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun CodingView() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(CONTENT_PADDING),
    ) {
        SectionHeader("AI Coding Assistant")
        Row {
            Text(text = "Connected to ", style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
            Text(
                text = "halcyon-app",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = AuraColors.Accent,
            )
            Text(text = " · main branch", style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.5f))
        }
        Box(
            modifier =
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuraColors.CodeSurface)
                    .padding(14.dp),
        ) {
            Text(
                text = "function scheduleSync(user) {\n    return aura.reconcile(user.calendar);\n}\n// AURA: refactored for O(n) merge — 3 tests passing",
                style = AuraTextStyles.mono,
                color = Color(0xFFCFD3E5),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraColors.Accent.copy(alpha = 0.12f))
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Run Tests",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AuraColors.Accent,
                )
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraColors.SurfaceGlass.copy(alpha = 0.6f))
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Explain",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AuraColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun ResearchView() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = CONTENT_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("AI Research Workspace", "Synthesizing 12 sources on \"battery-safe fast charging.\"") }
        items(WorkspaceSampleContent.researchSources, key = { it.index }) { source ->
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(AuraColors.Accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = source.index.toString(),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = AuraColors.Accent,
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = source.title, style = AuraTextStyles.bodySmall, color = AuraColors.TextPrimary)
                        Text(text = source.domain, style = AuraTextStyles.caption, color = AuraColors.TextPrimary.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}
