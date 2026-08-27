package com.example.imposterparty.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // ── Dialogs ──
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = StitchSurfaceContainer,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Clear All Records",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            },
            text = {
                Text(
                    "This will permanently delete all merged scores and match history. Are you sure?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        gameViewModel.clearScoreHistory()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    playerToDelete?.let { playerName ->
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            containerColor = StitchSurfaceContainer,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Delete Player Score",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete all score records for \"$playerName\"?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        gameViewModel.deletePlayerScores(playerName)
                        playerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Cancel", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    matchToDelete?.let { match ->
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            containerColor = StitchSurfaceContainer,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Delete Match Record",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this match record (${match.totalRounds} ${if (match.totalRounds == 1) "round" else "rounds"})?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        gameViewModel.deleteMatchSession(match.id)
                        matchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) {
                    Text("Cancel", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    Box(
        modifier = modifier
            .background(DeepSpaceBg)
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp),
        ) {
            // ── Top App Bar ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceContainerHigh.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back",
                        tint = OnSurfaceVariant,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("🏆", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Scoreboard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            fontSize = 24.sp,
                        ),
                        color = PrimaryContainerNeon,
                    )
                }

                if (leaderboard.isNotEmpty() || allMatches.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DangerRed.copy(alpha = 0.12f)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All",
                            tint = DangerRed,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Tabs Segmented Control ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(0.dp, Color.Transparent),
                    ),
            ) {
                // Leaderboard Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 0 }
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = if (selectedTab == 0) NeonCyanSoft else OnSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Leaderboard",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (selectedTab == 0) NeonCyanSoft else OnSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(
                                if (selectedTab == 0) Brush.horizontalGradient(listOf(NeonPurpleGlow, NeonPurple))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            ),
                    )
                }

                // Match History Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 1 }
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (selectedTab == 1) NeonCyanSoft else OnSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Match History",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (selectedTab == 1) NeonCyanSoft else OnSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(
                                if (selectedTab == 1) Brush.horizontalGradient(listOf(NeonPurpleGlow, NeonPurple))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            ),
                    )
                }
            }

            HorizontalDivider(
                color = OutlineSubtle.copy(alpha = 0.25f),
                thickness = 1.dp,
            )

            // ── Content Views ──
            if (selectedTab == 0) {
                // ── Leaderboard Tab ──
                Text(
                    text = "All scores for same names are merged together",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                )

                if (leaderboard.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = OnSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp),
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "No scores yet",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Play a round to start recording scores!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(leaderboard, key = { it.playerName }) { entry ->
                            val isGold = entry.rank == 1
                            val isSilver = entry.rank == 2
                            val isBronze = entry.rank == 3

                            val borderColor = when {
                                isGold -> NeonGold.copy(alpha = 0.6f)
                                isSilver -> NeonCyan.copy(alpha = 0.5f)
                                isBronze -> ImposterTertiary.copy(alpha = 0.5f)
                                else -> OutlineSubtle.copy(alpha = 0.3f)
                            }

                            val scoreColor = when {
                                isGold -> NeonGold
                                isSilver -> NeonCyan
                                isBronze -> ImposterTertiary
                                else -> NeonCyanSoft
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StitchSurfaceContainer)
                                    .border(
                                        BorderStroke(1.dp, borderColor),
                                        RoundedCornerShape(16.dp),
                                    )
                                    .padding(14.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    // Rank Badge / Icon
                                    if (isGold || isSilver || isBronze) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(scoreColor.copy(alpha = 0.15f))
                                                .border(BorderStroke(1.dp, scoreColor.copy(alpha = 0.4f)), CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = when (entry.rank) {
                                                    1 -> "1"
                                                    2 -> "2"
                                                    else -> "3"
                                                },
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                ),
                                                color = scoreColor,
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(StitchSurfaceContainerHigh)
                                                .border(BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)), CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "#${entry.rank}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = OnSurfaceVariant,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    // Player details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.playerName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                            ),
                                            color = Color.White,
                                        )
                                        val winRatePercent = if (entry.gamesPlayed > 0) (entry.gamesWon * 100 / entry.gamesPlayed) else 0
                                        Text(
                                            text = "Won ${entry.gamesWon}/${entry.gamesPlayed} rounds ($winRatePercent% win rate)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceVariant.copy(alpha = 0.75f),
                                        )
                                    }

                                    // Points & Label
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${entry.totalPoints}",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 22.sp,
                                            ),
                                            color = scoreColor,
                                        )
                                        Text(
                                            text = "total pts",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                            ),
                                            color = OnSurfaceVariant.copy(alpha = 0.6f),
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // Delete player score button
                                    IconButton(
                                        onClick = { playerToDelete = entry.playerName },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete score",
                                            tint = DangerRed.copy(alpha = 0.65f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Match History Tab ──
                Spacer(Modifier.height(14.dp))

                if (allMatches.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = OnSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp),
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "No match history yet",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Completed matches will be saved and grouped here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                            )
                        }
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(allMatches, key = { it.id }) { match ->
                            StitchMatchCard(
                                match = match,
                                dateFormat = dateFormat,
                                onResume = {
                                    gameViewModel.resumeMatch(match)
                                    onResumeMatch()
                                },
                                onDeleteMatch = {
                                    matchToDelete = match
                                },
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
private fun StitchMatchCard(
    match: MatchSession,
    dateFormat: SimpleDateFormat,
    onResume: () -> Unit,
    onDeleteMatch: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val allPlayersScores = remember(match) {
        val playerSet = LinkedHashSet<String>()
        playerSet.addAll(match.playerNames)
        playerSet.addAll(match.cumulativeScores.keys)
        playerSet.map { name ->
            name to (match.cumulativeScores[name] ?: 0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StitchSurfaceContainer)
            .border(
                BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)),
                RoundedCornerShape(18.dp),
            )
            .padding(16.dp),
    ) {
        Column {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎮 Match (${match.totalRounds} ${if (match.totalRounds == 1) "Round" else "Rounds"})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = dateFormat.format(Date(match.lastPlayedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                // Continue Button
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DeepSpaceBg,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Continue",
                        color = DeepSpaceBg,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Delete Button
                IconButton(
                    onClick = onDeleteMatch,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete match",
                        tint = DangerRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Scores Wrap Badges
            Text(
                text = "Match Scores:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurfaceVariant,
            )

            Spacer(Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                allPlayersScores.forEach { (player, score) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchSurfaceContainerHigh)
                            .border(
                                BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.25f)),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$score pt${if (score != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonCyan,
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
                        text = if (expanded) "Hide Rounds ▲" else "View ${match.rounds.size} Rounds ▼",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = NeonCyan,
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StitchSurfaceContainerHigh.copy(alpha = 0.6f))
                                    .border(
                                        BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.2f)),
                                        RoundedCornerShape(10.dp),
                                    )
                                    .padding(12.dp),
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "Round ${round.roundNumber}: ${round.secretWord}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = NeonCyanSoft,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = if (round.isImposterFound) "Civilians Won" else "Imposters Won",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = if (round.isImposterFound) SuccessGreen else DangerRed,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "🕵️ Imposter: ${round.imposterNames.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DangerRed,
                                        )
                                        if (round.accusedPlayerName != null) {
                                            Text(
                                                text = "Accused: ${round.accusedPlayerName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant,
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
