package com.example.imposterparty.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    var hasCurrentPlayerOpenedCard by remember(gameState.currentRevealIndex) { mutableStateOf(false) }
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
        targetValue = 1.04f,
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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                ),
                color = ImposterPrimaryLight,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (gameState.currentRevealIndex.toFloat()) / gameState.players.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ImposterPrimary,
                trackColor = DarkSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            if (currentPlayer != null) {
                // Pass instruction
                Text(
                    "Pass the phone to",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = TextOnDarkSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    currentPlayer.name,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    ),
                    color = ImposterSecondary,
                )

                Spacer(Modifier.height(28.dp))

                // The Card
                val cardShape = RoundedCornerShape(26.dp)
                val isBackSide = rotation > 90f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .scale(if (!isRevealing) pulseScale else 1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clip(cardShape)
                        .border(
                            BorderStroke(
                                width = if (isBackSide) 3.dp else 1.5.dp,
                                color = if (isBackSide) {
                                    if (currentPlayer.role == Role.IMPOSTER) DangerRed else ImposterPrimary
                                } else {
                                    DarkSurfaceHigh
                                }
                            ),
                            shape = cardShape
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(DarkSurfaceVariant, DarkSurfaceHigh)
                            )
                        )
                        .pointerInput(currentPlayer.id, gameState.currentRevealIndex) {
                            detectTapGestures(
                                onPress = {
                                    isRevealing = true
                                    hasCurrentPlayerOpenedCard = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    tryAwaitRelease()
                                    isRevealing = false
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isBackSide) {
                        // Card front (hidden)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔒", fontSize = 52.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Hold to Reveal",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                ),
                                color = TextOnDark,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Keep screen hidden from others",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
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
                                Text("🕵️", fontSize = 52.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "You are the",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = TextOnDarkSecondary,
                                )
                                Text(
                                    "IMPOSTER",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 3.sp,
                                    ),
                                    color = DangerRed,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "You don't know the secret word!",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = TextOnDarkSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                // Clue exclusively for imposter
                                if (!gameState.secretClue.isNullOrBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Surface(
                                        color = DangerRed.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.5.dp, DangerRed.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            "💡 Clue: ${gameState.secretClue}",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Black,
                                            ),
                                            color = WarningYellow,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            } else {
                                Text("✅", fontSize = 52.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "The secret word is",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = TextOnDarkSecondary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    gameState.secretWord,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                    ),
                                    color = ImposterSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Next Player button (Shown ONLY after the current player has opened their card at least once)
                AnimatedVisibility(
                    visible = hasCurrentPlayerOpenedCard,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Button(
                        onClick = {
                            isRevealing = false
                            gameViewModel.markCurrentPlayerRevealed()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ImposterPrimary),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                    ) {
                        Text(
                            if (gameState.currentRevealIndex >= gameState.players.size - 1)
                                "Start Discussion" else "Next Player",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(22.dp))
                    }
                }

                AnimatedVisibility(
                    visible = !hasCurrentPlayerOpenedCard,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        "👆 Press and hold the card above to reveal your role",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = WarningYellow,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
