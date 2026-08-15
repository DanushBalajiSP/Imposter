package com.example.imposterparty.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Default player names initialized with "Player 1", "Player 2", "Player 3"
    var playerNames by remember {
        val initial = gameState.players.map { it.name }.toMutableList()
        if (initial.size < 3) {
            while (initial.size < 3) {
                initial.add("Player ${initial.size + 1}")
            }
        }
        mutableStateOf(initial)
    }

    var showCustomTimer by remember { mutableStateOf(settings.timerDuration == TimerDuration.CUSTOM) }
    var customMinutes by remember { mutableStateOf((settings.customTimerSeconds / 60).toString()) }
    var customSeconds by remember { mutableStateOf((settings.customTimerSeconds % 60).toString()) }
    var showPackSelectorDialog by remember { mutableStateOf(false) }

    fun updatePlayerList() {
        val resolved = playerNames.mapIndexed { index, name ->
            if (name.isBlank()) "Player ${index + 1}" else name.trim()
        }
        gameViewModel.setPlayerNames(resolved)
    }

    // Category Selector Dialog
    if (showPackSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showPackSelectorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Select Word Packs",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val allIds = wordPacks.map { it.id }.toSet()
                            val newSelected = if (settings.selectedCategoryIds.size == allIds.size) {
                                emptySet()
                            } else {
                                allIds
                            }
                            gameViewModel.updateSettings(settings.copy(selectedCategoryIds = newSelected))
                        },
                    ) {
                        Text(
                            if (settings.selectedCategoryIds.size == wordPacks.size) "Clear" else "Select All",
                            color = ImposterPrimaryLight,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 350.dp),
                ) {
                    items(wordPacks) { pack ->
                        val isSelected = settings.selectedCategoryIds.isEmpty() || settings.selectedCategoryIds.contains(pack.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DarkSurfaceVariant else DarkSurfaceVariant.copy(alpha = 0.4f))
                                .clickable {
                                    val current = if (settings.selectedCategoryIds.isEmpty()) {
                                        wordPacks.map { it.id }.toSet()
                                    } else {
                                        settings.selectedCategoryIds
                                    }
                                    val newSelected = if (current.contains(pack.id)) {
                                        current - pack.id
                                    } else {
                                        current + pack.id
                                    }
                                    gameViewModel.updateSettings(settings.copy(selectedCategoryIds = newSelected))
                                }
                                .padding(10.dp),
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val current = if (settings.selectedCategoryIds.isEmpty()) {
                                        wordPacks.map { it.id }.toSet()
                                    } else {
                                        settings.selectedCategoryIds
                                    }
                                    val newSelected = if (checked) current + pack.id else current - pack.id
                                    gameViewModel.updateSettings(settings.copy(selectedCategoryIds = newSelected))
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ImposterSecondary,
                                    checkmarkColor = DarkBackground,
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                pack.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = if (isSelected) TextOnDark else TextOnDarkSecondary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPackSelectorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ImposterPrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
        )
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
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                        ),
                        color = TextOnDark,
                    )
                }
            }

            // ── Players Section ──
            item {
                SectionHeader("👥 Players (${playerNames.size})")
                Spacer(Modifier.height(8.dp))
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
                            playerNames = playerNames.toMutableList().also { it.add("Player ${it.size + 1}") }
                            updatePlayerList()
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, null, tint = ImposterSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Player", color = ImposterSecondary, fontWeight = FontWeight.Bold)
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
                                        val max = (playerNames.size - 1).coerceAtLeast(1)
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

            // ── Category Section (Shows Selected Word Packs Only) ──
            item {
                val selectedPacks = if (settings.selectedCategoryIds.isEmpty()) {
                    wordPacks
                } else {
                    wordPacks.filter { settings.selectedCategoryIds.contains(it.id) }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SectionHeader("📦 Selected Word Packs (${selectedPacks.size})")
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = { showPackSelectorDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ImposterSecondary.copy(alpha = 0.2f),
                            contentColor = ImposterSecondary,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Change", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (selectedPacks.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            Text(
                                "No word packs selected! Tap Change to select packs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnDark,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedPacks.forEach { pack ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ImposterTertiary.copy(alpha = 0.4f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        pack.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextOnDark,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Surface(
                                        color = if (pack.isBuiltIn) ImposterSecondary.copy(alpha = 0.15f) else ImposterTertiary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                    ) {
                                        Text(
                                            if (pack.isBuiltIn) "Built-in" else "Custom",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (pack.isBuiltIn) ImposterSecondary else ImposterTertiary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            val current = if (settings.selectedCategoryIds.isEmpty()) {
                                                wordPacks.map { it.id }.toSet()
                                            } else {
                                                settings.selectedCategoryIds
                                            }
                                            val newSelected = current - pack.id
                                            gameViewModel.updateSettings(settings.copy(selectedCategoryIds = newSelected))
                                        },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove pack",
                                            tint = TextOnDarkSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── Discussion Timer Section ──
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SectionHeader("⏱️ Discussion Timer")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = settings.isTimerEnabled,
                        onCheckedChange = { isEnabled ->
                            gameViewModel.updateSettings(settings.copy(isTimerEnabled = isEnabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WarningYellow,
                            checkedTrackColor = WarningYellow.copy(alpha = 0.4f),
                        ),
                    )
                }

                AnimatedVisibility(visible = settings.isTimerEnabled) {
                    Column {
                        Spacer(Modifier.height(8.dp))

                        // Duration chips
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
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WarningYellow,
                                        selectedLabelColor = DarkBackground,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // Custom timer inputs
                        AnimatedVisibility(visible = showCustomTimer) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Custom Duration",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextOnDarkSecondary,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedTextField(
                                            value = customMinutes,
                                            onValueChange = {
                                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                                    customMinutes = it
                                                    val mins = it.toIntOrNull() ?: 0
                                                    val secs = customSeconds.toIntOrNull() ?: 0
                                                    gameViewModel.updateSettings(
                                                        settings.copy(customTimerSeconds = (mins * 60 + secs).coerceAtLeast(10))
                                                    )
                                                }
                                            },
                                            label = { Text("Min") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        Text(":", style = MaterialTheme.typography.headlineMedium, color = TextOnDark)
                                        OutlinedTextField(
                                            value = customSeconds,
                                            onValueChange = {
                                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                                    val s = it.toIntOrNull() ?: 0
                                                    if (s < 60) {
                                                        customSeconds = it
                                                        val mins = customMinutes.toIntOrNull() ?: 0
                                                        gameViewModel.updateSettings(
                                                            settings.copy(customTimerSeconds = (mins * 60 + s).coerceAtLeast(10))
                                                        )
                                                    }
                                                }
                                            },
                                            label = { Text("Sec") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = !settings.isTimerEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            "Untimed discussion. Players can talk freely and tap 'Vote Now' when ready.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnDarkSecondary,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        // Start button (ALWAYS enabled)
        Button(
            onClick = {
                val resolved = playerNames.mapIndexed { index, name ->
                    if (name.isBlank()) "Player ${index + 1}" else name.trim()
                }.toMutableList()
                while (resolved.size < 3) {
                    resolved.add("Player ${resolved.size + 1}")
                }
                playerNames = resolved
                gameViewModel.setPlayerNames(resolved)
                onStartGame()
            },
            enabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(58.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ImposterPrimary,
                disabledContainerColor = DarkSurfaceVariant,
            ),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Start Game",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
        ),
        color = TextOnDark,
    )
}
