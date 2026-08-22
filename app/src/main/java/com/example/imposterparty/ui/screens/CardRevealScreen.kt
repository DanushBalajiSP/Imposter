package com.example.imposterparty.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

    // Watch for phase transition to Discussion or Sub-Round Discussion
    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.DISCUSSION || gameState.phase == GamePhase.SUB_ROUND_DISCUSSION) {
            onAllRevealed()
        }
    }

    // Card flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isRevealing) 180f else 0f,
        animationSpec = tween(400, easing = EaseInOutCubic),
        label = "cardFlip",
    )

    // Pulse animation for the hold hint and lock icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val instructionAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "instructionAlpha",
    )

    Box(
        modifier = modifier
            .background(DeepSpaceBg)
            .fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            val totalRevealCount = if (gameState.revealOrder.isNotEmpty()) gameState.revealOrder.size else gameState.players.size
            val currentRevealNum = (gameState.currentRevealIndex + 1).coerceAtMost(totalRevealCount)

            Text(
                text = if (gameState.isSubRound) {
                    "SUB-ROUND ${gameState.subRoundNumber} • PLAYER $currentRevealNum OF $totalRevealCount"
                } else {
                    "PLAYER $currentRevealNum OF $totalRevealCount"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp,
                ),
                color = if (gameState.isSubRound) WarningYellow else PrimaryContainerNeon,
            )

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { currentRevealNum.toFloat() / totalRevealCount.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (gameState.isSubRound) DangerRed else NeonPurple,
                trackColor = StitchSurfaceContainerHigh,
            )

            Spacer(Modifier.height(24.dp))

            if (currentPlayer != null) {
                // ── Player Turn Info ──
                Text(
                    text = "Pass the phone to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = currentPlayer.name,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        fontSize = 32.sp,
                    ),
                    color = NeonCyanSoft,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                // ── Interactive Card Container ──
                val cardShape = RoundedCornerShape(24.dp)
                val isBackSide = rotation > 90f
                val isImposter = currentPlayer.role == Role.IMPOSTER

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .scale(if (!isRevealing) pulseScale else 1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clip(cardShape)
                        .border(
                            BorderStroke(
                                width = if (isBackSide) 2.dp else 1.dp,
                                color = if (isBackSide) {
                                    if (isImposter) DangerRed else NeonCyan
                                } else {
                                    OutlineSubtle.copy(alpha = 0.35f)
                                }
                            ),
                            shape = cardShape,
                        )
                        .background(
                            if (isBackSide) {
                                if (isImposter) {
                                    Brush.verticalGradient(
                                        listOf(
                                            DangerRed.copy(alpha = 0.15f),
                                            StitchSurfaceContainer,
                                            StitchSurfaceContainer,
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            NeonCyan.copy(alpha = 0.12f),
                                            StitchSurfaceContainer,
                                            StitchSurfaceContainer,
                                        )
                                    )
                                }
                            } else {
                                Brush.verticalGradient(
                                    listOf(StitchSurfaceContainerHigh, StitchSurfaceContainer)
                                )
                            }
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
                        // ── Card Front (Hidden) ──
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeonGold,
                                modifier = Modifier
                                    .size(56.dp)
                                    .scale(pulseScale),
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Hold to Reveal",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                ),
                                color = Color.White,
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "Keep screen hidden from others",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        // ── Card Back (Revealed) ──
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .graphicsLayer { rotationY = 180f }
                                .padding(24.dp),
                        ) {
                            if (isImposter) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(48.dp),
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "You are the",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                )

                                Text(
                                    text = "IMPOSTER",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        fontSize = 36.sp,
                                    ),
                                    color = DangerRed,
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "Blend in. Guess the word.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )

                                // Clue exclusively for imposter
                                if (!gameState.secretClue.isNullOrBlank()) {
                                    Spacer(Modifier.height(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(StitchSurfaceContainerHigh)
                                            .border(
                                                BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                                                RoundedCornerShape(10.dp),
                                            )
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = NeonGold,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Clue: ${gameState.secretClue}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = DangerRed,
                                            )
                                        }
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(48.dp),
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "The secret word is",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                )

                                Text(
                                    text = gameState.secretWord.uppercase(),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 32.sp,
                                    ),
                                    color = NeonCyanSoft,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "Find the imposter among you.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Instructional Text Below Card ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.height(32.dp),
                ) {
                    if (!hasCurrentPlayerOpenedCard) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = NeonGold.copy(alpha = instructionAlpha),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Press and hold the card above to reveal your role",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = NeonGold.copy(alpha = instructionAlpha),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Fixed Bottom Action Button (Next Player / Start Discussion) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            DeepSpaceBg.copy(alpha = 0.85f),
                            DeepSpaceBg,
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            val totalRevealCount = if (gameState.revealOrder.isNotEmpty()) gameState.revealOrder.size else gameState.players.size
            val isReady = hasCurrentPlayerOpenedCard
            val isLastPlayer = gameState.currentRevealIndex >= totalRevealCount - 1

            Button(
                onClick = {
                    if (isReady) {
                        isRevealing = false
                        gameViewModel.markCurrentPlayerRevealed()
                    }
                },
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = StitchSurfaceContainerHigh,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isReady) {
                                Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple))
                            } else {
                                SolidColor(StitchSurfaceContainerHigh)
                            }
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isReady) PrimaryContainerNeon.copy(alpha = 0.4f) else OutlineSubtle.copy(alpha = 0.2f),
                            ),
                            shape = RoundedCornerShape(9999.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (isLastPlayer) {
                                if (gameState.isSubRound) "Start Sub-Round Discussion" else "Start Discussion"
                            } else "Next Player",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                            color = if (isReady) Color.White else OnSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isReady) Color.White else OnSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
