package com.example.imposterparty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun ScoreboardScreen(
    gameViewModel: GameViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaderboard by gameViewModel.leaderboard.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Scores") },
            text = { Text("This will permanently delete all score history. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    gameViewModel.clearScoreHistory()
                    showClearDialog = false
                }) {
                    Text("Clear All", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
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
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
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
                if (leaderboard.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteForever, "Clear", tint = DangerRed.copy(alpha = 0.7f))
                    }
                }
            }

            if (leaderboard.isEmpty()) {
                // Empty state
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
                            "Play a game to see the leaderboard!",
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
                    itemsIndexed(leaderboard) { index, entry ->
                        val medal = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> null
                        }

                        val rankColor = when (index) {
                            0 -> Color(0xFFFFD700) // Gold
                            1 -> Color(0xFFC0C0C0) // Silver
                            2 -> Color(0xFFCD7F32) // Bronze
                            else -> TextOnDarkSecondary
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (index < 3)
                                    rankColor.copy(alpha = 0.1f)
                                else
                                    DarkSurfaceVariant.copy(alpha = 0.5f),
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                                            "#${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextOnDarkSecondary,
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                }

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    entry.playerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextOnDark,
                                    fontWeight = if (index < 3) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                )

                                Text(
                                    "${entry.totalPoints}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = rankColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "pts",
                                    style = MaterialTheme.typography.labelSmall,
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
