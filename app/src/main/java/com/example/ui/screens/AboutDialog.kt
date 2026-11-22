package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AboutUiState
import kotlinx.coroutines.launch

@Composable
fun AboutDialog(
    state: AboutUiState,
    onRunSelfTest: () -> Unit,
    onGenerateBackup: suspend () -> String,
    onRestoreBackup: (String) -> Unit,
    onBackupMessage: (String) -> Unit,
    onSendContact: (name: String, email: String, subject: String, message: String, onClear: () -> Unit) -> Unit,
    onDismiss: () -> Unit
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ABOUT DIGGY CHESS", style = MaterialTheme.typography.titleSmall, color = AccentGreenHover)

                Text(
                    text = "Diggy Chess reviews your chess games with engine evaluations and explains the key moments in plain English.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "Games are analysed rapidly with a single unified Stockfish engine and automatic on-device alpha-beta fallback when offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "Includes offline tactical puzzles, opening explorer tools, and interactive lessons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                // Local Session Stats
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "YOUR SESSION STATS",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SessionStatTile(
                                label = "Bot Games",
                                value = state.userProfile.gamesPlayedBot.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            SessionStatTile(
                                label = "Analysed",
                                value = state.userProfile.gamesAnalysed.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SessionStatTile(
                                label = "Puzzles",
                                value = state.userProfile.puzzlesSolved.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            SessionStatTile(
                                label = "Win Streak",
                                value = state.userProfile.winStreak.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Expandable Contact & Support Section (only shown when email provider is configured)
                if (state.contactConfigured) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, if (contactExpanded) AccentGreen.copy(alpha = 0.5f) else BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { contactExpanded = !contactExpanded }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("contact_support_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MailOutline,
                                    contentDescription = "Contact and support",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Contact & Support",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = if (contactExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = if (contactExpanded) "Collapse contact form" else "Expand contact form",
                                tint = TextMuted
                            )
                        }

                        AnimatedVisibility(visible = contactExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                OutlinedTextField(
                                    value = senderName,
                                    onValueChange = { senderName = it },
                                    label = { Text("Your Name (optional)") },
                                    singleLine = true,
                                    enabled = state.contactConfigured,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contact_name_input"),
                                    colors = aboutFieldColors()
                                )

                                OutlinedTextField(
                                    value = senderEmail,
                                    onValueChange = { senderEmail = it },
                                    label = { Text("Your Email") },
                                    singleLine = true,
                                    enabled = state.contactConfigured,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contact_email_input"),
                                    colors = aboutFieldColors()
                                )

                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = { subject = it },
                                    label = { Text("Subject") },
                                    singleLine = true,
                                    enabled = state.contactConfigured,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contact_subject_input"),
                                    colors = aboutFieldColors()
                                )

                                OutlinedTextField(
                                    value = message,
                                    onValueChange = { message = it },
                                    label = { Text("Message / Bug report / Feedback") },
                                    minLines = 3,
                                    maxLines = 5,
                                    enabled = state.contactConfigured,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contact_message_input"),
                                    colors = aboutFieldColors()
                                )

                                Button(
                                    onClick = {
                                        onSendContact(senderName, senderEmail, subject, message) {
                                            message = ""
                                        }
                                    },
                                    enabled = state.contactConfigured && !state.isSendingContact,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGreen,
                                        contentColor = OnAccentDark,
                                        disabledContainerColor = BorderSubtle,
                                        disabledContentColor = TextMuted
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("contact_send_button")
                                ) {
                                    if (state.isSendingContact) {
                                        CircularProgressIndicator(
                                            color = OnAccentDark,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sending…", fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Text(
                                            text = if (state.contactConfigured) "Send Message" else "Send Unavailable",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                state.contactStatus?.let { msg ->
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (state.contactIsError) ClassBlunder else AccentGreenHover
                                    )
                                }
                            }
                        }
                    }
                }
                }

                OutlinedButton(
                    onClick = onRunSelfTest,
                    enabled = !state.isTesting,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("self_test_button")
                ) {
                    Text(
                        text = if (state.isTesting) "Running self-test…" else "Run self-test",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (state.testResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.testResults.forEach { row ->
                            Text(
                                text = (if (row.passed) "✅ " else "❌ ") + row.name +
                                    if (!row.passed && row.detail.isNotEmpty()) " — ${row.detail}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (row.passed) AccentGreenHover else ClassBlunder
                            )
                        }
                    }
                }

                Text(
                    text = "Diggy Chess · Vector pieces inspired by Cburnett (CC BY-SA 3.0) · Inter Typography (OFL 1.1).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("diggy-chess-backup.json") },
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_backup_button")
                    ) {
                        Text("Save backup", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("restore_backup_button")
                    ) {
                        Text("Restore", color = TextPrimary)
                    }
                }

                state.backupMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("about_close_button")
                    ) {
                        Text("Close", color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CardDark,
        border = BorderStroke(1.dp, LineSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentGreenHover
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun aboutFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentGreen,
    unfocusedBorderColor = BorderSubtle,
    focusedLabelColor = AccentGreen,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentGreen
)
