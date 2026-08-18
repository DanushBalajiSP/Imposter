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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun FinalImposterChoiceScreen(
    gameViewModel: GameViewModel,
    onNavigateToResult: () -> Unit,
    onNavigateToSubRoundDiscussion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val survivingImposter = gameState.players.find { it.id == gameState.remainingImposterId }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var isRevealed by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf<ChoiceMode?>(null) }
    var wordGuessInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
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

            // ── Top Header ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "FINAL IMPOSTER PHASE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    ),
                    color = DangerRed,
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Pass Phone Banner ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StitchSurfaceContainer)
                    .border(
                        BorderStroke(1.5.dp, DangerRed.copy(alpha = 0.6f)),
                        RoundedCornerShape(18.dp),
                    )
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "👉 PASS PHONE TO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                        ),
                        color = OnSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = survivingImposter?.name ?: "Remaining Imposter",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                        ),
                        color = DangerRed,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Keep screen hidden from other players!",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningYellow,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            if (!isRevealed) {
                // ── Secret Hold / Tap to Reveal Screen ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(24.dp))
                        .background(StitchSurfaceContainerHigh)
                        .border(
                            BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.35f)),
                            RoundedCornerShape(24.dp),
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isRevealed = true
                                }
                            )
                        }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NeonGold,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Tap to Reveal Your Choices",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Only the surviving Imposter should view this screen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))
            } else {
                // ── Revealed Imposter Choices ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "One Imposter was caught! Choose your path:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // ── Option A: Guess the Secret Word ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selectedMode == ChoiceMode.GUESS_WORD) StitchSurfaceContainerHigh else StitchSurfaceContainer
                            )
                            .border(
                                BorderStroke(
                                    if (selectedMode == ChoiceMode.GUESS_WORD) 2.dp else 1.dp,
                                    if (selectedMode == ChoiceMode.GUESS_WORD) NeonGold else OutlineSubtle.copy(alpha = 0.3f),
                                ),
                                RoundedCornerShape(18.dp),
                            )
                            .padding(16.dp),
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonGold.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = NeonGold,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "A. Guess the Word",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = NeonGold,
                                    )
                                    Text(
                                        text = "Guess correctly to instantly win (+3 pts). Wrong guess = Civilians win.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            if (selectedMode == ChoiceMode.GUESS_WORD) {
                                OutlinedTextField(
                                    value = wordGuessInput,
                                    onValueChange = { wordGuessInput = it },
                                    placeholder = { Text("Enter secret word guess...") },
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

                                Spacer(Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (wordGuessInput.isNotBlank()) {
                                            gameViewModel.submitImposterWordGuess(wordGuessInput.trim())
                                            onNavigateToResult()
                                        }
                                    },
                                    enabled = wordGuessInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                ) {
                                    Text(
                                        text = "Submit Word Guess",
                                        color = DeepSpaceBg,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedMode = ChoiceMode.GUESS_WORD },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NeonGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Select Option A", color = NeonGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // ── Option B: Play Final Sub-Round ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selectedMode == ChoiceMode.SUB_ROUND) StitchSurfaceContainerHigh else StitchSurfaceContainer
                            )
                            .border(
                                BorderStroke(
                                    if (selectedMode == ChoiceMode.SUB_ROUND) 2.dp else 1.dp,
                                    if (selectedMode == ChoiceMode.SUB_ROUND) NeonPurple else OutlineSubtle.copy(alpha = 0.3f),
                                ),
                                RoundedCornerShape(18.dp),
                            )
                            .padding(16.dp),
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = PrimaryContainerNeon,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "B. Play Sub-Round",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = PrimaryContainerNeon,
                                    )
                                    Text(
                                        text = "Continue with a final clue & vote with surviving players. Survive to win (+3 pts).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    gameViewModel.startSubRound()
                                    onNavigateToSubRoundDiscussion()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                            ) {
                                Text(
                                    text = "Start Final Sub-Round",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private enum class ChoiceMode {
    GUESS_WORD,
    SUB_ROUND,
}
