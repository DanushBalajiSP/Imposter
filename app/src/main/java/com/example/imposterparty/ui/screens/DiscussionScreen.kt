package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToVote
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

    // Timer color transitions from green to yellow to red
    val timerColor = when {
        progress > 0.5f -> SuccessGreen
        progress > 0.2f -> WarningYellow
        else -> DangerRed
    }

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(DarkBackground, GradientMid, DarkBackground)))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header
            Text(
                "💬 Discussion Time",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                ),
                color = TextOnDark,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

            // Scrollable Content
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
                            val strokeWidth = 14.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val topLeft = Offset(
                                (size.width - radius * 2) / 2,
                                (size.height - radius * 2) / 2,
                            )

                            // Background track
                            drawArc(
                                color = DarkSurfaceVariant,
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
                                "%02d:%02d".format(minutes, seconds),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                ),
                                color = timerColor,
                            )
                            Text(
                                "REMAINING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                ),
                                color = TextOnDarkSecondary,
                            )
                        }
                    }
                } else {
                    // Untimed Discussion Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, ImposterSecondary.copy(alpha = 0.4f)),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Text("♾️", fontSize = 44.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Free Discussion Mode",
                                style = MaterialTheme.typography.titleLarge,
                                color = ImposterSecondary,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Take your time to question players and discuss clues. When ready, tap 'Vote Now' below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnDarkSecondary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Single Starting Speaker Highlight Card (Randomly Selected)
                if (startingSpeaker != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.75f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, ImposterSecondary.copy(alpha = 0.7f)),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(listOf(ImposterSecondary, ImposterSecondaryDark))
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("🎤", fontSize = 26.sp)
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "STARTS THE CONVERSATION",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                ),
                                color = TextOnDarkSecondary,
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                startingSpeaker.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                ),
                                color = ImposterSecondary,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "${startingSpeaker.name} gives the first clue or statement, then everyone discusses freely!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = TextOnDarkSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // Fixed Bottom Action Button (Vote Now)
            Surface(
                color = DarkBackground.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Button(
                    onClick = onEndDiscussion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                ) {
                    Icon(Icons.Default.HowToVote, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Vote Now",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
