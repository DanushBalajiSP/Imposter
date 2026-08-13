package com.example.imposterparty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.GamePhase
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun VotingScreen(
    gameViewModel: GameViewModel,
    onVotingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val currentVoter = gameViewModel.getCurrentVoter()
    var selectedPlayerId by remember(gameState.currentVoterIndex) { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    // Watch for phase transition to Result
    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.RESULT) {
            onVotingComplete()
        }
    }

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(DarkBackground, GradientEnd, DarkBackground)))
            .padding(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header
            Text(
                "🗳️ Voting",
                style = MaterialTheme.typography.headlineMedium,
                color = TextOnDark,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Player ${gameState.currentVoterIndex + 1} of ${gameState.players.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextOnDarkSecondary,
            )

            Spacer(Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { gameState.currentVoterIndex.toFloat() / gameState.players.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = ImposterPrimary,
                trackColor = DarkSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            if (currentVoter != null) {
                // Current voter info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            "Pass phone to",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnDarkSecondary,
                        )
                        Text(
                            currentVoter.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = ImposterSecondary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Who do you think is the Imposter?",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnDarkSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Player list to vote for
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    val otherPlayers = gameState.players.filter { it.id != currentVoter.id }
                    items(otherPlayers) { player ->
                        val isSelected = selectedPlayerId == player.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPlayerId = player.id
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ImposterPrimary.copy(alpha = 0.2f)
                                else DarkSurfaceVariant.copy(alpha = 0.4f),
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                width = 2.dp,
                                brush = Brush.horizontalGradient(listOf(ImposterPrimary, ImposterSecondary)),
                            ) else null,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) ImposterPrimary
                                            else DarkSurfaceHigh
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    } else {
                                        Text(
                                            player.name.first().uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextOnDarkSecondary,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    player.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) ImposterPrimary else TextOnDark,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Cast vote button
                Button(
                    onClick = {
                        selectedPlayerId?.let { id ->
                            gameViewModel.castVote(id)
                            selectedPlayerId = null
                        }
                    },
                    enabled = selectedPlayerId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImposterPrimary,
                        disabledContainerColor = DarkSurfaceVariant,
                    ),
                ) {
                    Icon(Icons.Default.HowToVote, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cast Vote", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
