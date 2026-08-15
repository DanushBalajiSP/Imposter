package com.example.imposterparty.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.MatchSession
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScoreboardScreen(
    gameViewModel: GameViewModel,
    onBack: () -> Unit,
    onResumeMatch: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val leaderboard by gameViewModel.leaderboard.collectAsStateWithLifecycle()
    val allMatches by gameViewModel.allMatches.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<String?>(null) }
    var matchToDelete by remember { mutableStateOf<MatchSession?>(null) }

    // Clear All Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Records") },
            text = { Text("This will permanently delete all merged scores and match history. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    gameViewModel.clearScoreHistory()
                    showClearAllDialog = false
                }) {
                    Text("Clear All", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Delete Specific Player Dialog
    playerToDelete?.let { playerName ->
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            title = { Text("Delete Player Score") },
            text = { Text("Are you sure you want to delete all score records for \"$playerName\"?") },
            confirmButton = {
                TextButton(onClick = {
                    gameViewModel.deletePlayerScores(playerName)
                    playerToDelete = null
                }) {
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Delete Specific Match Dialog
    matchToDelete?.let { match ->
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            title = { Text("Delete Match Record") },
            text = { Text("Are you sure you want to delete this match record (${match.totalRounds} ${if (match.totalRounds == 1) "round" else "rounds"})?") },
            confirmButton = {
                TextButton(onClick = {
                    gameViewModel.deleteMatchSession(match.id)
                    matchToDelete = null
                }) {
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(DarkBackground, DarkSurface))
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextOnDark)
                }
                Text(
                    "🏆 Scoreboard",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextOnDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (leaderboard.isNotEmpty() || allMatches.isNotEmpty()) {
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(Icons.Default.DeleteForever, "Clear All", tint = DangerRed.copy(alpha = 0.7f))
                    }
                }
            }

            // Tab selector
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = ImposterSecondary,
                divider = { HorizontalDivider(color = DarkSurfaceVariant) },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Leaderboard, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Leaderboard", fontWeight = FontWeight.Bold)
                        }
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Match History", fontWeight = FontWeight.Bold)
                        }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            if (selectedTab == 0) {
                // ── Leaderboard Tab ──
                if (leaderboard.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                null,
                                tint = TextOnDarkSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(80.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No scores yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextOnDarkSecondary,
                            )
                            Text(
                                "Play a round to start recording scores!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnDarkSecondary.copy(alpha = 0.7f),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text(
                                "All scores for same names are merged together",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnDarkSecondary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }

                        items(leaderboard) { entry ->
                            val medal = when (entry.rank) {
                                1 -> "🥇"
                                2 -> "🥈"
                                3 -> "🥉"
                                else -> null
                            }

                            val rankColor = when (entry.rank) {
                                1 -> Color(0xFFFFD700) // Gold
                                2 -> Color(0xFFC0C0C0) // Silver
                                3 -> Color(0xFFCD7F32) // Bronze
                                else -> TextOnDarkSecondary
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (entry.rank <= 3)
                                        rankColor.copy(alpha = 0.12f)
                                    else
                                        DarkSurfaceVariant.copy(alpha = 0.5f),
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = if (entry.rank <= 3) BorderStroke(1.dp, rankColor.copy(alpha = 0.4f)) else null,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                                ) {
                                    // Rank
                                    if (medal != null) {
                                        Text(
                                            medal,
                                            fontSize = 28.sp,
                                            modifier = Modifier.width(40.dp),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(DarkSurfaceHigh),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "#${entry.rank}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextOnDarkSecondary,
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            entry.playerName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = if (entry.rank <= 3) FontWeight.Black else FontWeight.Bold,
                                            ),
                                            color = TextOnDark,
                                        )
                                        val winRatePercent = if (entry.gamesPlayed > 0) (entry.gamesWon * 100 / entry.gamesPlayed) else 0
                                        Text(
                                            "Won ${entry.gamesWon}/${entry.gamesPlayed} rounds ($winRatePercent% win rate)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = TextOnDarkSecondary,
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${entry.totalPoints}",
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                            color = if (entry.rank <= 3) rankColor else ImposterSecondary,
                                        )
                                        Text(
                                            "total pts",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextOnDarkSecondary,
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Delete specific player score button
                                    IconButton(
                                        onClick = { playerToDelete = entry.playerName },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete player score",
                                            tint = DangerRed.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Match History Tab (Game-wise) ──
                if (allMatches.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.History,
                                null,
                                tint = TextOnDarkSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(80.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No match history yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextOnDarkSecondary,
                            )
                            Text(
                                "Completed matches will be saved and grouped here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnDarkSecondary.copy(alpha = 0.7f),
                            )
                        }
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(allMatches) { match ->
                            MatchCard(
                                match = match,
                                dateFormat = dateFormat,
                                onResume = {
                                    gameViewModel.resumeMatch(match)
                                    onResumeMatch()
                                },
                                onDeleteMatch = {
                                    matchToDelete = match
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchCard(
    match: MatchSession,
    dateFormat: SimpleDateFormat,
    onResume: () -> Unit,
    onDeleteMatch: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // Build complete player score list
    val allPlayersScores = remember(match) {
        val playerSet = LinkedHashSet<String>()
        playerSet.addAll(match.playerNames)
        playerSet.addAll(match.cumulativeScores.keys)
        playerSet.map { name ->
            name to (match.cumulativeScores[name] ?: 0)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🎮 Match (${match.totalRounds} ${if (match.totalRounds == 1) "Round" else "Rounds"})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextOnDark,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        dateFormat.format(Date(match.lastPlayedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnDarkSecondary,
                    )
                }

                // Continue Match Button
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ImposterSecondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Continue",
                        color = DarkBackground,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Delete specific match history button
                IconButton(
                    onClick = onDeleteMatch,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete match record",
                        tint = DangerRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Scores Summary in this match (wrapped cleanly using FlowRow)
            Text(
                "Match Scores:",
                style = MaterialTheme.typography.labelSmall,
                color = TextOnDarkSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                allPlayersScores.forEach { (player, score) ->
                    Surface(
                        color = DarkSurfaceHigh,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ImposterPrimary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                player,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnDark,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "$score pt${if (score != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ImposterSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Expandable round breakdown
            if (match.rounds.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        if (expanded) "Hide Rounds ▲" else "View ${match.rounds.size} Rounds ▼",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImposterSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        match.rounds.forEach { round ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHigh.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "Round ${round.roundNumber}: ${round.secretWord}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ImposterSecondary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            if (round.isImposterFound) "Civilians Won" else "Imposters Won",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (round.isImposterFound) SuccessGreen else DangerRed,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            "🕵️ Imposter: ${round.imposterNames.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DangerRed,
                                        )
                                        if (round.accusedPlayerName != null) {
                                            Text(
                                                "Accused: ${round.accusedPlayerName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextOnDarkSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
