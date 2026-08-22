package com.example.imposterparty.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val scrollState = rememberScrollState()

    val startingSpeaker = gameState.players.find { it.id == gameState.startingSpeakerIndex }
        ?: gameState.players.getOrNull(gameState.startingSpeakerIndex)
        ?: gameState.players.firstOrNull()

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
                    text = if (gameState.isSubRound) "SUB-ROUND ${gameState.subRoundNumber} DISCUSSION" else "DISCUSSION TIME",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                    ),
                    color = if (gameState.isSubRound) DangerRed else Color.White,
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (gameState.isSubRound) "Identify the remaining Imposter!" else "Find out who doesn't know the secret word!",
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
                // ── Starting Speaker Highlight Card ──
                if (startingSpeaker != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(StitchSurfaceContainer)
                            .border(BorderStroke(1.5.dp, NeonPurple.copy(alpha = 0.7f)), RoundedCornerShape(22.dp))
                            .padding(20.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
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
                                    modifier = Modifier.size(28.dp),
                                )
                            }

                            Spacer(Modifier.height(12.dp))

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

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "${startingSpeaker.name} gives the first clue or statement about the secret word, then everyone continues around the circle!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── What to Do Instructions Card ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(StitchSurfaceContainer)
                        .border(BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.35f)), RoundedCornerShape(22.dp))
                        .padding(20.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "What To Do",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        DiscussionGuideItem(
                            stepNumber = "1",
                            title = "Give Your Clue",
                            description = "Take turns saying one word or phrase related to the secret word without making it too obvious.",
                            accentColor = NeonCyan,
                        )

                        Spacer(Modifier.height(12.dp))

                        DiscussionGuideItem(
                            stepNumber = "2",
                            title = "Interrogate & Defend",
                            description = "Question suspicious, vague, or repetitive clues. The Imposter will try to blend in!",
                            accentColor = NeonGold,
                        )

                        Spacer(Modifier.height(12.dp))

                        DiscussionGuideItem(
                            stepNumber = "3",
                            title = "Proceed to Voting",
                            description = "Once all players have spoken and shared suspicions, tap the button below to vote.",
                            accentColor = PrimaryContainerNeon,
                        )
                    }
                }

                Spacer(Modifier.height(110.dp))
            }
        }

        // ── Fixed Bottom Action Bar (Proceed to Voting) ──
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
                            text = "Proceed to Voting",
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

@Composable
private fun DiscussionGuideItem(
    stepNumber: String,
    title: String,
    description: String,
    accentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                ),
                color = accentColor,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        }
    }
}
