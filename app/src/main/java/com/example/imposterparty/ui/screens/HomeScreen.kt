package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.GamePhase
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel
import kotlin.math.sin
import kotlin.random.Random

// ── Data class for floating star particles ──
private data class StarParticle(
    val xFraction: Float,     // 0..1  horizontal position
    val size: Float,          // dp radius
    val speed: Float,         // seconds for full traverse
    val delay: Float,         // initial delay seconds
    val alpha: Float,         // max brightness
)

@Composable
fun HomeScreen(
    gameViewModel: GameViewModel,
    onResumeInProgressGame: (GamePhase) -> Unit,
    onNewGame: () -> Unit,
    onWordPacks: () -> Unit,
    onScoreboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()

    val isMidGame = gameState.phase == GamePhase.REVEALING ||
            gameState.phase == GamePhase.DISCUSSION ||
            gameState.phase == GamePhase.VOTING

    // ── Animations ──
    val infiniteTransition = rememberInfiniteTransition(label = "home")

    // Eye pulse animation
    val eyeGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eyeGlow",
    )

    // Particle drift timer (0→1 repeating)
    val particleTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particleTick",
    )

    // Subtle background glow shift
    val bgPulse by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgPulse",
    )

    // Generate stable star set
    val stars = remember {
        List(35) {
            StarParticle(
                xFraction = Random.nextFloat(),
                size = Random.nextFloat() * 1.5f + 0.5f,
                speed = Random.nextFloat() * 20f + 15f,
                delay = Random.nextFloat() * 10f,
                alpha = Random.nextFloat() * 0.5f + 0.15f,
            )
        }
    }

    Box(
        modifier = modifier
            .background(DeepSpaceBg)
            .drawBehind {
                // Multi-layered radial glows matching Stitch design
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = bgPulse),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = bgPulse + 0.04f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.7f),
                        radius = size.width * 0.7f,
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.2f, size.height * 0.7f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.05f),
                            DeepSpaceBg,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                        radius = size.width,
                    ),
                    radius = size.width,
                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                )

                // Floating star particles
                stars.forEach { star ->
                    val progress = ((particleTick * 30f + star.delay) % star.speed) / star.speed
                    val y = size.height * (1f - progress)
                    val x = size.width * star.xFraction
                    val currentAlpha = star.alpha * sin(progress * Math.PI.toFloat())
                    drawCircle(
                        color = Color.White.copy(alpha = currentAlpha.coerceIn(0f, 1f)),
                        radius = star.size * density,
                        center = Offset(x, y),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.weight(0.8f))

            // ── Hero: Imposter Silhouette ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    DangerRed.copy(alpha = 0.3f * eyeGlow),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                // Glass circle
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(DeepSpaceSurface.copy(alpha = 0.6f))
                        .border(
                            BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.4f)),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Imposter ghost drawn via Canvas
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawImposterSilhouette(eyeGlow)
                    }
                }

                // Under-glow
                Box(
                    modifier = Modifier
                        .offset(y = 70.dp)
                        .size(width = 180.dp, height = 40.dp)
                        .clip(RoundedCornerShape(100))
                        .background(DangerRed.copy(alpha = 0.15f))
                        .blur(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Title: IMPOSTER ──
            Text(
                text = "IMPOSTER",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    fontSize = 52.sp,
                ),
                color = Color.White,
                modifier = Modifier.drawBehind {
                    // Neon purple text glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                PrimaryContainerNeon.copy(alpha = 0.35f),
                                NeonPurple.copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.width * 0.6f,
                        ),
                        radius = size.width * 0.6f,
                        center = center,
                    )
                },
            )

            // ── Subtitle: PARTY ──
            Text(
                text = "PARTY",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    fontSize = 24.sp,
                ),
                color = NeonCyanSoft,
                modifier = Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.25f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.width * 0.5f,
                        ),
                        radius = size.width * 0.5f,
                        center = center,
                    )
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Tagline ──
            Text(
                text = "Find the imposter among your friends!",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Resume In-Progress Game ──
            if (isMidGame) {
                val phaseLabel = when (gameState.phase) {
                    GamePhase.REVEALING -> "Card Reveal"
                    GamePhase.DISCUSSION -> "Discussion"
                    GamePhase.VOTING -> "Voting"
                    else -> "Game"
                }

                Button(
                    onClick = { onResumeInProgressGame(gameState.phase) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(DangerRed, Color(0xFFD50000), ImposterPrimaryDark)
                                ),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .border(
                                BorderStroke(1.5.dp, WarningYellow),
                                shape = RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "⚡ Resume Active Game",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Round ${gameState.roundNumber} • $phaseLabel Phase",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                    color = WarningYellow,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Primary CTA: New Game ──
            Button(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp),
                        )
                        .border(
                            BorderStroke(1.dp, PrimaryContainerNeon.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
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
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New Game",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Secondary CTA: Word Packs (Cyan glass) ──
            GlassButton(
                text = "Word Packs",
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                borderColor = NeonCyan.copy(alpha = 0.3f),
                textColor = NeonCyan,
                bgColor = NeonCyan.copy(alpha = 0.08f),
                onClick = onWordPacks,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Tertiary CTA: Scoreboard (Gold glass) ──
            GlassButton(
                text = "Scoreboard",
                icon = Icons.Default.EmojiEvents,
                borderColor = NeonGold.copy(alpha = 0.3f),
                textColor = NeonGold,
                bgColor = NeonGold.copy(alpha = 0.08f),
                onClick = onScoreboard,
            )

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

// ── Glass-morphic outline button ──
@Composable
private fun GlassButton(
    text: String,
    icon: ImageVector,
    borderColor: Color,
    textColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = textColor,
            )
        }
    }
}

// ── Draw the Imposter ghost silhouette with glowing eyes ──
private fun DrawScope.drawImposterSilhouette(eyeGlowAlpha: Float) {
    val w = size.width
    val h = size.height

    // Ghost body
    val bodyPath = Path().apply {
        moveTo(w * 0.5f, h * 0.08f)
        cubicTo(w * 0.25f, h * 0.08f, w * 0.15f, h * 0.25f, w * 0.15f, h * 0.45f)
        cubicTo(w * 0.15f, h * 0.6f, w * 0.2f, h * 0.7f, w * 0.2f, h * 0.88f)
        lineTo(w * 0.8f, h * 0.88f)
        cubicTo(w * 0.8f, h * 0.7f, w * 0.85f, h * 0.6f, w * 0.85f, h * 0.45f)
        cubicTo(w * 0.85f, h * 0.25f, w * 0.75f, h * 0.08f, w * 0.5f, h * 0.08f)
        close()
    }

    drawPath(
        path = bodyPath,
        color = Color(0xFF31334B).copy(alpha = 0.9f),
        style = Fill,
    )

    // Left eye
    val leftEyePath = Path().apply {
        moveTo(w * 0.32f, h * 0.45f)
        quadraticTo(w * 0.38f, h * 0.38f, w * 0.44f, h * 0.45f)
        quadraticTo(w * 0.38f, h * 0.52f, w * 0.32f, h * 0.45f)
        close()
    }

    // Right eye
    val rightEyePath = Path().apply {
        moveTo(w * 0.68f, h * 0.45f)
        quadraticTo(w * 0.62f, h * 0.38f, w * 0.56f, h * 0.45f)
        quadraticTo(w * 0.62f, h * 0.52f, w * 0.68f, h * 0.45f)
        close()
    }

    // Eye glow halo
    val eyeGlowColor = ImposterEyeRed.copy(alpha = 0.4f * eyeGlowAlpha)
    drawCircle(
        color = eyeGlowColor,
        radius = w * 0.1f,
        center = Offset(w * 0.38f, h * 0.45f),
    )
    drawCircle(
        color = eyeGlowColor,
        radius = w * 0.1f,
        center = Offset(w * 0.62f, h * 0.45f),
    )

    // Eye fill
    val eyeColor = ImposterEyeRed.copy(alpha = eyeGlowAlpha)
    drawPath(path = leftEyePath, color = eyeColor, style = Fill)
    drawPath(path = rightEyePath, color = eyeColor, style = Fill)
}
