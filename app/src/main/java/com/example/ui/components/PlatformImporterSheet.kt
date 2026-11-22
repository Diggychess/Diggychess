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
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import Games",
                        style = MaterialTheme.typography.titleLarge,
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
                    modifier = Modifier
                        .size(DiggySpacing.touchTargetMin)
                        .testTag("importer_close_button")
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close importer",
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Platform selector
            DiggySegmentedControl(
                options = ChessPlatform.entries,
                selectedOption = state.importPlatform,
                onSelect = onSelectPlatform,
                labelFor = { it.label },
                tagFor = { "platform_chip_${it.id}" }
            )

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
                        focusedContainerColor = CardSurfaceElevated,
                        unfocusedContainerColor = CardSurfaceElevated,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(DiggySpacing.controlRadius)
                )
                DiggyButton(
                    text = "Fetch",
                    onClick = onFetch,
                    loading = state.isFetchingPlatform,
                    icon = Icons.Rounded.PersonSearch,
                    testTag = "import_fetch_button"
                )
            }

            AnimatedVisibility(visible = state.platformError != null) {
                DiggyErrorState(
                    message = state.platformError.orEmpty(),
                    onRetry = if (state.importUsername.isNotBlank()) onFetch else null
                )
            }

            // Player Profile & Ratings Card
            state.platformProfile?.let { profile ->
                DiggyCard(
                    elevated = true,
                    borderColor = StudioCyan.copy(alpha = 0.45f),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profile.title?.let { t ->
                                Surface(color = ClassMistake, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = t,
                                        color = OnAccentDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
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
                                        color = BgDark,
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
                                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
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
                            DiggyChip(
                                label = f.label,
                                selected = state.speedFilter == f,
                                onClick = { onSpeedFilter(f) },
                                accentColor = StudioCyan
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImportResultFilter.entries.forEach { f ->
                            DiggyChip(
                                label = f.label,
                                selected = state.resultFilter == f,
                                onClick = { onResultFilter(f) }
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(24.dp), color = BorderSubtle)
                        ImportColorFilter.entries.forEach { f ->
                            DiggyChip(
                                label = f.label,
                                selected = state.colorFilter == f,
                                onClick = { onColorFilter(f) }
                            )
                        }
                    }
                }
            }

            // Skeleton loader / Empty state / Virtualized list with pull-to-refresh
            when {
                state.isFetchingPlatform && state.allImportedGames.isEmpty() -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(4) {
                            DiggySkeletonBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp),
                                cornerRadius = DiggySpacing.cardRadius
                            )
                        }
                    }
                }
                state.allImportedGames.isEmpty() && state.platformError == null -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        DiggyEmptyState(
                            title = "Search any player",
                            subtitle = "Enter a Chess.com or Lichess username above to pull their latest games for coach review.",
                            icon = Icons.Rounded.CloudDownload
                        )
                    }
                }
                state.allImportedGames.isNotEmpty() && state.filteredImportedGames.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        DiggyEmptyState(
                            title = "No matching games",
                            subtitle = "No games match the active filters. Adjust Speed, Result, or Color above."
                        )
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isFetchingPlatform,
                        onRefresh = onFetch,
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
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
        "win" -> ClassBest
        "loss" -> ClassBlunder
        else -> ClassInaccuracy
    }
    val outcomeLabel = when (game.userOutcome) {
        "win" -> "WIN"
        "loss" -> "LOSS"
        else -> "DRAW"
    }

    DiggyCard(
        elevated = false,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("imported_game_${game.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    text = "White: ${game.whiteName}" + (game.whiteRating?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (game.userColor == "white") FontWeight.Bold else FontWeight.Normal,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Black: ${game.blackName}" + (game.blackRating?.let { " ($it)" } ?: ""),
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
                DiggyButton(
                    text = "Analyse",
                    onClick = onAnalyze,
                    icon = Icons.Rounded.Analytics,
                    testTag = "analyze_imported_${game.id}"
                )
            }
        }
    }
}
