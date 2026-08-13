package com.example.imposterparty.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.*
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    gameViewModel: GameViewModel,
    onStartGame: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val wordPacks by gameViewModel.wordPacks.collectAsStateWithLifecycle()
    val settings = gameState.settings

    var playerNames by remember { mutableStateOf(gameState.players.map { it.name }.toMutableList().also { if (it.isEmpty()) it.addAll(listOf("", "", "")) }) }
    var newPlayerName by remember { mutableStateOf("") }
    var showCustomTimer by remember { mutableStateOf(settings.timerDuration == TimerDuration.CUSTOM) }
    var customMinutes by remember { mutableStateOf((settings.customTimerSeconds / 60).toString()) }
    var customSeconds by remember { mutableStateOf((settings.customTimerSeconds % 60).toString()) }

    fun updatePlayerList() {
        val validNames = playerNames.filter { it.isNotBlank() }
        gameViewModel.setPlayerNames(validNames)
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(DarkBackground, DarkSurface))
        ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            // Top bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextOnDark)
                    }
                    Text(
                        "Game Setup",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextOnDark,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── Players Section ──
            item {
                SectionHeader("👥 Players (${playerNames.count { it.isNotBlank() }})")
            }

            itemsIndexed(playerNames) { index, name ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ImposterPrimary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ImposterPrimaryLight,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            playerNames = playerNames.toMutableList().also { it[index] = newName }
                            updatePlayerList()
                        },
                        placeholder = { Text("Player ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ImposterPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.3f),
                        ),
                    )
                    if (playerNames.size > 3) {
                        IconButton(onClick = {
                            playerNames = playerNames.toMutableList().also { it.removeAt(index) }
                            updatePlayerList()
                        }) {
                            Icon(Icons.Default.Close, "Remove", tint = DangerRed.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Add player button
            item {
                if (playerNames.size < 12) {
                    TextButton(
                        onClick = {
                            playerNames = playerNames.toMutableList().also { it.add("") }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, null, tint = ImposterSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Player", color = ImposterSecondary)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Imposter Mode Section ──
            item {
                SectionHeader("🕵️ Imposter Mode")
                Spacer(Modifier.height(8.dp))

                // Mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ImposterMode.entries.forEach { mode ->
                        val selected = settings.imposterMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = {
                                gameViewModel.updateSettings(settings.copy(imposterMode = mode))
                            },
                            label = {
                                Text(
                                    if (mode == ImposterMode.MANUAL) "Manual" else "Auto Range",
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ImposterPrimary,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Mode-specific options
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (settings.imposterMode == ImposterMode.MANUAL) {
                            Text(
                                "Number of Imposters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnDarkSecondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                IconButton(
                                    onClick = {
                                        val newCount = (settings.manualImposterCount - 1).coerceAtLeast(1)
                                        gameViewModel.updateSettings(settings.copy(manualImposterCount = newCount))
                                    },
                                ) {
                                    Icon(Icons.Default.Remove, "Decrease", tint = ImposterPrimary)
                                }
                                Text(
                                    "${settings.manualImposterCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = ImposterPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                                IconButton(
                                    onClick = {
                                        val max = (playerNames.count { it.isNotBlank() } - 1).coerceAtLeast(1)
                                        val newCount = (settings.manualImposterCount + 1).coerceAtMost(max)
                                        gameViewModel.updateSettings(settings.copy(manualImposterCount = newCount))
                                    },
                                ) {
                                    Icon(Icons.Default.Add, "Increase", tint = ImposterPrimary)
                                }
                            }
                        } else {
                            Text(
                                "Imposter Range",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnDarkSecondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ImposterRange.entries.forEach { range ->
                                    val selected = settings.autoRange == range
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            gameViewModel.updateSettings(settings.copy(autoRange = range))
                                        },
                                        label = {
                                            Text(
                                                range.label,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ImposterSecondary,
                                            selectedLabelColor = DarkBackground,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "Reveal imposter count at end",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextOnDark,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = settings.revealImposterCountAtEnd,
                                    onCheckedChange = {
                                        gameViewModel.updateSettings(settings.copy(revealImposterCountAtEnd = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ImposterSecondary,
                                        checkedTrackColor = ImposterSecondaryDark,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── Category Section ──
            item {
                SectionHeader("📦 Word Category")
                Spacer(Modifier.height(8.dp))

                if (wordPacks.isEmpty()) {
                    Text(
                        "Loading categories...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnDarkSecondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        wordPacks.forEach { pack ->
                            val selected = settings.selectedCategoryId == pack.id
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    gameViewModel.updateSettings(settings.copy(selectedCategoryId = pack.id))
                                },
                                label = {
                                    Text(
                                        pack.name,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ImposterTertiary,
                                    selectedLabelColor = DarkBackground,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── Timer Section ──
            item {
                SectionHeader("⏱️ Discussion Timer")
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimerDuration.entries.forEach { duration ->
                        val selected = settings.timerDuration == duration
                        FilterChip(
                            selected = selected,
                            onClick = {
                                gameViewModel.updateSettings(settings.copy(timerDuration = duration))
                                showCustomTimer = duration == TimerDuration.CUSTOM
                            },
                            label = {
                                Text(
                                    duration.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningYellow,
                                selectedLabelColor = DarkBackground,
                            ),
                        )
                    }
                }

                AnimatedVisibility(visible = showCustomTimer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = customMinutes,
                            onValueChange = {
                                customMinutes = it.filter { c -> c.isDigit() }.take(2)
                                val mins = customMinutes.toIntOrNull() ?: 0
                                val secs = customSeconds.toIntOrNull() ?: 0
                                gameViewModel.updateSettings(
                                    settings.copy(customTimerSeconds = mins * 60 + secs)
                                )
                            },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarningYellow,
                                unfocusedBorderColor = DarkSurfaceVariant,
                            ),
                        )
                        Text(":", color = TextOnDark, style = MaterialTheme.typography.headlineMedium)
                        OutlinedTextField(
                            value = customSeconds,
                            onValueChange = {
                                customSeconds = it.filter { c -> c.isDigit() }.take(2)
                                val mins = customMinutes.toIntOrNull() ?: 0
                                val secs = customSeconds.toIntOrNull() ?: 0
                                gameViewModel.updateSettings(
                                    settings.copy(customTimerSeconds = mins * 60 + secs)
                                )
                            },
                            label = { Text("Sec") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarningYellow,
                                unfocusedBorderColor = DarkSurfaceVariant,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        // Start button (floating at bottom)
        val validPlayerCount = playerNames.count { it.isNotBlank() }
        val canStart = validPlayerCount >= 3 && settings.selectedCategoryId > 0

        Button(
            onClick = {
                updatePlayerList()
                onStartGame()
            },
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(56.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ImposterPrimary,
                disabledContainerColor = DarkSurfaceVariant,
            ),
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (canStart) "Start Game" else "Add ${3 - validPlayerCount} more players & pick a category",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = TextOnDark,
        fontWeight = FontWeight.Bold,
    )
}
