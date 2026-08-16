package com.example.imposterparty.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.GamePhase
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun VotingScreen(
    gameViewModel: GameViewModel,
    onVotingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val currentVoter = gameViewModel.getCurrentVoter()
    var selectedPlayerId by remember(gameState.currentVoterIndex) { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    // Watch for phase transition to Result
    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.RESULT) {
            onVotingComplete()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "votingGlow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlow",
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
            // ── Top Header ──
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = null,
                        tint = PrimaryContainerNeon,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "SECRET VOTING",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                        ),
                        color = Color.White,
                    )
                }

                // Voter progress counter pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(StitchSurfaceContainerHigh)
                        .border(
                            BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.35f)),
                            RoundedCornerShape(9999.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Voter ${gameState.currentVoterIndex + 1} of ${gameState.players.size}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = NeonCyanSoft,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Prominent Center Current Voter Card ──
            if (currentVoter != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(StitchSurfaceContainer)
                        .border(
                            BorderStroke(1.5.dp, NeonPurple.copy(alpha = 0.6f)),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "👉 PASS PHONE TO / VOTING NOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontSize = 11.sp,
                            ),
                            color = OnSurfaceVariant,
                        )

                        Spacer(Modifier.height(4.dp))

                        // Large, Bold Voter Name
                        Text(
                            text = currentVoter.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                fontSize = 28.sp,
                            ),
                            color = PrimaryContainerNeon,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(
                                            NeonPurple.copy(alpha = 0.35f),
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

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Who do you think is the Imposter?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = WarningYellow,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (gameState.currentVoterIndex.toFloat() + 1f) / gameState.players.size.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NeonPurple,
                    trackColor = StitchSurfaceContainerHigh,
                )

                Spacer(Modifier.height(14.dp))

                // ── 2-Column Player Grid ──
                val candidatePlayers = remember(gameState.players, currentVoter.id) {
                    gameState.players.filter { it.id != currentVoter.id }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(candidatePlayers, key = { it.id }) { player ->
                        val isSelected = selectedPlayerId == player.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSelected) StitchSurfaceContainerHigh else StitchSurfaceContainer)
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) NeonPurple else OutlineSubtle.copy(alpha = 0.3f),
                                    ),
                                    RoundedCornerShape(24.dp),
                                )
                                .clickable {
                                    selectedPlayerId = player.id
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                .padding(14.dp),
                        ) {
                            // Left indicator accent strip
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(3.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isSelected) NeonPurple else NeonCyan),
                            )

                            // Top-right selection icon
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = NeonPurpleGlow,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp),
                                )
                            }

                            // Card Content
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                // Circular Avatar
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(StitchSurfaceBright)
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isSelected) NeonPurple.copy(alpha = 0.5f) else OutlineSubtle.copy(alpha = 0.3f),
                                            ),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) NeonPurpleGlow else OnSurfaceVariant,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // Player Name
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(2.dp))

                                // Status text
                                Text(
                                    text = if (isSelected) "ACCUSE" else "SUSPECT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 9.sp,
                                    ),
                                    color = if (isSelected) NeonPurpleGlow else OnSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Fixed Bottom Action Bar (Confirm Vote) ──
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
            val isEnabled = selectedPlayerId != null

            Button(
                onClick = {
                    selectedPlayerId?.let { id ->
                        gameViewModel.castVote(id)
                        selectedPlayerId = null
                    }
                },
                enabled = isEnabled,
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
                            if (isEnabled) {
                                Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple))
                            } else {
                                SolidColor(StitchSurfaceContainerHigh)
                            }
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isEnabled) PrimaryContainerNeon.copy(alpha = 0.4f) else OutlineSubtle.copy(alpha = 0.2f),
                            ),
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
                            tint = if (isEnabled) Color.White else OnSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEnabled) "Confirm Vote" else "Select a Player",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                            color = if (isEnabled) Color.White else OnSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}
