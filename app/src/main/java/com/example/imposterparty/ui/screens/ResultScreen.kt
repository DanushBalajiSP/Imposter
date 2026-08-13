package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.ImposterMode
import com.example.imposterparty.data.model.Role
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun ResultScreen(
    gameViewModel: GameViewModel,
    onNextRound: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val sessionScores by gameViewModel.sessionScores.collectAsStateWithLifecycle()

    val accusedPlayer = gameState.players.find { it.id == gameState.accusedPlayerId }
    val imposters = gameState.players.filter { it.role == Role.IMPOSTER }

    // Dramatic entry animation
    val animScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "resultScale",
    )

    // Pulse for the result icon
    val infiniteTransition = rememberInfiniteTransition(label = "resultPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                if (gameState.isImposterFound)
                    listOf(DarkBackground, SuccessGreen.copy(alpha = 0.1f), DarkBackground)
                else
                    listOf(DarkBackground, DangerRed.copy(alpha = 0.1f), DarkBackground)
            )
        ),
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Result announcement
            item {
                Spacer(Modifier.height(16.dp))

                Text(
                    if (gameState.isImposterFound) "🎉" else "😈",
                    fontSize = 64.sp,
                    modifier = Modifier.scale(pulseScale),
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    if (gameState.isImposterFound) "IMPOSTER FOUND!" else "IMPOSTERS WIN!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (gameState.isImposterFound) SuccessGreen else DangerRed,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(animScale),
                )

                Spacer(Modifier.height(8.dp))

                if (accusedPlayer != null) {
                    Text(
                        "${accusedPlayer.name} was accused",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextOnDarkSecondary,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            // Secret word reveal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Text(
                            "The Secret Word",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnDarkSecondary,
                        )
                        Text(
                            gameState.secretWord,
                            style = MaterialTheme.typography.headlineMedium,
                            color = ImposterSecondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Imposter reveal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            "🕵️ The Imposter${if (imposters.size > 1) "s" else ""} ${if (imposters.size > 1) "were" else "was"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnDarkSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        imposters.forEach { imposter ->
                            Text(
                                imposter.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = DangerRed,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        // Reveal imposter count if auto-range mode + toggle enabled
                        if (
                            gameState.settings.imposterMode == ImposterMode.AUTO_RANGE &&
                            gameState.settings.revealImposterCountAtEnd
                        ) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "There ${if (gameState.actualImposterCount == 1) "was" else "were"} ${gameState.actualImposterCount} imposter${if (gameState.actualImposterCount > 1) "s" else ""} this round",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningYellow,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Scoring
            item {
                Text(
                    "📊 Round ${gameState.roundNumber} Scores",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextOnDark,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
            }

            // Score table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        ) {
                            Text("Player", color = TextOnDarkSecondary, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            Text("Role", color = TextOnDarkSecondary, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                            Text("Pts", color = TextOnDarkSecondary, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                            Text("Total", color = TextOnDarkSecondary, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                        }

                        HorizontalDivider(color = DarkSurfaceHigh)

                        gameState.players.forEach { player ->
                            val isImposter = player.role == Role.IMPOSTER
                            val roundPoints = if (gameState.isImposterFound) {
                                if (isImposter) 0 else 1
                            } else {
                                if (isImposter) 1 else 0
                            }
                            val totalPoints = sessionScores.getOrDefault(player.name, 0)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                            ) {
                                Text(
                                    player.name,
                                    color = TextOnDark,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isImposter) DangerRed.copy(alpha = 0.2f)
                                            else SuccessGreen.copy(alpha = 0.2f)
                                        )
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        if (isImposter) "Imposter" else "Civilian",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isImposter) DangerRed else SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(
                                    "+$roundPoints",
                                    color = if (roundPoints > 0) SuccessGreen else TextOnDarkSecondary,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "$totalPoints",
                                    color = ImposterSecondary,
                                    modifier = Modifier.width(50.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Home", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNextRound,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ImposterPrimary),
                    ) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Next Round", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
