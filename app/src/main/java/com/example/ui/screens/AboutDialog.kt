package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("ABOUT KNIGHTSCOPE", style = MaterialTheme.typography.titleSmall, color = TextMuted)

                Text(
                    text = "KnightScope reviews your chess games with engine evaluations and explains the key moments in plain English.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "Games are analysed on your device and PGNs are never uploaded. When online, Lichess Cloud Eval is queried for fast known positions, with the on-device alpha-beta engine as fallback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "Puzzles come from the Lichess open puzzle database (CC0), bundled directly for offline training.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

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
                    text = "Sources & licenses: Lichess puzzle database (CC0) · Lichess cloud-eval API · Cburnett-inspired vector pieces (CC BY-SA 3.0) · Inter & JetBrains Mono fonts (OFL). Opening names come from a built-in list of 80+ common openings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("knightscope-backup.json") },
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
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = AccentGreen)
                    }
                }
            }
        }
    }
}
