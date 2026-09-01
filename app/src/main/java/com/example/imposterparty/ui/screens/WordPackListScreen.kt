package com.example.imposterparty.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.CommunityWordPack
import com.example.imposterparty.data.model.UserProfile
import com.example.imposterparty.data.model.WordPack
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

private enum class PackFilter(val label: String) {
    ALL("All"),
    BUILT_IN("Built-in"),
    CUSTOM("Custom"),
    COMMUNITY("🌐 Community"),
}

@Composable
fun WordPackListScreen(
    gameViewModel: GameViewModel,
    onCreateNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val wordPacks by gameViewModel.wordPacks.collectAsStateWithLifecycle()
    val communityPacks by gameViewModel.communityPacks.collectAsStateWithLifecycle()
    val currentUser by gameViewModel.currentUser.collectAsStateWithLifecycle()
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val settings = gameState.settings

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PackFilter.ALL) }
    var packToDelete by remember { mutableStateOf<WordPack?>(null) }
    var communityPackToDelete by remember { mutableStateOf<CommunityWordPack?>(null) }
    var publishingPackId by remember { mutableStateOf<Long?>(null) }
    var downloadingPackId by remember { mutableStateOf<String?>(null) }

    // Filtered local packs
    val filteredLocalPacks = remember(wordPacks, searchQuery, selectedFilter) {
        wordPacks.filter { pack ->
            val matchesSearch = searchQuery.isBlank() || pack.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                PackFilter.ALL -> true
                PackFilter.BUILT_IN -> pack.isBuiltIn
                PackFilter.CUSTOM -> !pack.isBuiltIn
                PackFilter.COMMUNITY -> false
            }
            matchesSearch && matchesFilter
        }
    }

    // Filtered community packs
    val filteredCommunityPacks = remember(communityPacks, searchQuery, selectedFilter) {
        if (selectedFilter != PackFilter.COMMUNITY) emptyList()
        else {
            communityPacks.filter { pack ->
                searchQuery.isBlank() ||
                        pack.name.contains(searchQuery, ignoreCase = true) ||
                        pack.authorName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var showResetWeightageDialog by remember { mutableStateOf(false) }

    // Reset Weightage confirmation dialog
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

    // Delete local pack dialog
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

    // Delete community pack dialog
    if (communityPackToDelete != null) {
        AlertDialog(
            onDismissRequest = { communityPackToDelete = null },
            containerColor = StitchSurfaceContainer,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Remove Community Pack",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove \"${communityPackToDelete?.name}\" from the global Community Hub? Other players won't be able to download it anymore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        communityPackToDelete?.let { cp ->
                            gameViewModel.deleteCommunityPack(cp.id) { result ->
                                result.onSuccess {
                                    Toast.makeText(context, "Pack removed from community.", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "Failed to remove: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        communityPackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { communityPackToDelete = null },
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
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp),
        ) {
            // ── Top App Bar ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
            ) {
                // Left: Back button
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

                // Title: Takes available space
                Text(
                    text = "📦 Word Packs",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        fontSize = 22.sp,
                    ),
                    color = PrimaryContainerNeon,
                    modifier = Modifier.weight(1f),
                )

                // Right: "Reset Weights" Button
                IconButton(
                    onClick = { showResetWeightageDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceContainerHigh.copy(alpha = 0.6f))
                        .border(BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Randomizer Weights",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Subtitle
            Text(
                text = if (selectedFilter == PackFilter.COMMUNITY)
                    "Browse & download packs published by players worldwide."
                else
                    "Choose the categories your party wants to play with.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = OnSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
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
                                    text = if (selectedFilter == PackFilter.COMMUNITY) "Search community packs or authors..." else "Search word packs...",
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

            Spacer(Modifier.height(14.dp))

            // ── Local vs Community Content ──
            if (selectedFilter == PackFilter.COMMUNITY) {
                // ── Community Packs List ──
                if (filteredCommunityPacks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌐", fontSize = 40.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching community packs found" else "No community packs published yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Create a custom pack and tap 'Publish' to share it here!",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filteredCommunityPacks, key = { it.id }) { communityPack ->
                            val isDownloaded = wordPacks.any { localPack ->
                                localPack.name.startsWith(communityPack.name, ignoreCase = true)
                            }
                            val isAuthor = currentUser != null && currentUser?.userId == communityPack.authorId
                            val isDownloading = downloadingPackId == communityPack.id

                            CommunityPackCard(
                                pack = communityPack,
                                isDownloaded = isDownloaded,
                                isAuthor = isAuthor,
                                isDownloading = isDownloading,
                                onDownload = {
                                    downloadingPackId = communityPack.id
                                    gameViewModel.downloadCommunityPack(communityPack) { result ->
                                        downloadingPackId = null
                                        result.onSuccess {
                                            Toast.makeText(context, "Downloaded \"${communityPack.name}\" to your packs!", Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            Toast.makeText(context, "Download failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDelete = {
                                    communityPackToDelete = communityPack
                                },
                            )
                        }
                    }
                }
            } else {
                // ── Local Packs Selection Row ──
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

                // ── Local Word Packs List ──
                if (filteredLocalPacks.isEmpty()) {
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
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filteredLocalPacks, key = { it.id }) { pack ->
                            val isSelected = settings.selectedCategoryIds.isEmpty() || settings.selectedCategoryIds.contains(pack.id)
                            val isPublishing = publishingPackId == pack.id

                            WordPackItemCard(
                                pack = pack,
                                isSelected = isSelected,
                                isPublishing = isPublishing,
                                currentUser = currentUser,
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
                                onPublish = {
                                    if (currentUser == null) {
                                        Toast.makeText(context, "Please set up your profile from the Home screen first.", Toast.LENGTH_LONG).show()
                                    } else {
                                        publishingPackId = pack.id
                                        gameViewModel.publishWordPackToCommunity(pack) { result ->
                                            publishingPackId = null
                                            result.onSuccess {
                                                Toast.makeText(context, "🎉 \"${pack.name}\" successfully published to Community Hub!", Toast.LENGTH_LONG).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Publish failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // ── Floating Action Button: + New Pack ──
        FloatingActionButton(
            onClick = onCreateNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            containerColor = Color.Transparent,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(NeonPurpleGlow, NeonPurple)),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .border(
                        BorderStroke(1.dp, PrimaryContainerNeon.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "New Pack",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun WordPackItemCard(
    pack: WordPack,
    isSelected: Boolean,
    isPublishing: Boolean,
    currentUser: UserProfile?,
    gameViewModel: GameViewModel,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPublish: () -> Unit,
) {
    val entries by gameViewModel.getEntriesForPack(pack.id).collectAsStateWithLifecycle(initialValue = emptyList())

    // Parse clean name and author tag
    val (cleanName, authorTag) = remember(pack.name, pack.authorName, currentUser?.username) {
        if (!pack.authorName.isNullOrBlank()) {
            pack.name to pack.authorName
        } else {
            val regex = Regex("""^(.*?)\s*\(([^)]+)\)$""")
            val match = regex.find(pack.name)
            if (match != null && !pack.isBuiltIn) {
                match.groupValues[1].trim() to match.groupValues[2].trim()
            } else {
                pack.name to (if (pack.isBuiltIn) null else currentUser?.username ?: "You")
            }
        }
    }

    // Determine if the current user owns this pack (locally created or matches profile username)
    val isOwnPack = remember(pack.isBuiltIn, pack.authorName, authorTag, currentUser?.username) {
        if (pack.isBuiltIn) false
        else {
            val currentName = currentUser?.username
            when {
                pack.authorName == null -> true // Locally created pack
                pack.authorName.equals("You", ignoreCase = true) -> true
                currentName != null && pack.authorName.equals(currentName, ignoreCase = true) -> true
                authorTag == null || authorTag.equals("You", ignoreCase = true) -> true
                currentName != null && authorTag.equals(currentName, ignoreCase = true) -> true
                else -> false
            }
        }
    }

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
                    text = cleanName,
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
                    if (pack.isBuiltIn) {
                        // Built-in badge (Cyan)
                        Surface(
                            color = NeonCyan.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = "BUILT-IN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = NeonCyanSoft,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    } else {
                        // Author name chip (Yellow / NeonGold)
                        val displayAuthor = authorTag ?: currentUser?.username ?: "You"
                        Surface(
                            color = NeonGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NeonGold.copy(alpha = 0.4f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = NeonGold,
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = displayAuthor,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                    ),
                                    color = NeonGold,
                                )
                            }
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

            // Edit, Publish & Delete Buttons for Custom / Community Packs
            if (!pack.isBuiltIn) {
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Upload / Publish Button (ONLY shown for locally created / owned packs)
                    if (isOwnPack) {
                        IconButton(
                            onClick = onPublish,
                            enabled = !isPublishing,
                            modifier = Modifier.size(32.dp),
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonCyan,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Publish to Community",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    // Edit Button (Enabled for owned packs, Disabled for cloned/community packs)
                    IconButton(
                        onClick = onEdit,
                        enabled = isOwnPack,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isOwnPack) "Edit pack" else "Community pack (Read only)",
                            tint = if (isOwnPack) PrimaryContainerNeon else OnSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // Delete Button (Always available so user can remove custom or downloaded packs)
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

@Composable
private fun CommunityPackCard(
    pack: CommunityWordPack,
    isDownloaded: Boolean,
    isAuthor: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(StitchSurfaceContainer)
            .border(
                BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.35f)),
                RoundedCornerShape(24.dp),
            )
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Pack Title & Author
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                        color = Color.White,
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Author badge
                        Surface(
                            color = NeonCyan.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = NeonCyanSoft,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = pack.authorName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                    ),
                                    color = NeonCyanSoft,
                                )
                            }
                        }

                        // Word count
                        Text(
                            text = "• ${pack.wordCount} words",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = OnSurfaceVariant.copy(alpha = 0.7f),
                        )

                        // Download count
                        if (pack.downloadCount > 0) {
                            Text(
                                text = "• 🔥 ${pack.downloadCount}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = NeonGold,
                            )
                        }
                    }
                }

                // Delete option if author
                if (isAuthor) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove pack",
                            tint = DangerRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }

                // Download / Status Button
                if (isDownloaded) {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Added",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = SuccessGreen,
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onDownload,
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.8f), NeonPurple)))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Get Pack",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
