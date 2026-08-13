package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.GamePhase
import com.example.imposterparty.data.model.Role
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun CardRevealScreen(
    gameViewModel: GameViewModel,
    onAllRevealed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val currentPlayer = gameViewModel.getCurrentRevealPlayer()
    var isRevealing by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Watch for phase transition to Discussion
    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.DISCUSSION) {
            onAllRevealed()
        }
    }

    // Card flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isRevealing) 180f else 0f,
        animationSpec = tween(400, easing = EaseInOutCubic),
        label = "cardFlip",
    )

    // Pulse animation for the hold hint
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(DarkBackground, GradientMid, DarkBackground)))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Progress indicator
            Text(
                "Player ${gameState.currentRevealIndex + 1} of ${gameState.players.size}",
                style = MaterialTheme.typography.titleMedium,
                color = TextOnDarkSecondary,
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (gameState.currentRevealIndex.toFloat()) / gameState.players.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = ImposterPrimary,
                trackColor = DarkSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            if (currentPlayer != null) {
                // Pass instruction
                Text(
                    "Pass the phone to",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextOnDarkSecondary,
                )
                Text(
                    currentPlayer.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = ImposterSecondary,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(32.dp))

                // The Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .scale(if (!isRevealing) pulseScale else 1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (rotation <= 90f) {
                                Brush.verticalGradient(
                                    listOf(DarkSurfaceVariant, DarkSurfaceHigh)
                                )
                            } else {
                                if (currentPlayer.role == Role.IMPOSTER) {
                                    Brush.verticalGradient(
                                        listOf(DangerRed.copy(alpha = 0.8f), DangerRed.copy(alpha = 0.4f))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(ImposterPrimary.copy(alpha = 0.6f), ImposterPrimaryDark.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        )
                        .pointerInput(currentPlayer.id) {
                            detectTapGestures(
                                onPress = {
                                    isRevealing = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    tryAwaitRelease()
                                    isRevealing = false
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (rotation <= 90f) {
                        // Card front (hidden)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔒", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Hold to Reveal",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextOnDark,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Release to hide",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnDarkSecondary,
                            )
                        }
                    } else {
                        // Card back (revealed) — mirrored so text reads correctly
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .graphicsLayer { rotationY = 180f }
                                .padding(24.dp),
                        ) {
                            if (currentPlayer.role == Role.IMPOSTER) {
                                Text("🕵️", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "You are the",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Text(
                                    "IMPOSTER",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "You don't know the secret word!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                Text("✅", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "The secret word is",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Text(
                                    gameState.secretWord,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                )
                                if (!gameState.secretClue.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "💡 Clue: ${gameState.secretClue}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WarningYellow.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Next Player button
                Button(
                    onClick = {
                        isRevealing = false
                        gameViewModel.markCurrentPlayerRevealed()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ImposterPrimary),
                ) {
                    Text(
                        if (gameState.currentRevealIndex >= gameState.players.size - 1)
                            "Start Discussion" else "Next Player",
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}
