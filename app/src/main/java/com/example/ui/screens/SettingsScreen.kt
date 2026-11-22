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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AnalysisPreset
import com.example.ui.viewmodel.SettingsUiState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSetDarkMode: (Boolean) -> Unit,
    onSelectBoardTheme: (BoardTheme) -> Unit,
    onSelectPieceStyle: (PieceStyle) -> Unit,
    onSetBoard3dEnabled: (Boolean) -> Unit,
    onSetPieceAnimations: (Boolean) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit,
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
    var subject by remember { mutableStateOf("Diggy Chess Feedback") }
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
            .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
        verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                text = "Customize themes, board style, pieces, sound, haptics, analysis depth, and local backups.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Appearance, Board & Pieces Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Appearance & Board", style = MaterialTheme.typography.titleMedium, color = AccentGreen)

                SettingsToggleRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "Dark Theme",
                    subtitle = if (state.isDarkMode) "Dark mode active" else "Light mode active",
                    checked = state.isDarkMode,
                    onCheckedChange = onSetDarkMode,
                    testTag = "settings_dark_mode_switch"
                )

                HorizontalDivider(color = LineSubtle)

                SettingsToggleRow(
                    icon = Icons.Rounded.ViewInAr,
                    title = "3D Perspective Board",
                    subtitle = "Tilted wooden cabinet frame with upright 3D Staunton pieces",
                    checked = state.board3dEnabled,
                    onCheckedChange = onSetBoard3dEnabled,
                    testTag = "settings_3d_board_switch"
                )

                HorizontalDivider(color = LineSubtle)

                SettingsToggleRow(
                    icon = Icons.Rounded.Animation,
                    title = "Smooth Piece Animations",
                    subtitle = "200ms lifted piece movement across all boards",
                    checked = state.pieceAnimations,
                    onCheckedChange = onSetPieceAnimations,
                    testTag = "settings_animations_switch"
                )

                HorizontalDivider(color = LineSubtle)

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("settings_theme_selector")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Theme Selector", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Text(
                                text = "Preview with ${state.pieceStyle.label} pieces · Switch anytime",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    BoardTheme.entries.chunked(2).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowThemes.forEach { theme ->
                                val selected = theme == state.boardTheme
                                Surface(
                                    onClick = { onSelectBoardTheme(theme) },
                                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                    color = if (selected) AccentGreen.copy(alpha = 0.14f) else CardSurfaceElevated,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) AccentGreen else BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 68.dp)
                                        .testTag("settings_board_${theme.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 2x2 mini checkerboard preview with live piece set contrast
                                        Column(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (selected) AccentGreen else BorderSubtle,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(theme.lightSquare),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    ChessPieceIcon(
                                                        piece = ChessPiece(PieceColor.BLACK, PieceType.QUEEN),
                                                        pieceStyle = state.pieceStyle,
                                                        modifier = Modifier.fillMaxSize(0.84f)
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(theme.darkSquare)
                                                )
                                            }
                                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(theme.darkSquare),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    ChessPieceIcon(
                                                        piece = ChessPiece(PieceColor.WHITE, PieceType.KNIGHT),
                                                        pieceStyle = state.pieceStyle,
                                                        modifier = Modifier.fillMaxSize(0.84f)
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(theme.lightSquare)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = theme.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selected) AccentGreen else TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CheckCircle,
                                                        contentDescription = "${theme.label} selected",
                                                        tint = AccentGreen,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                Text(
                                                    text = when {
                                                        selected && theme == BoardTheme.GOLD -> "Active · Default"
                                                        selected -> "Active"
                                                        theme == BoardTheme.GOLD -> "Default"
                                                        else -> "Palette"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) AccentGreen.copy(alpha = 0.9f) else TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowThemes.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                HorizontalDivider(color = LineSubtle)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Piece Style", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    DiggySegmentedControl(
                        options = PieceStyle.entries,
                        selectedOption = state.pieceStyle,
                        onSelect = onSelectPieceStyle,
                        labelFor = { it.label },
                        tagFor = { "settings_piece_${it.id}" }
                    )
                    Text(
                        text = state.pieceStyle.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Sound & Haptics Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Sound & Haptics", style = MaterialTheme.typography.titleMedium, color = AccentGreen)

                SettingsToggleRow(
                    icon = Icons.Rounded.VolumeUp,
                    title = "Move & Check Sounds",
                    subtitle = "Warm boxwood taps, double-clack captures, checkmate fanfare & draw cues",
                    checked = state.soundEnabled,
                    onCheckedChange = { enabled ->
                        onSetSoundEnabled(enabled)
                        if (enabled) {
                            com.example.audio.ChessSoundPlayer.ensureInit(context)
                            com.example.audio.ChessSoundPlayer.playMove()
                        }
                    },
                    testTag = "settings_sound_switch"
                )

                AnimatedVisibility(visible = state.soundEnabled) {
                    var activePreview by remember { mutableStateOf<String?>(null) }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("settings_sound_preview_group")
                    ) {
                        Text(
                            text = "Preview Audio Cues",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyChip(
                                label = "Move",
                                selected = activePreview == "move",
                                onClick = {
                                    activePreview = "move"
                                    com.example.audio.ChessSoundPlayer.ensureInit(context)
                                    com.example.audio.ChessSoundPlayer.playMove()
                                },
                                testTag = "settings_preview_move",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyChip(
                                label = "Capture",
                                selected = activePreview == "capture",
                                onClick = {
                                    activePreview = "capture"
                                    com.example.audio.ChessSoundPlayer.ensureInit(context)
                                    com.example.audio.ChessSoundPlayer.playCapture()
                                },
                                testTag = "settings_preview_capture",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyChip(
                                label = "Check",
                                selected = activePreview == "check",
                                onClick = {
                                    activePreview = "check"
                                    com.example.audio.ChessSoundPlayer.ensureInit(context)
                                    com.example.audio.ChessSoundPlayer.playCheck()
                                },
                                testTag = "settings_preview_check",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyChip(
                                label = "Checkmate",
                                selected = activePreview == "checkmate",
                                onClick = {
                                    activePreview = "checkmate"
                                    com.example.audio.ChessSoundPlayer.ensureInit(context)
                                    com.example.audio.ChessSoundPlayer.playCheckmate()
                                },
                                testTag = "settings_preview_checkmate",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyChip(
                                label = "Draw",
                                selected = activePreview == "draw",
                                onClick = {
                                    activePreview = "draw"
                                    com.example.audio.ChessSoundPlayer.ensureInit(context)
                                    com.example.audio.ChessSoundPlayer.playDraw()
                                },
                                testTag = "settings_preview_draw",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = LineSubtle)

                SettingsToggleRow(
                    icon = Icons.Rounded.Vibration,
                    title = "Haptic Feedback",
                    subtitle = "Subtle tactile response on moves and taps",
                    checked = state.hapticsEnabled,
                    onCheckedChange = onSetHapticsEnabled,
                    testTag = "settings_haptics_switch"
                )
            }
        }

        // Analysis & Training Presets Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Analysis & Training", style = MaterialTheme.typography.titleMedium, color = AccentGreen)
                Text("Game Review Depth Preset", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalysisPreset.entries.forEach { preset ->
                        val selected = state.reviewSettings.depth == preset.depth
                        Surface(
                            onClick = { onSelectAnalysisPreset(preset) },
                            shape = RoundedCornerShape(DiggySpacing.controlRadius),
                            color = if (selected) AccentGreen.copy(alpha = 0.16f) else CardSurfaceElevated,
                            border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("settings_preset_${preset.label.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) AccentGreen else TextPrimary
                                )
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Text("Daily Puzzle Goal", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(3, 5, 10, 20).forEach { goal ->
                        DiggyChip(
                            label = "$goal / day",
                            selected = state.dailyGoal == goal,
                            onClick = { onSelectDailyGoal(goal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Data Backup & Engine Diagnostics
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup & Diagnostics", style = MaterialTheme.typography.titleMedium, color = AccentGreen)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyButton(
                        text = "Save Backup",
                        onClick = { exportLauncher.launch("diggy-chess-backup.json") },
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.UploadFile,
                        testTag = "settings_backup_export",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "Restore",
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.Download,
                        testTag = "settings_backup_restore",
                        modifier = Modifier.weight(1f)
                    )
                }

                state.backupMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }

                DiggyButton(
                    text = if (state.isTesting) "Running Diagnostics…" else "Run Engine & Rules Self-Test",
                    onClick = onRunSelfTest,
                    variant = DiggyButtonVariant.SECONDARY,
                    loading = state.isTesting,
                    icon = Icons.Rounded.VerifiedUser,
                    testTag = "settings_self_test_button",
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.testResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.testResults.forEach { row ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (row.passed) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                    contentDescription = if (row.passed) "Passed" else "Failed",
                                    tint = if (row.passed) FeedbackCorrectGreen else FeedbackDangerRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = row.name + if (!row.passed && row.detail.isNotEmpty()) " — ${row.detail}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (row.passed) TextPrimary else FeedbackDangerRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Optional Contact & Support Card (shown only when a delivery service is configured)
        if (state.contactConfigured) {
            DiggyCard(
                borderColor = if (contactExpanded) AccentGreen.copy(alpha = 0.5f) else BorderSubtle,
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = DiggySpacing.touchTargetMin)
                            .clickable { contactExpanded = !contactExpanded }
                            .padding(16.dp)
                            .testTag("settings_contact_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MailOutline,
                                contentDescription = "Contact & Support",
                                tint = AccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Contact & Support",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (contactExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (contactExpanded) "Collapse" else "Expand",
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    AnimatedVisibility(visible = contactExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = senderName,
                                onValueChange = { senderName = it },
                                label = { Text("Your Name (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = senderEmail,
                                onValueChange = { senderEmail = it },
                                label = { Text("Your Email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                label = { Text("Subject") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text("Message / Feedback") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            DiggyButton(
                                text = "Send Message",
                                onClick = {
                                    onSendContact(senderName, senderEmail, subject, message) { message = "" }
                                },
                                enabled = !state.isSendingContact,
                                loading = state.isSendingContact,
                                modifier = Modifier.fillMaxWidth()
                            )
                            state.contactStatus?.let { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.contactIsError) FeedbackDangerRed else FeedbackCorrectGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Diggy Chess · Vector pieces inspired by Cburnett (CC BY-SA 3.0) · Inter Font (OFL 1.1)",
            style = MaterialTheme.typography.labelSmall,
            color = TextDisabled,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DiggySpacing.touchTargetMin),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AccentGreen,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
