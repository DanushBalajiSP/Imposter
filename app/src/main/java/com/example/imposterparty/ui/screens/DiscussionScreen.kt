package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

    // Haptic at 10 seconds
    LaunchedEffect(gameState.timerRemainingSeconds) {
        if (gameState.timerRemainingSeconds == 10) {
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
            .padding(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                "💬 Discussion Time",
                style = MaterialTheme.typography.headlineMedium,
                color = TextOnDark,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(24.dp))

            // Circular Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp),
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
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                        color = timerColor,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Speaker order
            Text(
                "Speaker Order",
                style = MaterialTheme.typography.titleMedium,
                color = TextOnDarkSecondary,
            )

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    gameState.discussionOrder.forEachIndexed { index, playerIdx ->
                        val player = gameState.players[playerIdx]
                        val isCurrent = index == gameState.currentSpeakerIndex
                        val isPast = index < gameState.currentSpeakerIndex

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) ImposterPrimary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent -> ImposterPrimary
                                            isPast -> SuccessGreen.copy(alpha = 0.3f)
                                            else -> DarkSurfaceHigh
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isCurrent) Color.White else TextOnDarkSecondary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                player.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = when {
                                    isCurrent -> ImposterPrimary
                                    isPast -> TextOnDarkSecondary
                                    else -> TextOnDark
                                },
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (isCurrent) {
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "Speaking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ImposterSecondary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (gameState.currentSpeakerIndex < gameState.discussionOrder.size - 1) {
                    OutlinedButton(
                        onClick = { gameViewModel.advanceSpeaker() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ImposterSecondary,
                        ),
                    ) {
                        Text("Next Speaker", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }

                Button(
                    onClick = onEndDiscussion,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                ) {
                    Icon(Icons.Default.HowToVote, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Vote Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
