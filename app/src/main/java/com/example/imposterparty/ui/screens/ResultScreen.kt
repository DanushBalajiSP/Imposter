package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.ImposterMode
import com.example.imposterparty.data.model.Role
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel
import kotlin.math.sin
import kotlin.random.Random

// ── Floating confetti particle data class ──
private data class ResultParticle(
    val xFraction: Float,
    val size: Float,
    val speed: Float,
    val delay: Float,
    val color: Color,
)

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
    val isCivilianWin = gameState.isImposterFound

    // ── Animations ──
    val infiniteTransition = rememberInfiniteTransition(label = "resultPulse")

    // Pulse animation for header badge/icon
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // Floating particles ticker
    val particleTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particleTick",
    )

    val particles = remember {
        val colors = listOf(NeonCyan, NeonPurpleGlow, NeonGold, DangerRed)
        List(30) {
            ResultParticle(
                xFraction = Random.nextFloat(),
                size = Random.nextFloat() * 2.5f + 1.5f,
                speed = Random.nextFloat() * 12f + 8f,
                delay = Random.nextFloat() * 8f,
                color = colors[Random.nextInt(colors.size)],
            )
        }
    }

    Box(
        modifier = modifier
            .background(DeepSpaceBg)
            .fillMaxSize()
            .drawBehind {
                // Floating celebration particles
                particles.forEach { p ->
                    val progress = ((particleTick * 20f + p.delay) % p.speed) / p.speed
                    val y = size.height * (1f - progress)
                    val x = size.width * p.xFraction
                    val alpha = sin(progress * Math.PI.toFloat()).coerceIn(0f, 1f) * 0.7f
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.size * density,
                        center = Offset(x, y),
                    )
                }
            },
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp),
        ) {
            // ── Celebration Icon ──
            item {
                Spacer(Modifier.height(12.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp),
                ) {
                    // Glow behind icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isCivilianWin) NeonGold.copy(alpha = 0.35f) else DangerRed.copy(alpha = 0.35f),
                                        Color.Transparent,
                                    )
                                )
                            )
                    )

                    if (isCivilianWin) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = "Celebration",
                            tint = NeonGold,
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulseScale),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SentimentVeryDissatisfied,
                            contentDescription = "Imposters Win",
                            tint = DangerRed,
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulseScale),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Main Result Announcement ──
                Text(
                    text = if (isCivilianWin) "CIVILIANS WIN!" else "IMPOSTERS WIN!",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        fontSize = 36.sp,
                    ),
                    color = if (isCivilianWin) NeonCyanSoft else DangerRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    (if (isCivilianWin) NeonCyan else DangerRed).copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.width * 0.7f,
                            ),
                            radius = size.width * 0.7f,
                            center = center,
                        )
                    },
                )

                Spacer(Modifier.height(10.dp))

                // ── Accused Pill ──
                if (accusedPlayer != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(StitchSurfaceContainerHigh)
                            .border(
                                BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.35f)),
                                RoundedCornerShape(9999.dp),
                            )
                            .padding(horizontal = 18.dp, vertical = 7.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Accused: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                            )
                            Text(
                                text = accusedPlayer.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // ── The Secret Word Card ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StitchSurfaceContainer)
                        .border(
                            BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.8f)),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "THE SECRET WORD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontSize = 11.sp,
                            ),
                            color = OnSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = gameState.secretWord.uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                fontSize = 26.sp,
                            ),
                            color = NeonCyanSoft,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // ── The Imposters Reveal Card ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StitchSurfaceContainer)
                        .border(
                            BorderStroke(1.5.dp, DangerRed.copy(alpha = 0.8f)),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(18.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "👺",
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "THE IMPOSTER${if (imposters.size > 1) "S WERE" else " WAS"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontSize = 11.sp,
                                ),
                                color = OnSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        imposters.forEach { imposter ->
                            Text(
                                text = imposter.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                ),
                                color = DangerRed,
                            )
                        }

                        // Reveal imposter count if auto-range mode + toggle enabled
                        if (
                            gameState.settings.imposterMode == ImposterMode.AUTO_RANGE &&
                            gameState.settings.revealImposterCountAtEnd
                        ) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "There ${if (gameState.actualImposterCount == 1) "was" else "were"} ${gameState.actualImposterCount} imposter${if (gameState.actualImposterCount > 1) "s" else ""} this round",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = NeonGold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // ── Scoreboard Header ──
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Round ${gameState.roundNumber} Scores",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                        color = Color.White,
                    )
                }

                Spacer(Modifier.height(10.dp))
            }

            // ── Scoreboard Table ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StitchSurfaceContainerHigh)
                        .border(
                            BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.35f)),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(16.dp),
                ) {
                    Column {
                        // Table Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                        ) {
                            Text(
                                text = "PLAYER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp,
                                ),
                                color = OnSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "ROLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp,
                                ),
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(90.dp),
                            )
                            Text(
                                text = "PTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp,
                                ),
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(44.dp),
                            )
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp,
                                ),
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(50.dp),
                            )
                        }

                        HorizontalDivider(
                            color = OutlineSubtle.copy(alpha = 0.3f),
                            thickness = 1.dp,
                        )

                        // Player Rows
                        gameState.players.forEachIndexed { index, player ->
                            val isImposter = player.role == Role.IMPOSTER
                            val roundPoints = if (isCivilianWin) {
                                if (isImposter) 0 else 1
                            } else {
                                if (isImposter) 1 else 0
                            }
                            val totalPoints = sessionScores.getOrDefault(player.name, 0)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                            ) {
                                // Player Name
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )

                                // Role Badge Pill
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isImposter) DangerRed.copy(alpha = 0.12f) else NeonCyan.copy(alpha = 0.12f)
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isImposter) DangerRed.copy(alpha = 0.45f) else NeonCyan.copy(alpha = 0.45f),
                                            ),
                                            RoundedCornerShape(6.dp),
                                        )
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = (if (isImposter) "Imposter" else "Civilian").uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = if (isImposter) DangerRed else NeonCyanSoft,
                                    )
                                }

                                // Round Pts
                                Text(
                                    text = "+$roundPoints",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    ),
                                    color = if (roundPoints > 0) NeonCyan else OnSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(44.dp),
                                )

                                // Total Score
                                Text(
                                    text = "$totalPoints",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                    ),
                                    color = NeonCyan,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(50.dp),
                                )
                            }

                            if (index < gameState.players.size - 1) {
                                HorizontalDivider(
                                    color = StitchSurfaceBright.copy(alpha = 0.4f),
                                    thickness = 0.8.dp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Fixed Bottom Action Bar ──
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Home Button (Pill shape)
                Button(
                    onClick = onHome,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSurfaceContainerHigh),
                    border = BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = OnSurfaceVariant,
                        )
                    }
                }

                // Next Round Button (Pill shape with purple gradient)
                Button(
                    onClick = onNextRound,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(52.dp),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(NeonPurpleGlow, NeonPurple)
                                ),
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
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Next Round",
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
}
