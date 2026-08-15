package com.example.imposterparty.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
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
                "🗳️ Secret Voting",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                ),
                color = TextOnDark,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Voter ${gameState.currentVoterIndex + 1} of ${gameState.players.size}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = ImposterPrimaryLight,
            )

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (gameState.currentVoterIndex.toFloat()) / gameState.players.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ImposterPrimary,
                trackColor = DarkSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            if (currentVoter != null) {
                // Current voter info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, ImposterSecondary.copy(alpha = 0.3f)),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            "Pass phone to",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = TextOnDarkSecondary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            currentVoter.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                            ),
                            color = ImposterSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Who do you think is the Imposter?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = WarningYellow,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Player list to vote for
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                containerColor = if (isSelected) ImposterPrimary.copy(alpha = 0.25f)
                                else DarkSurfaceVariant.copy(alpha = 0.5f),
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) ImposterSecondary else DarkSurfaceHigh,
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) ImposterSecondary
                                             else DarkSurfaceHigh
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = DarkBackground,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    } else {
                                        Text(
                                            player.name.first().uppercase(),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                            ),
                                            color = TextOnDark,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    player.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    ),
                                    color = if (isSelected) ImposterSecondary else TextOnDark,
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
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        disabledContainerColor = DarkSurfaceVariant,
                    ),
                    border = BorderStroke(
                        width = if (selectedPlayerId != null) 1.5.dp else 0.dp,
                        color = if (selectedPlayerId != null) Color.White.copy(alpha = 0.4f) else Color.Transparent
                    ),
                ) {
                    Icon(Icons.Default.HowToVote, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Submit Secret Vote",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
