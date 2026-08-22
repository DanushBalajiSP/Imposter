package com.example.imposterparty.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.Role
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun FinalImposterChoiceScreen(
    gameViewModel: GameViewModel,
    onNavigateToResult: () -> Unit,
    onNavigateToSubRoundReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val activePlayers = remember(gameState.players, gameState.eliminatedPlayerIds) {
        gameState.players.filter { it.id !in gameState.eliminatedPlayerIds }
    }
    val activeImposters = remember(activePlayers) {
        activePlayers.filter { it.role == Role.IMPOSTER }
    }
    val accusedPlayer = gameState.players.find { it.id == gameState.accusedPlayerId }

    var isVolunteeringToGuess by remember { mutableStateOf(false) }
    var selectedVolunteerId by remember(activeImposters) {
        mutableStateOf<Int?>(activeImposters.firstOrNull()?.id)
    }
    var wordGuessInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
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
            Spacer(Modifier.height(16.dp))

            // ── Top Header Badge ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = null,
                    tint = NeonGold,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "IMPOSTER CHOICE PHASE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    ),
                    color = NeonGold,
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Status Banner (Anonymous) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StitchSurfaceContainer)
                    .border(
                        BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.35f)),
                        RoundedCornerShape(18.dp),
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (accusedPlayer != null) {
                        Text(
                            text = "${accusedPlayer.name} was eliminated!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = WarningYellow,
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = "${activePlayers.size} Players Remaining",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = OnSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!isVolunteeringToGuess) {
                // ── Main Public Question View ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    // Question Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(24.dp))
                            .background(StitchSurfaceContainerHigh)
                            .border(
                                BorderStroke(1.5.dp, NeonPurple.copy(alpha = 0.6f)),
                                RoundedCornerShape(24.dp),
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = PrimaryContainerNeon,
                                    modifier = Modifier.size(36.dp),
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Is the imposter want to guess the word?",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "• Yes: An imposter volunteers to guess for an instant +3 pts (eliminated if correct, Civilians win if wrong).\n• No: Proceed to the Sub-Round with a new secret word.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Choice Buttons ──
                    // YES Button (Gold/Amber)
                    Button(
                        onClick = { isVolunteeringToGuess = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = DeepSpaceBg,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Yes, Guess Secret Word (+3 pts)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                ),
                                color = DeepSpaceBg,
                            )
                        }
                    }

                    // NO Button (Purple/Indigo)
                    Button(
                        onClick = {
                            gameViewModel.startSubRound()
                            onNavigateToSubRoundReveal()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple)),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .border(
                                    BorderStroke(1.dp, PrimaryContainerNeon.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "No, Go to Sub-Round",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                    ),
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            } else {
                // ── Imposter Volunteer Input View ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(StitchSurfaceContainerHigh)
                            .border(
                                BorderStroke(1.5.dp, NeonGold.copy(alpha = 0.8f)),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(20.dp),
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = NeonGold,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Imposter Word Guess",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                    ),
                                    color = NeonGold,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "The volunteering imposter is stepping forward! Enter the exact secret word below:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                            )

                            if (activeImposters.size > 1) {
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Select Volunteering Player:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OnSurfaceVariant,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    activeImposters.forEach { imposter ->
                                        val isSelected = selectedVolunteerId == imposter.id
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) NeonGold.copy(alpha = 0.2f) else StitchSurfaceContainer)
                                                .border(
                                                    BorderStroke(
                                                        if (isSelected) 1.5.dp else 1.dp,
                                                        if (isSelected) NeonGold else OutlineSubtle.copy(alpha = 0.3f),
                                                    ),
                                                    RoundedCornerShape(10.dp),
                                                )
                                                .clickable { selectedVolunteerId = imposter.id }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = imposter.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                                ),
                                                color = if (isSelected) NeonGold else Color.White,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = wordGuessInput,
                                onValueChange = { wordGuessInput = it },
                                placeholder = { Text("Type the secret word...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGold,
                                    unfocusedBorderColor = OutlineSubtle,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            )

                            Spacer(Modifier.height(14.dp))

                            // Stakes Reminder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StitchSurfaceContainer)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = "⚠️ Correct = +3 pts instant & eliminated • Wrong = Civilians Win",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WarningYellow,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (wordGuessInput.isNotBlank()) {
                                        gameViewModel.submitImposterWordGuess(
                                            guess = wordGuessInput.trim(),
                                            volunteerPlayerId = selectedVolunteerId,
                                        )
                                        onNavigateToResult()
                                    }
                                },
                                enabled = wordGuessInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                            ) {
                                Text(
                                    text = "Confirm & Submit Guess",
                                    color = DeepSpaceBg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            TextButton(
                                onClick = { isVolunteeringToGuess = false },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Cancel & Go Back",
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
