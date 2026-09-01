package com.example.imposterparty.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.*
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel
import java.util.UUID

private data class PlayerSetupItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
)

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
    var playerList by remember {
        val initial = gameState.players.map { PlayerSetupItem(name = it.name) }.toMutableList()
        if (initial.size < 3) {
            while (initial.size < 3) {
                initial.add(PlayerSetupItem(name = "Player ${initial.size + 1}"))
            }
        }
        mutableStateOf<List<PlayerSetupItem>>(initial)
    }

    var selectedPlayerForSwap by remember { mutableStateOf<Int?>(null) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var showPackSelectorDialog by remember { mutableStateOf(false) }

    fun updatePlayerList() {
        val resolved = playerList.mapIndexed { index, item ->
            if (item.name.isBlank()) "Player ${index + 1}" else item.name.trim()
        }
        gameViewModel.setPlayerNames(resolved)
    }

    fun swapPlayers(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in playerList.indices || toIndex !in playerList.indices) return
        val mutable = playerList.toMutableList()
        val temp = mutable[fromIndex]
        mutable[fromIndex] = mutable[toIndex]
        mutable[toIndex] = temp
        playerList = mutable
        updatePlayerList()
    }

    fun movePlayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in playerList.indices || toIndex !in playerList.indices) return
        val mutable = playerList.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        playerList = mutable
        updatePlayerList()
    }

    // ── Quick Swap Dialog (Spotify Queue Style) ──
    if (selectedPlayerForSwap != null) {
        val fromIdx = selectedPlayerForSwap!!
        if (fromIdx in playerList.indices) {
            val currentPlayer = playerList[fromIdx]
            val currentDisplayName = currentPlayer.name.ifBlank { "Player ${fromIdx + 1}" }

            AlertDialog(
                onDismissRequest = { selectedPlayerForSwap = null },
                containerColor = StitchSurfaceContainer,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.2f))
                                    .border(BorderStroke(1.dp, NeonPurple.copy(alpha = 0.6f)), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    tint = NeonPurpleGlow,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = "Reorder Queue",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                ),
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Selected: Position #${fromIdx + 1} • $currentDisplayName",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = NeonCyanSoft,
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Quick Move Controls
                        Text(
                            text = "QUICK MOVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = OnSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // Move Up
                            Button(
                                onClick = {
                                    movePlayer(fromIdx, fromIdx - 1)
                                    selectedPlayerForSwap = null
                                },
                                enabled = fromIdx > 0,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StitchSurfaceContainerHigh,
                                    disabledContainerColor = StitchSurfaceContainerHigh.copy(alpha = 0.3f),
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                            ) {
                                Text("▲ Up", fontSize = 12.sp, color = if (fromIdx > 0) Color.White else OnSurfaceVariant.copy(alpha = 0.4f))
                            }

                            // Move Down
                            Button(
                                onClick = {
                                    movePlayer(fromIdx, fromIdx + 1)
                                    selectedPlayerForSwap = null
                                },
                                enabled = fromIdx < playerList.size - 1,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StitchSurfaceContainerHigh,
                                    disabledContainerColor = StitchSurfaceContainerHigh.copy(alpha = 0.3f),
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                            ) {
                                Text("▼ Down", fontSize = 12.sp, color = if (fromIdx < playerList.size - 1) Color.White else OnSurfaceVariant.copy(alpha = 0.4f))
                            }

                            // Move to Top
                            Button(
                                onClick = {
                                    movePlayer(fromIdx, 0)
                                    selectedPlayerForSwap = null
                                },
                                enabled = fromIdx != 0,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StitchSurfaceContainerHigh,
                                    disabledContainerColor = StitchSurfaceContainerHigh.copy(alpha = 0.3f),
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                            ) {
                                Text("🔝 Top", fontSize = 12.sp, color = if (fromIdx != 0) Color.White else OnSurfaceVariant.copy(alpha = 0.4f))
                            }

                            // Move to Bottom
                            Button(
                                onClick = {
                                    movePlayer(fromIdx, playerList.size - 1)
                                    selectedPlayerForSwap = null
                                },
                                enabled = fromIdx != playerList.size - 1,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StitchSurfaceContainerHigh,
                                    disabledContainerColor = StitchSurfaceContainerHigh.copy(alpha = 0.3f),
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                            ) {
                                Text("🔚 End", fontSize = 12.sp, color = if (fromIdx != playerList.size - 1) Color.White else OnSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "SWAP DIRECTLY WITH (SPOTIFY QUEUE)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = OnSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                        ) {
                            itemsIndexed(playerList) { idx, targetPlayer ->
                                if (idx != fromIdx) {
                                    val targetName = targetPlayer.name.ifBlank { "Player ${idx + 1}" }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(StitchSurfaceContainerHigh)
                                            .border(
                                                BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.25f)),
                                                RoundedCornerShape(12.dp),
                                            )
                                            .clickable {
                                                swapPlayers(fromIdx, idx)
                                                selectedPlayerForSwap = null
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            // Number badge
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(StitchSurfaceBright)
                                                    .border(BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)), CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = PrimaryContainerNeon,
                                                )
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            Text(
                                                text = targetName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = Color.White,
                                                modifier = Modifier.weight(1f),
                                            )

                                            Spacer(Modifier.width(8.dp))

                                            // Swap Chip
                                            Surface(
                                                color = NeonPurple.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f)),
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SwapHoriz,
                                                        contentDescription = null,
                                                        tint = NeonPurpleGlow,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "Swap",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                        ),
                                                        color = NeonPurpleGlow,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedPlayerForSwap = null }) {
                        Text("Close", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                },
            )
        }
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
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SectionHeader(emoji = "👥", title = "Players (${playerList.size})")
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Drag ≡ or tap ⇄ to swap",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                        ),
                        color = OnSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            itemsIndexed(playerList, key = { _, item -> item.id }) { index, item ->
                val isDraggingThis = draggingIndex == index
                val itemOffsetY = if (isDraggingThis) dragOffsetY else 0f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .zIndex(if (isDraggingThis) 2f else 1f)
                        .graphicsLayer {
                            translationY = itemOffsetY
                            scaleX = if (isDraggingThis) 1.02f else 1.0f
                            scaleY = if (isDraggingThis) 1.02f else 1.0f
                            shadowElevation = if (isDraggingThis) 12f else 0f
                        }
                        .padding(vertical = 4.dp),
                ) {
                    // Spotify-style Drag Handle (≡ / DragHandle)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDraggingThis) NeonPurple.copy(alpha = 0.3f)
                                else StitchSurfaceContainerHigh.copy(alpha = 0.7f)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isDraggingThis) NeonCyan else OutlineSubtle.copy(alpha = 0.3f)
                                ),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable {
                                selectedPlayerForSwap = index
                            }
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val itemHeight = 56.dp.toPx()
                                        val currentDrag = draggingIndex
                                        if (currentDrag != null) {
                                            if (dragOffsetY > itemHeight && currentDrag < playerList.size - 1) {
                                                movePlayer(currentDrag, currentDrag + 1)
                                                draggingIndex = currentDrag + 1
                                                dragOffsetY -= itemHeight
                                            } else if (dragOffsetY < -itemHeight && currentDrag > 0) {
                                                movePlayer(currentDrag, currentDrag - 1)
                                                draggingIndex = currentDrag - 1
                                                dragOffsetY += itemHeight
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                        updatePlayerList()
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Reorder position",
                            tint = if (isDraggingThis) NeonCyan else OnSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Circular Player Number Badge (Tap to Quick Swap)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(StitchSurfaceContainerHigh)
                            .border(BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)), CircleShape)
                            .clickable {
                                selectedPlayerForSwap = index
                            },
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

                    Spacer(Modifier.width(10.dp))

                    // Player Name Capsule Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDraggingThis) StitchSurfaceContainerHigh else StitchSurfaceContainer
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isDraggingThis) NeonPurpleGlow else OutlineSubtle.copy(alpha = 0.3f),
                                ),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            BasicTextField(
                                value = item.name,
                                onValueChange = { newName ->
                                    playerList = playerList.toMutableList().also { it[index] = it[index].copy(name = newName) }
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
                                    if (item.name.isEmpty()) {
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

                            // Quick Swap Button
                            IconButton(
                                onClick = { selectedPlayerForSwap = index },
                                modifier = Modifier.size(26.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap position with another player",
                                    tint = OnSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            if (playerList.size > 3) {
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        playerList = playerList.toMutableList().also { it.removeAt(index) }
                                        updatePlayerList()
                                    },
                                    modifier = Modifier.size(24.dp),
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
                if (playerList.size < 24) {
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
                                playerList = playerList.toMutableList().also {
                                    it.add(PlayerSetupItem(name = "Player ${it.size + 1}"))
                                }
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
                        val maxAllowed = maxImpostersForPlayerCount(playerList.size)
                        val recommended = maxAllowed

                        // Auto-clamp if current value exceeds allowed maximum
                        LaunchedEffect(playerList.size) {
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
                        val availableRanges = validAutoRanges(playerList.size)
                        val recommended = recommendedAutoRange(playerList.size)

                        LaunchedEffect(playerList.size) {
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
                                text = "Recommended: ${recommended.label} for ${playerList.size} players",
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
                    val resolved = playerList.mapIndexed { index, item ->
                        if (item.name.isBlank()) "Player ${index + 1}" else item.name.trim()
                    }.toMutableList()
                    while (resolved.size < 3) {
                        resolved.add("Player ${resolved.size + 1}")
                    }
                    playerList = resolved.map { PlayerSetupItem(name = it) }
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
