package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun DiscussionScreen(
    gameViewModel: GameViewModel,
    onEndDiscussion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val startingSpeaker = gameState.players.getOrNull(gameState.startingSpeakerIndex)
        ?: gameState.players.firstOrNull()

    // Haptic at 10 seconds (only if timer is enabled)
    LaunchedEffect(gameState.timerRemainingSeconds, gameState.settings.isTimerEnabled) {
        if (gameState.settings.isTimerEnabled && gameState.timerRemainingSeconds == 10) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val totalSeconds = when (gameState.settings.timerDuration) {
        com.example.imposterparty.data.model.TimerDuration.CUSTOM -> gameState.settings.customTimerSeconds
        else -> gameState.settings.timerDuration.seconds
    }
    val progress = if (totalSeconds > 0) gameState.timerRemainingSeconds.toFloat() / totalSeconds else 0f

    // Timer color transitions
    val timerColor = when {
        progress > 0.5f -> NeonCyan
        progress > 0.2f -> NeonGold
        else -> DangerRed
    }

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
            // ── Header ──
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = if (gameState.isSubRound) DangerRed else PrimaryContainerNeon,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (gameState.isSubRound) "SUB-ROUND DISCUSSION" else "DISCUSSION TIME",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                    ),
                    color = if (gameState.isSubRound) DangerRed else Color.White,
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (gameState.isSubRound) "Final chance to find the remaining Imposter!" else "Who is the Imposter?",
                style = MaterialTheme.typography.bodyMedium,
                color = if (gameState.isSubRound) WarningYellow else OnSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            // ── Scrollable Content ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                if (gameState.settings.isTimerEnabled) {
                    // Circular Timer
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(190.dp)
                            .padding(8.dp),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 12.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val topLeft = Offset(
                                (size.width - radius * 2) / 2,
                                (size.height - radius * 2) / 2,
                            )

                            // Background track
                            drawArc(
                                color = StitchSurfaceContainerHigh,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )

                            // Progress arc
                            drawArc(
                                color = timerColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val minutes = gameState.timerRemainingSeconds / 60
                            val seconds = gameState.timerRemainingSeconds % 60
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                ),
                                color = timerColor,
                            )
                            Text(
                                text = "REMAINING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                ),
                                color = OnSurfaceVariant,
                            )
                        }
                    }
                } else {
                    // Untimed Mode Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(StitchSurfaceContainer)
                            .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("♾️", fontSize = 36.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Free Discussion Mode",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = NeonCyanSoft,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Take your time to question players and discuss clues. When ready, tap 'Vote Now' below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Starting Speaker Highlight Card ──
                if (startingSpeaker != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(StitchSurfaceContainer)
                            .border(BorderStroke(1.5.dp, NeonPurple.copy(alpha = 0.7f)), RoundedCornerShape(20.dp))
                            .padding(20.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple))
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "STARTS THE CONVERSATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                ),
                                color = OnSurfaceVariant,
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = startingSpeaker.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = PrimaryContainerNeon,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "${startingSpeaker.name} gives the first clue or statement, then everyone discusses freely!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(110.dp))
            }
        }

        // ── Fixed Bottom Action Bar (Vote Now) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            StitchSurfaceContainerHigh.copy(alpha = 0.85f),
                            StitchSurfaceContainerHigh,
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Button(
                onClick = onEndDiscussion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple)),
                            shape = RoundedCornerShape(9999.dp),
                        )
                        .border(
                            BorderStroke(1.dp, PrimaryContainerNeon.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(9999.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Vote Now",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
