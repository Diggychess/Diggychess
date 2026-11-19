package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AnalyzeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformImporterSheet(
    state: AnalyzeUiState,
    onSelectPlatform: (ChessPlatform) -> Unit,
    onUsernameChange: (String) -> Unit,
    onFetch: () -> Unit,
    onSpeedFilter: (ImportSpeedFilter) -> Unit,
    onResultFilter: (ImportResultFilter) -> Unit,
    onColorFilter: (ImportColorFilter) -> Unit,
    onAnalyzeImportedGame: (ImportedGameSummary) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Import by Username",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fetch live ratings & recent games from Chess.com or Lichess.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("importer_close_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close importer", tint = TextMuted)
                }
            }

            // Platform selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChessPlatform.entries.forEach { platform ->
                    val active = state.importPlatform == platform
                    FilterChip(
                        selected = active,
                        onClick = { onSelectPlatform(platform) },
                        label = {
                            Text(
                                text = platform.label,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.20f),
                            selectedLabelColor = AccentGreenHover
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("platform_chip_${platform.id}")
                    )
                }
            }

            // Username search bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.importUsername,
                    onValueChange = onUsernameChange,
                    placeholder = { Text(state.importPlatform.placeholder, color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onFetch() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_username_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardDark,
                        unfocusedContainerColor = CardDark,
                        focusedBorderColor = StudioCyan,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = onFetch,
                    enabled = !state.isFetchingPlatform,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = OnAccentDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 54.dp)
                        .testTag("import_fetch_button")
                ) {
                    if (state.isFetchingPlatform) {
                        CircularProgressIndicator(
                            color = OnAccentDark,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(Icons.Filled.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetch", fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = state.platformError != null) {
                Surface(
                    color = ClassBlunder.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ClassBlunder.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.platformError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Player Profile & Ratings Card
            state.platformProfile?.let { profile ->
                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profile.title?.let { t ->
                                Surface(color = ClassMistake, shape = RoundedCornerShape(5.dp)) {
                                    Text(
                                        text = t,
                                        color = OnAccentDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = profile.username,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "· ${profile.platform.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioCyan
                            )
                        }

                        if (profile.stats.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                profile.stats.forEach { st ->
                                    Surface(
                                        color = SurfaceDark,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(st.mode, fontSize = 11.sp, color = TextMuted)
                                            Text(
                                                text = "${st.rating}",
                                                fontSize = 16.sp,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (st.losses > 0 || st.draws > 0) {
                                                Text(
                                                    text = "${st.wins}W/${st.losses}L/${st.draws}D",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            } else if (st.wins > 0) {
                                                Text("${st.wins} games", fontSize = 10.sp, color = TextMuted)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Filter Chips (Time control, Result, Color)
            if (state.allImportedGames.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ImportSpeedFilter.entries.forEach { f ->
                            FilterChip(
                                selected = state.speedFilter == f,
                                onClick = { onSpeedFilter(f) },
                                label = { Text(f.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = StudioCyan
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ImportResultFilter.entries.forEach { f ->
                            FilterChip(
                                selected = state.resultFilter == f,
                                onClick = { onResultFilter(f) },
                                label = { Text(f.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentGreenHover
                                )
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(28.dp), color = BorderSubtle)
                        ImportColorFilter.entries.forEach { f ->
                            FilterChip(
                                selected = state.colorFilter == f,
                                onClick = { onColorFilter(f) },
                                label = { Text(f.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentGreenHover
                                )
                            )
                        }
                    }
                }
            }

            // Virtualized Game Results List
            if (state.allImportedGames.isNotEmpty() && state.filteredImportedGames.isEmpty()) {
                Text(
                    text = "No games match the active filters. Adjust Time, Result, or Color above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.filteredImportedGames, key = { "${it.platform.id}_${it.id}" }) { game ->
                        ImportedGameCard(
                            game = game,
                            onAnalyze = { onAnalyzeImportedGame(game) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportedGameCard(
    game: ImportedGameSummary,
    onAnalyze: () -> Unit
) {
    val dateLabel = remember(game.playedAtMillis) {
        SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(game.playedAtMillis))
    }
    val outcomeColor = when (game.userOutcome) {
        "win" -> AccentGreenHover
        "loss" -> ClassBlunder
        else -> ClassInaccuracy
    }
    val outcomeLabel = when (game.userOutcome) {
        "win" -> "WIN"
        "loss" -> "LOSS"
        else -> "DRAW"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("imported_game_${game.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = outcomeColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = outcomeLabel,
                            color = outcomeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${game.timeClass.replaceFirstChar { it.uppercase() }} (${game.timeControl})",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioCyan
                    )
                }
                Text(
                    text = "$dateLabel · ${game.moveCount} moves",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "⚪ ${game.whiteName}" + (game.whiteRating?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (game.userColor == "white") FontWeight.Bold else FontWeight.Normal,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "⚫ ${game.blackName}" + (game.blackRating?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (game.userColor == "black") FontWeight.Bold else FontWeight.Normal,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (game.eco?.let { "$it · " } ?: "") + game.openingName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAnalyze,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = OnAccentDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("analyze_imported_${game.id}")
                ) {
                    Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyse", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
