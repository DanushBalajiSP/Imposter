package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.R
import com.example.imposterparty.data.model.GamePhase
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

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

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgShift",
    )

    val iconGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "iconGlow",
    )

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd, DarkBackground),
                    start = Offset(animOffset, 0f),
                    end = Offset(animOffset + 500f, 1500f),
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Stylized Imposter Logo Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(105.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((95 * iconGlow).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(DangerRed.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Imposter Logo",
                    modifier = Modifier.size(95.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = "IMPOSTER",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp,
                ),
                color = ImposterPrimary,
            )
            Text(
                text = "PARTY",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 9.sp,
                ),
                color = ImposterSecondary,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Find the imposter among your friends!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = TextOnDarkSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Resume In-Progress Game (ONLY shown if interrupted in the middle of gameplay)
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
                        .height(68.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(DangerRed, Color(0xFFD50000), ImposterPrimaryDark)),
                                shape = RoundedCornerShape(18.dp),
                            )
                            .border(BorderStroke(2.dp, WarningYellow), shape = RoundedCornerShape(18.dp)),
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
                                modifier = Modifier.size(28.dp),
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

                Spacer(modifier = Modifier.height(14.dp))
            }

            // New Game Button
            HomeButton(
                text = "New Game",
                icon = Icons.Default.PlayArrow,
                colors = listOf(ImposterPrimary, ImposterPrimaryDark),
                onClick = onNewGame,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Word Packs Button
            HomeButton(
                text = "Word Packs",
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                colors = listOf(ImposterSecondaryDark, ImposterSecondary),
                onClick = onWordPacks,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Scoreboard Button
            HomeButton(
                text = "Scoreboard",
                icon = Icons.Default.EmojiEvents,
                colors = listOf(ImposterTertiaryDark, ImposterTertiary),
                onClick = onScoreboard,
            )
        }
    }
}

@Composable
private fun HomeButton(
    text: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(colors),
                    shape = RoundedCornerShape(18.dp),
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), shape = RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}
