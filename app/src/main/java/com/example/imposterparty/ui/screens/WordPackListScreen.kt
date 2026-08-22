package com.example.imposterparty.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.WordPack
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

private enum class PackFilter(val label: String) {
    ALL("All"),
    BUILT_IN("Built-in"),
    CUSTOM("Custom"),
}

@Composable
fun WordPackListScreen(
    gameViewModel: GameViewModel,
    onCreateNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wordPacks by gameViewModel.wordPacks.collectAsStateWithLifecycle()
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val settings = gameState.settings

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PackFilter.ALL) }
    var packToDelete by remember { mutableStateOf<WordPack?>(null) }

    // Filtered word packs based on search & tab
    val filteredPacks = remember(wordPacks, searchQuery, selectedFilter) {
        wordPacks.filter { pack ->
            val matchesSearch = searchQuery.isBlank() || pack.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                PackFilter.ALL -> true
                PackFilter.BUILT_IN -> pack.isBuiltIn
                PackFilter.CUSTOM -> !pack.isBuiltIn
            }
            matchesSearch && matchesFilter
        }
    }

    var showResetWeightageDialog by remember { mutableStateOf(false) }

    // Reset Weightage confirmation dialog (Deep Space theme)
    if (showResetWeightageDialog) {
        AlertDialog(
            onDismissRequest = { showResetWeightageDialog = false },
            containerColor = StitchSurfaceContainer,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Reset Randomizer Weightage",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                }
            },
            text = {
                Text(
                    "This will reset all secret word cooldowns and player imposter frequency counters back to default equal weights across all matches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        gameViewModel.resetRandomizerWeightage()
                        showResetWeightageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Reset Weights", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetWeightageDialog = false },
                ) {
                    Text("Cancel", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    // Delete confirmation dialog (Deep Space theme)
    if (packToDelete != null) {
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            containerColor = StitchSurfaceContainer,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Delete Word Pack",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${packToDelete?.name}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        packToDelete?.let { gameViewModel.deleteWordPack(it) }
                        packToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { packToDelete = null },
                ) {
                    Text("Cancel", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    Box(
        modifier = modifier
            .background(DeepSpaceBg)
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            // ── Top App Bar ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
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
                        contentDescription = "Back",
                        tint = OnSurfaceVariant,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "📦 Word Packs",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        fontSize = 24.sp,
                    ),
                    color = PrimaryContainerNeon,
                    modifier = Modifier.weight(1f),
                )

                // "Reset Weights" Button
                IconButton(
                    onClick = { showResetWeightageDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceContainerHigh.copy(alpha = 0.6f))
                        .border(BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Randomizer Weights",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.width(8.dp))

                // "+ New Pack" Button
                Button(
                    onClick = onCreateNew,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Brush.verticalGradient(listOf(NeonPurpleGlow, NeonPurple)))
                            .border(BorderStroke(1.dp, PrimaryContainerNeon.copy(alpha = 0.4f)), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "New Pack",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            // Subtitle
            Text(
                text = "Choose the worlds your party wants to play in.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 52.dp, bottom = 16.dp),
            )

            // ── Search Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(StitchSurfaceContainerHigh)
                    .border(BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        cursorBrush = SolidColor(NeonPurpleGlow),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search word packs...",
                                    style = TextStyle(
                                        color = OnSurfaceVariant.copy(alpha = 0.45f),
                                        fontSize = 15.sp,
                                    ),
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Filter Pills ──
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(PackFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
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
                                    if (isSelected) PrimaryContainerNeon.copy(alpha = 0.5f) else OutlineSubtle.copy(alpha = 0.3f),
                                ),
                                RoundedCornerShape(9999.dp),
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 0.5.sp,
                            ),
                            color = if (isSelected) Color.White else OnSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Selection Status & Select All Row ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                val selectedCount = if (settings.selectedCategoryIds.isEmpty()) {
                    "All ${wordPacks.size} Selected for Game"
                } else {
                    "${settings.selectedCategoryIds.size} Selected for Game"
                }

                Text(
                    text = selectedCount.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontSize = 12.sp,
                    ),
                    color = NeonCyan,
                    modifier = Modifier.weight(1f),
                )

                if (wordPacks.isNotEmpty()) {
                    Text(
                        text = if (settings.selectedCategoryIds.size == wordPacks.size) "Clear" else "Select All",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = PrimaryContainerNeon,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val allIds = wordPacks.map { it.id }.toSet()
                                val newSelected = if (settings.selectedCategoryIds.size == allIds.size) {
                                    emptySet()
                                } else {
                                    allIds
                                }
                                gameViewModel.updateSettings(settings.copy(selectedCategoryIds = newSelected))
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Word Packs List ──
            if (filteredPacks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No word packs found",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredPacks, key = { it.id }) { pack ->
                        val isSelected = settings.selectedCategoryIds.isEmpty() || settings.selectedCategoryIds.contains(pack.id)

                        // Word pack item card
                        WordPackItemCard(
                            pack = pack,
                            isSelected = isSelected,
                            gameViewModel = gameViewModel,
                            onToggleSelect = {
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
                            },
                            onEdit = { onEdit(pack.id) },
                            onDelete = { packToDelete = pack },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordPackItemCard(
    pack: WordPack,
    isSelected: Boolean,
    gameViewModel: GameViewModel,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val entries by gameViewModel.getEntriesForPack(pack.id).collectAsStateWithLifecycle(initialValue = emptyList())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) StitchSurfaceContainerHigh else StitchSurfaceContainer)
            .border(
                BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) NeonCyan.copy(alpha = 0.85f) else OutlineSubtle.copy(alpha = 0.35f),
                ),
                RoundedCornerShape(24.dp),
            )
            .clickable { onToggleSelect() }
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Custom Rounded Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) NeonCyan else Color.Transparent)
                    .border(
                        BorderStroke(
                            if (isSelected) 1.5.dp else 2.dp,
                            if (isSelected) NeonCyan else OutlineSubtle.copy(alpha = 0.6f),
                        ),
                        RoundedCornerShape(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = DeepSpaceBg,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Pack Details
            Column(modifier = Modifier.weight(1f)) {
                // Name
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = Color.White,
                )

                Spacer(Modifier.height(6.dp))

                // Tags Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Built-in or Custom badge
                    Surface(
                        color = if (pack.isBuiltIn) NeonCyan.copy(alpha = 0.12f) else NeonGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            if (pack.isBuiltIn) NeonCyan.copy(alpha = 0.3f) else NeonGold.copy(alpha = 0.3f),
                        ),
                    ) {
                        Text(
                            text = (if (pack.isBuiltIn) "Built-in" else "Custom").uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                            ),
                            color = if (pack.isBuiltIn) NeonCyanSoft else NeonGold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    // Active for Next Game badge (if selected)
                    if (isSelected) {
                        Surface(
                            color = NeonCyan.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = "ACTIVE FOR NEXT GAME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = NeonCyanSoft,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    // Words count
                    if (entries.isNotEmpty()) {
                        Text(
                            text = "• ${entries.size} words",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                            ),
                            color = OnSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Edit & Delete Buttons for Custom Packs
            if (!pack.isBuiltIn) {
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit pack",
                            tint = PrimaryContainerNeon,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete pack",
                            tint = DangerRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
