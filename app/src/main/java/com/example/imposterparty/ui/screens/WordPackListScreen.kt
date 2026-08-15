package com.example.imposterparty.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imposterparty.data.model.WordPack
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

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
    var packToDelete by remember { mutableStateOf<WordPack?>(null) }

    // Delete confirmation dialog
    if (packToDelete != null) {
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            title = { Text("Delete Word Pack", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${packToDelete?.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    packToDelete?.let { gameViewModel.deleteWordPack(it) }
                    packToDelete = null
                }) {
                    Text("Delete", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { packToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(DarkBackground, DarkSurface))
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextOnDark)
                }
                Text(
                    "📦 Word Packs",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    color = TextOnDark,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onCreateNew,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImposterPrimary,
                        contentColor = TextOnPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "New Pack",
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            // Selection controls bar for game rounds
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                val selectedCount = if (settings.selectedCategoryIds.isEmpty()) {
                    "All ${wordPacks.size} Active"
                } else {
                    "${settings.selectedCategoryIds.size} Selected for Game"
                }

                Text(
                    selectedCount,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = ImposterSecondary,
                    modifier = Modifier.weight(1f),
                )

                if (wordPacks.isNotEmpty()) {
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
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(wordPacks) { pack ->
                    val isSelected = settings.selectedCategoryIds.isEmpty() || settings.selectedCategoryIds.contains(pack.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) DarkSurfaceVariant.copy(alpha = 0.85f)
                            else DarkSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) ImposterSecondary.copy(alpha = 0.7f) else DarkSurfaceHigh
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val current = if (settings.selectedCategoryIds.isEmpty()) {
                                        wordPacks.map { it.id }.toSet()
                                    } else {
                                        settings.selectedCategoryIds
                                    }
                                    val newSelected = if (checked) {
                                        current + pack.id
                                    } else {
                                        current - pack.id
                                    }
                                    gameViewModel.updateSettings(settings.copy(selectedCategoryIds = newSelected))
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ImposterSecondary,
                                    uncheckedColor = TextOnDarkSecondary,
                                    checkmarkColor = DarkBackground,
                                ),
                            )

                            Spacer(Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pack.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                    ),
                                    color = if (isSelected) TextOnDark else TextOnDarkSecondary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = if (pack.isBuiltIn) ImposterSecondary.copy(alpha = 0.15f) else ImposterTertiary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (pack.isBuiltIn) ImposterSecondary.copy(alpha = 0.4f) else ImposterTertiary.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Text(
                                            if (pack.isBuiltIn) "Built-in" else "Custom",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                            ),
                                            color = if (pack.isBuiltIn) ImposterSecondary else ImposterTertiary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }

                                    if (isSelected) {
                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                                        ) {
                                            Text(
                                                "Active for Next Game",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = SuccessGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            if (!pack.isBuiltIn) {
                                IconButton(onClick = { onEdit(pack.id) }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = ImposterPrimary)
                                }
                                IconButton(onClick = { packToDelete = pack }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = DangerRed.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
