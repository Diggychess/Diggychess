package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AnalysisPreset
import com.example.ui.viewmodel.SettingsUiState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSetDarkMode: (Boolean) -> Unit,
    onSelectBoardTheme: (BoardTheme) -> Unit,
    onSetPieceAnimations: (Boolean) -> Unit,
    onSelectAnalysisPreset: (AnalysisPreset) -> Unit,
    onSelectDailyGoal: (Int) -> Unit,
    onRunSelfTest: () -> Unit,
    onGenerateBackup: suspend () -> String,
    onRestoreBackup: (String) -> Unit,
    onBackupMessage: (String) -> Unit,
    onSendContact: (name: String, email: String, subject: String, message: String, onClear: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contactExpanded by remember { mutableStateOf(false) }
    var senderName by remember { mutableStateOf("") }
    var senderEmail by remember(state.contactEmail) { mutableStateOf(state.contactEmail) }
    var subject by remember { mutableStateOf("KnightScope Feedback") }
    var message by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = onGenerateBackup()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onBackupMessage("Backup saved.")
            } catch (_: Exception) {
                onBackupMessage("Backup was not saved.")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                onRestoreBackup(content)
            } else {
                onBackupMessage("Could not read backup file.")
            }
        } catch (_: Exception) {
            onBackupMessage("This is not a valid backup file.")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Settings & About", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                text = "Customize themes, board aesthetics, analysis speed, local backups, and open-source licenses.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Appearance & Board Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("APPEARANCE & BOARD", style = MaterialTheme.typography.titleSmall, color = TextMuted)

                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = AccentGreenHover)
                        Column {
                            Text("Dark Studio Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(if (state.isDarkMode) "Dark Studio active" else "Light Classic active", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                    Switch(
                        checked = state.isDarkMode,
                        onCheckedChange = onSetDarkMode,
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smooth Piece Animations", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Animated piece slides across all boards", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Switch(
                        checked = state.pieceAnimations,
                        onCheckedChange = onSetPieceAnimations,
                        modifier = Modifier.testTag("settings_animations_switch")
                    )
                }

                Text("Board Colors", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoardTheme.entries.forEach { theme ->
                        val selected = theme == state.boardTheme
                        Surface(
                            onClick = { onSelectBoardTheme(theme) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) AccentGreen.copy(alpha = 0.16f) else SurfaceDark,
                            border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_board_${theme.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(modifier = Modifier.size(28.dp, 14.dp).clip(RoundedCornerShape(4.dp))) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.lightSquare))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.darkSquare))
                                }
                                Text(theme.label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Analysis & Training Presets Card (Clean user-friendly presets replacing raw thresholds)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ANALYSIS & TRAINING", style = MaterialTheme.typography.titleSmall, color = TextMuted)
                Text("Game Review Depth Preset", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalysisPreset.entries.forEach { preset ->
                        val selected = state.reviewSettings.depth == preset.depth
                        Surface(
                            onClick = { onSelectAnalysisPreset(preset) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) AccentGreen.copy(alpha = 0.18f) else SurfaceDark,
                            border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_preset_${preset.label.lowercase()}")
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(preset.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (selected) AccentGreenHover else TextPrimary)
                                Text(preset.subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }

                Text("Daily Puzzle Goal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 10, 20).forEach { goal ->
                        FilterChip(
                            selected = state.dailyGoal == goal,
                            onClick = { onSelectDailyGoal(goal) },
                            label = { Text("$goal / day") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                                selectedLabelColor = AccentGreenHover
                            )
                        )
                    }
                }
            }
        }

        // Data Backup & Engine Diagnostics
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("BACKUP & SELF-TEST", style = MaterialTheme.typography.titleSmall, color = TextMuted)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("knightscope-backup.json") },
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_backup_export")
                    ) {
                        Text("Save Backup", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_backup_restore")
                    ) {
                        Text("Restore", color = TextPrimary)
                    }
                }

                state.backupMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = AccentGreenHover)
                }

                OutlinedButton(
                    onClick = onRunSelfTest,
                    enabled = !state.isTesting,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_self_test_button")
                ) {
                    Text(
                        text = if (state.isTesting) "Running diagnostics…" else "Run Engine & Rules Self-Test",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (state.testResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.testResults.forEach { row ->
                            Text(
                                text = (if (row.passed) "✔ " else "✖ ") + row.name +
                                    if (!row.passed && row.detail.isNotEmpty()) " — ${row.detail}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (row.passed) AccentGreenHover else ClassBlunder
                            )
                        }
                    }
                }
            }
        }

        // Optional Contact & Support Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, if (contactExpanded) AccentGreen.copy(alpha = 0.5f) else LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { contactExpanded = !contactExpanded }
                        .padding(14.dp)
                        .testTag("settings_contact_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.MailOutline, contentDescription = "Contact & Support", tint = AccentGreen)
                        Text("Contact & Support", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(
                        imageVector = if (contactExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (contactExpanded) "Collapse" else "Expand",
                        tint = TextMuted
                    )
                }

                AnimatedVisibility(visible = contactExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!state.contactConfigured) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceDark,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth().testTag("settings_contact_unconfigured_notice")
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.Info, contentDescription = "Notice", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Contact delivery is optional and currently disabled because no email service (Formspree or EmailJS) is configured.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = senderName,
                            onValueChange = { senderName = it },
                            label = { Text("Your Name (optional)") },
                            singleLine = true,
                            enabled = state.contactConfigured,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = senderEmail,
                            onValueChange = { senderEmail = it },
                            label = { Text("Your Email") },
                            singleLine = true,
                            enabled = state.contactConfigured,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            singleLine = true,
                            enabled = state.contactConfigured,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Message / Feedback") },
                            minLines = 3,
                            enabled = state.contactConfigured,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSendContact(senderName, senderEmail, subject, message) { message = "" }
                            },
                            enabled = state.contactConfigured && !state.isSendingContact,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(if (state.contactConfigured) "Send Message" else "Send Unavailable", fontWeight = FontWeight.Bold)
                        }
                        state.contactStatus?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = if (state.contactIsError) ClassBlunder else AccentGreenHover)
                        }
                    }
                }
            }
        }

        // Open-Source Credits & Licenses
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("OPEN-SOURCE CREDITS & LICENSES", style = MaterialTheme.typography.titleSmall, color = TextMuted)
                Text("• Stockfish Online / Local Alpha-Beta Chess Engine (GPL-3.0)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text("• Lichess Open Puzzle Database & Opening Explorer (CC0 Public Domain)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text("• Lichess Cloud Eval, Open Challenge & Game APIs (Free Public API)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text("• Chess.com Published Data API (Public Game Importer)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text("• Cburnett-Inspired Staunton Vector Pieces (CC BY-SA 3.0)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text("• Inter & JetBrains Mono Typography (SIL Open Font License 1.1)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
        }
    }
}
