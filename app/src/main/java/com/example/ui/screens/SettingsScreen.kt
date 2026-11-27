package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AnalysisPreset
import com.example.ui.viewmodel.SettingsUiState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

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
    onSaveGroqApiKey: (String) -> Unit = {},
    onSaveTinyFishApiKey: (String) -> Unit = {},
    onGoogleSignInStarting: () -> Unit = {},
    onGoogleIdTokenSignedIn: (String) -> Unit = {},
    onSignOutAccount: () -> Unit = {},
    onAuthError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contactExpanded by remember { mutableStateOf(false) }
    var senderName by remember { mutableStateOf("") }
    var senderEmail by remember(state.contactEmail) { mutableStateOf(state.contactEmail) }
    var subject by remember { mutableStateOf("Chess Analyzer: AI Coach Feedback") }
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
                text = "Customize themes, board style, pieces, cloud sync, sound, haptics, and backups.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Account & Cloud Sync Card (Firebase Google Sign-In via CredentialManager + Guest Mode)
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (state.isSignedIn) AccentGreen.copy(alpha = 0.18f) else CardSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isSignedIn) Icons.Rounded.CloudDone else Icons.Rounded.AccountCircle,
                            contentDescription = "Account status",
                            tint = if (state.isSignedIn) AccentGreen else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isSignedIn) {
                                state.authDisplayName?.takeIf { it.isNotBlank() } ?: "Google Account"
                            } else {
                                "Local Guest Mode"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (state.isSignedIn) {
                                "${state.authEmail ?: "Signed in"} · Firestore cloud sync active"
                            } else {
                                "Games save locally. Sign in with Google to sync games to Firestore."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                if (state.isSignedIn) {
                    DiggyButton(
                        text = "Sign Out",
                        onClick = {
                            scope.launch {
                                runCatching {
                                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                                }
                                onSignOutAccount()
                            }
                        },
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.Logout,
                        testTag = "settings_sign_out_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    DiggyButton(
                        text = if (state.isAuthBusy) "Signing in…" else "Sign in with Google",
                        onClick = {
                            val activity = context.findHostActivity()
                            if (activity == null) {
                                onAuthError("Sign-in requires an active window.")
                                return@DiggyButton
                            }
                            onGoogleSignInStarting()
                            scope.launch {
                                val credManager = CredentialManager.create(activity)
                                val webClientId = context.getString(R.string.default_web_client_id)
                                try {
                                    val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(signInOption)
                                        .build()
                                    val result = credManager.getCredential(activity, request)
                                    val cred = result.credential
                                    if (cred is CustomCredential &&
                                        cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    ) {
                                        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                                        onGoogleIdTokenSignedIn(token)
                                    } else {
                                        onAuthError("Unsupported credential type returned.")
                                    }
                                } catch (_: GetCredentialCancellationException) {
                                    onAuthError("Sign-in cancelled. Continuing in guest mode.")
                                } catch (_: NoCredentialException) {
                                    // Fallback to GetGoogleIdOption if device has no saved Google account sheet
                                    try {
                                        val googleIdOpt = GetGoogleIdOption.Builder()
                                            .setServerClientId(webClientId)
                                            .setFilterByAuthorizedAccounts(false)
                                            .build()
                                        val req2 = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOpt)
                                            .build()
                                        val res2 = credManager.getCredential(activity, req2)
                                        val cred2 = res2.credential
                                        if (cred2 is CustomCredential &&
                                            cred2.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        ) {
                                            val token = GoogleIdTokenCredential.createFrom(cred2.data).idToken
                                            onGoogleIdTokenSignedIn(token)
                                        } else {
                                            onAuthError("Google account sign-in unavailable.")
                                        }
                                    } catch (e2: Exception) {
                                        onAuthError(e2.message ?: "No Google account available on device.")
                                    }
                                } catch (e: Exception) {
                                    onAuthError(e.message ?: "Google Sign-In failed.")
                                }
                            }
                        },
                        loading = state.isAuthBusy,
                        variant = DiggyButtonVariant.PRIMARY,
                        icon = Icons.Rounded.Login,
                        testTag = "settings_google_signin_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                state.authMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.authIsError) FeedbackDangerRed else AccentGreen
                    )
                }
            }
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
                    subtitle = "300ms lifted piece movement across all boards",
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
                                                        selected && theme == BoardTheme.GREEN -> "Active · Default"
                                                        selected -> "Active"
                                                        theme == BoardTheme.GREEN -> "Default"
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
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Text("Daily Puzzle Goal", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val goalOptions = listOf(3, 5, 10, 20)
                    if (maxWidth < 340.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            goalOptions.chunked(2).forEach { pair ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    pair.forEach { goal ->
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
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            goalOptions.forEach { goal ->
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
                if (state.dailyGoal !in listOf(3, 5, 10, 20)) {
                    Text(
                        text = "Custom target active: ${state.dailyGoal} puzzles / day (editable on Home)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen
                    )
                }
            }
        }

        // AI Chess Coach & TinyFish Web Search Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            var groqDraft by remember(state.groqApiKey) { mutableStateOf(state.groqApiKey) }
            var tinyFishDraft by remember(state.tinyFishApiKey) { mutableStateOf(state.tinyFishApiKey) }
            var showGroq by remember { mutableStateOf(false) }
            var showTinyFish by remember { mutableStateOf(false) }

            val groqActive = state.groqApiKey.isNotBlank() || state.hasBuildGroqKey
            val tinyFishActive = state.tinyFishApiKey.isNotBlank() || state.hasBuildTinyFishKey

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Chess Coach & TinyFish",
                        tint = StudioCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Chess Coach & TinyFish Search",
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioCyan
                    )
                }
                Text(
                    text = "Coach uses Firebase AI Logic first, then Groq, Free Cloud LLM, and always guarantees a Stockfish Tactical Coach reply. Add optional keys below or in AI Studio Secrets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiStatusBadge(
                        label = "Groq Cloud",
                        active = groqActive,
                        sourceHint = when {
                            state.groqApiKey.isNotBlank() -> "Device key"
                            state.hasBuildGroqKey -> "Secrets"
                            else -> "Optional"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AiStatusBadge(
                        label = "TinyFish Web",
                        active = tinyFishActive,
                        sourceHint = when {
                            state.tinyFishApiKey.isNotBlank() -> "Device key"
                            state.hasBuildTinyFishKey -> "Secrets"
                            else -> "Optional"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = groqDraft,
                    onValueChange = { groqDraft = it },
                    label = { Text("Groq API Key (gsk_...)") },
                    singleLine = true,
                    visualTransformation = if (showGroq) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGroq = !showGroq }) {
                            Icon(
                                imageVector = if (showGroq) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showGroq) "Hide key" else "Show key"
                            )
                        }
                    },
                    colors = diggyInputColors(),
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_groq_key_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyButton(
                        text = "Save Groq Key",
                        onClick = { onSaveGroqApiKey(groqDraft) },
                        variant = DiggyButtonVariant.SECONDARY,
                        testTag = "settings_save_groq_key",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "Clear",
                        onClick = {
                            groqDraft = ""
                            onSaveGroqApiKey("")
                        },
                        variant = DiggyButtonVariant.OUTLINED,
                        testTag = "settings_clear_groq_key"
                    )
                }

                OutlinedTextField(
                    value = tinyFishDraft,
                    onValueChange = { tinyFishDraft = it },
                    label = { Text("TinyFish Search API Key") },
                    singleLine = true,
                    visualTransformation = if (showTinyFish) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showTinyFish = !showTinyFish }) {
                            Icon(
                                imageVector = if (showTinyFish) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showTinyFish) "Hide key" else "Show key"
                            )
                        }
                    },
                    colors = diggyInputColors(),
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_tinyfish_key_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyButton(
                        text = "Save TinyFish Key",
                        onClick = { onSaveTinyFishApiKey(tinyFishDraft) },
                        variant = DiggyButtonVariant.SECONDARY,
                        testTag = "settings_save_tinyfish_key",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "Clear",
                        onClick = {
                            tinyFishDraft = ""
                            onSaveTinyFishApiKey("")
                        },
                        variant = DiggyButtonVariant.OUTLINED,
                        testTag = "settings_clear_tinyfish_key"
                    )
                }

                state.aiKeysStatus?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
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
                        onClick = { exportLauncher.launch("chess-analyzer-backup.json") },
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
                                colors = diggyInputColors(),
                                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = senderEmail,
                                onValueChange = { senderEmail = it },
                                label = { Text("Your Email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = diggyInputColors(),
                                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                label = { Text("Subject") },
                                singleLine = true,
                                colors = diggyInputColors(),
                                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text("Message / Feedback") },
                                minLines = 3,
                                colors = diggyInputColors(),
                                shape = RoundedCornerShape(DiggySpacing.controlRadius),
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
            text = "Chess Analyzer: AI Coach · Vector pieces inspired by Cburnett (CC BY-SA 3.0) · Nunito, Inter & JetBrains Mono",
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

@Composable
private fun AiStatusBadge(
    label: String,
    active: Boolean,
    sourceHint: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (active) AccentGreen.copy(alpha = 0.14f) else CardSurfaceElevated,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(1.dp, if (active) AccentGreen.copy(alpha = 0.55f) else BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) AccentGreen else TextPrimary
            )
            Text(
                text = if (active) "Active · $sourceHint" else sourceHint,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) AccentGreenHover else TextMuted
            )
        }
    }
}
