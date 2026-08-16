package com.example.imposterparty.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

    // Default player names initialized with at least 3 players
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

    // Category Selector Dialog (Deep Space Stitch themed)
    if (showPackSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showPackSelectorDialog = false },
            containerColor = StitchSurfaceContainer,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Select Word Packs",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                        ),
                        color = Color.White,
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
                            color = NeonCyan,
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
                                .background(
                                    if (isSelected) StitchSurfaceContainerHigh else StitchSurfaceContainerHigh.copy(alpha = 0.4f)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonPurple.copy(alpha = 0.5f) else OutlineSubtle.copy(alpha = 0.2f)
                                    ),
                                    RoundedCornerShape(12.dp),
                                )
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
                                .padding(12.dp),
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
                                    checkedColor = NeonPurple,
                                    checkmarkColor = Color.White,
                                    uncheckedColor = OutlineSubtle,
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                pack.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = if (isSelected) Color.White else OnSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPackSelectorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
        )
    }

    Box(
        modifier = modifier
            .background(DeepSpaceBg)
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp),
        ) {
            // ── Top Bar / Header ──
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StitchSurfaceContainerHigh.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = OnSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Game Setup",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = PrimaryContainerNeon,
                        )
                        Text(
                            text = "Set the rules. Start the chaos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // ── Players Section ──
            item {
                SectionHeader(emoji = "👥", title = "Players (${playerNames.size})")
                Spacer(Modifier.height(12.dp))
            }

            itemsIndexed(playerNames) { index, name ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    // Circular Player Number Badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(StitchSurfaceContainerHigh)
                            .border(BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = PrimaryContainerNeon,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Player Name Capsule Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StitchSurfaceContainer)
                            .border(
                                BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            BasicTextField(
                                value = name,
                                onValueChange = { newName ->
                                    playerNames = playerNames.toMutableList().also { it[index] = newName }
                                    updatePlayerList()
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                cursorBrush = SolidColor(NeonPurpleGlow),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (name.isEmpty()) {
                                        Text(
                                            text = "Player ${index + 1}",
                                            style = TextStyle(
                                                color = OnSurfaceVariant.copy(alpha = 0.4f),
                                                fontSize = 16.sp,
                                            ),
                                        )
                                    }
                                    innerTextField()
                                },
                            )

                            if (playerNames.size > 3) {
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        playerNames = playerNames.toMutableList().also { it.removeAt(index) }
                                        updatePlayerList()
                                    },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove player",
                                        tint = DangerRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Add Player Button (up to 24) ──
            item {
                if (playerNames.size < 24) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StitchSurfaceContainer.copy(alpha = 0.4f))
                            .border(
                                BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.4f)),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable {
                                playerNames = playerNames.toMutableList().also { it.add("Player ${it.size + 1}") }
                                updatePlayerList()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ADD PLAYER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                ),
                                color = OnSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Imposter Mode Section ──
            item {
                SectionHeader(emoji = "🕵️", title = "Imposter Mode")
                Spacer(Modifier.height(12.dp))

                // Segmented Switcher (Pill Style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchSurfaceContainerHigh)
                        .border(
                            BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.25f)),
                            RoundedCornerShape(10.dp),
                        )
                        .padding(4.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ImposterMode.entries.forEach { mode ->
                            val isSelected = settings.imposterMode == mode
                            val label = if (mode == ImposterMode.MANUAL) "Manual" else "Auto Range"

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple))
                                        } else {
                                            SolidColor(Color.Transparent)
                                        }
                                    )
                                    .clickable {
                                        gameViewModel.updateSettings(settings.copy(imposterMode = mode))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.5.sp,
                                    ),
                                    color = if (isSelected) Color.White else OnSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Mode-specific container card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(StitchSurfaceContainer)
                        .border(
                            BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.25f)),
                            RoundedCornerShape(20.dp),
                        )
                        .padding(20.dp),
                ) {
                    if (settings.imposterMode == ImposterMode.MANUAL) {
                        val maxAllowed = maxImpostersForPlayerCount(playerNames.size)
                        val recommended = maxAllowed

                        // Auto-clamp if current value exceeds allowed maximum
                        LaunchedEffect(playerNames.size) {
                            if (settings.manualImposterCount > maxAllowed) {
                                gameViewModel.updateSettings(settings.copy(manualImposterCount = maxAllowed))
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Number of Imposters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                            )

                            Spacer(Modifier.height(16.dp))

                            // Stepper Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                // Minus Button
                                IconButton(
                                    onClick = {
                                        val newCount = (settings.manualImposterCount - 1).coerceAtLeast(1)
                                        gameViewModel.updateSettings(settings.copy(manualImposterCount = newCount))
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(StitchSurfaceBright)
                                        .border(
                                            BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)),
                                            CircleShape,
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease imposters",
                                        tint = PrimaryContainerNeon,
                                    )
                                }

                                Spacer(Modifier.width(28.dp))

                                // Number with Glow
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.width(60.dp),
                                ) {
                                    Text(
                                        text = "${settings.manualImposterCount}",
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 36.sp,
                                        ),
                                        color = PrimaryContainerNeon,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.drawBehind {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    listOf(
                                                        NeonPurple.copy(alpha = 0.4f),
                                                        Color.Transparent,
                                                    ),
                                                    center = center,
                                                    radius = size.width * 0.8f,
                                                ),
                                                radius = size.width * 0.8f,
                                                center = center,
                                            )
                                        },
                                    )
                                }

                                Spacer(Modifier.width(28.dp))

                                // Plus Button
                                IconButton(
                                    onClick = {
                                        val newCount = (settings.manualImposterCount + 1).coerceAtMost(maxAllowed)
                                        gameViewModel.updateSettings(settings.copy(manualImposterCount = newCount))
                                    },
                                    enabled = settings.manualImposterCount < maxAllowed,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (settings.manualImposterCount < maxAllowed) StitchSurfaceBright else StitchSurfaceBright.copy(alpha = 0.4f)
                                        )
                                        .border(
                                            BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)),
                                            CircleShape,
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase imposters",
                                        tint = if (settings.manualImposterCount < maxAllowed) PrimaryContainerNeon else PrimaryContainerNeon.copy(alpha = 0.3f),
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Recommended: $recommended Imposters",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = OutlineSubtle,
                            )
                        }
                    } else {
                        // ── Auto Range Mode ──
                        val availableRanges = validAutoRanges(playerNames.size)
                        val recommended = recommendedAutoRange(playerNames.size)

                        LaunchedEffect(playerNames.size) {
                            if (settings.autoRange !in availableRanges) {
                                gameViewModel.updateSettings(settings.copy(autoRange = recommended))
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Imposter Range",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                            )

                            Spacer(Modifier.height(14.dp))

                            // Auto Range Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                availableRanges.forEach { range ->
                                    val isSelected = settings.autoRange == range
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) {
                                                    Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple))
                                                } else {
                                                    SolidColor(StitchSurfaceContainerHigh)
                                                }
                                            )
                                            .border(
                                                BorderStroke(
                                                    1.dp,
                                                    if (isSelected) PrimaryContainerNeon.copy(alpha = 0.5f) else OutlineSubtle.copy(alpha = 0.2f),
                                                ),
                                                RoundedCornerShape(10.dp),
                                            )
                                            .clickable {
                                                gameViewModel.updateSettings(settings.copy(autoRange = range))
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = range.label,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            ),
                                            color = if (isSelected) Color.White else OnSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Recommended: ${recommended.label} for ${playerNames.size} players",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyanSoft,
                            )

                            Spacer(Modifier.height(16.dp))

                            // Reveal Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Reveal imposter count at end",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = settings.revealImposterCountAtEnd,
                                    onCheckedChange = {
                                        gameViewModel.updateSettings(settings.copy(revealImposterCountAtEnd = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                        uncheckedThumbColor = OnSurfaceVariant,
                                        uncheckedTrackColor = StitchSurfaceContainerHigh,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── Word Packs Section ──
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
                    SectionHeader(
                        emoji = "📦",
                        title = "Selected Word Packs (${selectedPacks.size})",
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showPackSelectorDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Change packs",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = NeonCyan,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (selectedPacks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DangerRed.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = "No word packs selected! Tap Change to select packs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedPacks.forEach { pack ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StitchSurfaceContainer)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (pack.isBuiltIn) NeonCyan.copy(alpha = 0.25f) else NeonGold.copy(alpha = 0.25f),
                                        ),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    // Pack Name
                                    Text(
                                        text = pack.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.weight(1f),
                                    )

                                    // Badge Pill
                                    Surface(
                                        color = if (pack.isBuiltIn) NeonCyan.copy(alpha = 0.12f) else NeonGold.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp),
                                    ) {
                                        Text(
                                            text = if (pack.isBuiltIn) "Built-in" else "Custom",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = if (pack.isBuiltIn) NeonCyanSoft else NeonGold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // Remove Icon Button
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
                                        modifier = Modifier.size(20.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove pack",
                                            tint = OnSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp),
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
                    SectionHeader(emoji = "⏱️", title = "Discussion Timer")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = settings.isTimerEnabled,
                        onCheckedChange = { isEnabled ->
                            gameViewModel.updateSettings(settings.copy(isTimerEnabled = isEnabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonPurpleGlow,
                            checkedTrackColor = NeonPurple.copy(alpha = 0.4f),
                            uncheckedThumbColor = OnSurfaceVariant,
                            uncheckedTrackColor = StitchSurfaceContainerHigh,
                        ),
                    )
                }

                AnimatedVisibility(visible = settings.isTimerEnabled) {
                    Column {
                        Spacer(Modifier.height(12.dp))

                        // Duration chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TimerDuration.entries.forEach { duration ->
                                val selected = settings.timerDuration == duration
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) {
                                                Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple))
                                            } else {
                                                SolidColor(StitchSurfaceContainerHigh)
                                            }
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (selected) PrimaryContainerNeon.copy(alpha = 0.5f) else OutlineSubtle.copy(alpha = 0.2f),
                                            ),
                                            RoundedCornerShape(10.dp),
                                        )
                                        .clickable {
                                            gameViewModel.updateSettings(settings.copy(timerDuration = duration))
                                            showCustomTimer = duration == TimerDuration.CUSTOM
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = duration.label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                        color = if (selected) Color.White else OnSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // Custom timer inputs
                        AnimatedVisibility(visible = showCustomTimer) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StitchSurfaceContainer)
                                    .border(
                                        BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)),
                                        RoundedCornerShape(16.dp),
                                    )
                                    .padding(16.dp),
                            ) {
                                Column {
                                    Text(
                                        text = "Custom Duration",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant,
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
                                            label = { Text("Min", color = OnSurfaceVariant) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonPurple,
                                                unfocusedBorderColor = OutlineSubtle,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                        )
                                        Text(":", style = MaterialTheme.typography.headlineMedium, color = Color.White)
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
                                            label = { Text("Sec", color = OnSurfaceVariant) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonPurple,
                                                unfocusedBorderColor = OutlineSubtle,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = !settings.isTimerEnabled) {
                    Text(
                        text = "Untimed discussion. Players can talk freely and tap 'Vote Now' when ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        // ── Sticky Footer CTA (Start Game) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            DeepSpaceBg.copy(alpha = 0.85f),
                            DeepSpaceBg,
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
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
                            Brush.verticalGradient(
                                listOf(NeonPurpleGlow, NeonPurple)
                            ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            BorderStroke(1.dp, PrimaryContainerNeon.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
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
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Start Game",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
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
private fun SectionHeader(emoji: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
            color = Color.White,
        )
    }
}
