package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AiGameSummaryReport
import com.example.service.AiInsightState
import com.example.service.CoachChatMessage
import com.example.service.CoachChatStreamState
import com.example.ui.theme.*

@Composable
fun AiGameCoachingReportCard(
    reportState: AiInsightState<AiGameSummaryReport>,
    dailyAiRemaining: Int,
    onRefreshReport: () -> Unit,
    onUseOfflineReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    DiggyCard(
        elevated = false,
        borderColor = StudioCyan.copy(alpha = 0.42f),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_game_report_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Coaching Report",
                        tint = StudioCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Coaching Report",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Surface(
                    color = CardSurfaceElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "$dailyAiRemaining left today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontFeatureSettings = "tnum"
                        ),
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            when (reportState) {
                is AiInsightState.Idle -> {
                    Text(
                        text = "Generate a phase-by-phase human coaching breakdown grounded in Stockfish accuracy and key moments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    DiggyButton(
                        text = "Generate AI Report",
                        onClick = onRefreshReport,
                        variant = DiggyButtonVariant.SECONDARY,
                        icon = Icons.Rounded.AutoAwesome,
                        testTag = "generate_ai_report_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is AiInsightState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = StudioCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Chess Analyzer: AI Coach is writing your game report…",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioCyan
                        )
                    }
                }
                is AiInsightState.Success -> {
                    val rep = reportState.data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = StudioCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = if (rep.isFromCache) "${rep.providerLabel} · Cached" else rep.providerLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        TextButton(
                            onClick = onRefreshReport,
                            modifier = Modifier.heightIn(min = DiggySpacing.touchTargetMin)
                        ) {
                            Text("Refresh", color = AccentGreen, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    PhaseCoachingBlock("Opening Phase", rep.openingInsight, AccentGreen)
                    PhaseCoachingBlock("Middlegame", rep.middlegameInsight, StudioCyan)
                    PhaseCoachingBlock("Endgame", rep.endgameInsight, TrophyGold)

                    if (rep.strengths.isNotEmpty()) {
                        HorizontalDivider(color = LineSubtle)
                        Text("Strengths", style = MaterialTheme.typography.labelMedium, color = AccentGreenHover)
                        rep.strengths.forEach { s ->
                            Text("• $s", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }

                    if (rep.weaknesses.isNotEmpty()) {
                        Text("Areas to Sharpen", style = MaterialTheme.typography.labelMedium, color = ClassMistake)
                        rep.weaknesses.forEach { w ->
                            Text("• $w", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }

                    Surface(
                        color = AccentGreen.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(DiggySpacing.controlRadius),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Next Training Focus",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                            Text(
                                text = rep.trainingFocus,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
                is AiInsightState.Error -> {
                    Text(
                        text = reportState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClassMistake
                    )
                    reportState.fallbackMessage?.let { fb ->
                        Text(
                            text = fb,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiggyChip(
                            label = "Retry AI",
                            selected = false,
                            onClick = onRefreshReport,
                            icon = Icons.Rounded.Refresh,
                            accentColor = StudioCyan,
                            testTag = "retry_ai_report_button"
                        )
                        DiggyChip(
                            label = "Offline Report",
                            selected = false,
                            onClick = onUseOfflineReport,
                            icon = Icons.Rounded.MenuBook,
                            accentColor = AccentGreen,
                            testTag = "offline_ai_report_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseCoachingBlock(title: String, body: String, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 0.6.sp
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
}

@Composable
fun AskAiCoachChatCard(
    messages: List<CoachChatMessage>,
    streamState: CoachChatStreamState,
    currentPly: Int,
    onSendQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var draftQuestion by remember { mutableStateOf("") }
    val isBusy = streamState is CoachChatStreamState.Loading || streamState is CoachChatStreamState.Streaming
    val quickPrompts = remember(currentPly) {
        if (currentPly > 0) {
            listOf(
                "Why is Stockfish's best move stronger here?",
                "What tactical motif did I miss?",
                "What is my best plan from this position?",
                "Explain this opening theory & recent web notes"
            )
        } else {
            listOf(
                "Where was the turning point of this game?",
                "How did both sides handle the opening?",
                "What tactic should I practice after this game?",
                "Search chess theory & recent web notes"
            )
        }
    }

    DiggyCard(
        elevated = false,
        borderColor = AccentGreen.copy(alpha = 0.35f),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ask_ai_coach_chat_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QuestionAnswer,
                    contentDescription = "Ask AI Chess Coach",
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ask Chess Analyzer: AI Coach",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ask about mistakes, alternatives, plans, or tactics grounded in Stockfish.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEachIndexed { idx, prompt ->
                    DiggyChip(
                        label = prompt,
                        selected = false,
                        onClick = { if (!isBusy) onSendQuestion(prompt) },
                        icon = Icons.Rounded.AutoAwesome,
                        accentColor = StudioCyan,
                        testTag = "coach_quick_prompt_$idx"
                    )
                }
            }

            if (messages.isNotEmpty() || isBusy || streamState is CoachChatStreamState.Error) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    messages.forEach { msg ->
                        Surface(
                            color = if (msg.isUser) AccentGreen.copy(alpha = 0.14f) else CardSurfaceElevated,
                            shape = RoundedCornerShape(DiggySpacing.controlRadius),
                            border = BorderStroke(
                                1.dp,
                                if (msg.isUser) AccentGreen.copy(alpha = 0.45f) else BorderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (msg.isUser) "You" else (msg.providerLabel ?: "AI Coach"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.isUser) AccentGreen else StudioCyan
                                )
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                if (!msg.isUser && msg.webSources.isNotEmpty()) {
                                    HorizontalDivider(
                                        color = LineSubtle,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        text = "TinyFish Web Sources",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    msg.webSources.forEachIndexed { sIdx, src ->
                                        Surface(
                                            color = CardSurfaceElevated,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    runCatching { uriHandler.openUri(src.url) }
                                                }
                                                .testTag("coach_web_source_$sIdx")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Public,
                                                    contentDescription = "Web reference",
                                                    tint = StudioCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = src.title,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = src.url,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                    contentDescription = "Open link",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    when (streamState) {
                        is CoachChatStreamState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = StudioCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Coach is thinking…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioCyan
                                )
                            }
                        }
                        is CoachChatStreamState.Streaming -> {
                            Surface(
                                color = CardSurfaceElevated,
                                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "AI Coach · typing…",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioCyan
                                    )
                                    Text(
                                        text = streamState.partialText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                        is CoachChatStreamState.Error -> {
                            DiggyErrorState(message = streamState.message)
                        }
                        is CoachChatStreamState.Idle -> Unit
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draftQuestion,
                    onValueChange = { draftQuestion = it },
                    placeholder = { Text("Ask about this move or position…", color = InputHint) },
                    singleLine = true,
                    enabled = !isBusy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (draftQuestion.isNotBlank() && !isBusy) {
                                val q = draftQuestion
                                draftQuestion = ""
                                onSendQuestion(q)
                            }
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = InputText),
                    colors = diggyInputColors(),
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("coach_chat_input")
                )
                FilledIconButton(
                    onClick = {
                        val q = draftQuestion
                        draftQuestion = ""
                        onSendQuestion(q)
                    },
                    enabled = draftQuestion.isNotBlank() && !isBusy,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentGreen,
                        contentColor = OnAccentDark
                    ),
                    modifier = Modifier
                        .size(DiggySpacing.touchTargetMin)
                        .testTag("coach_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send question to AI Coach"
                    )
                }
            }
        }
    }
}
