package com.example.imposterparty.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var packToDelete by remember { mutableStateOf<WordPack?>(null) }

    // Delete confirmation dialog
    if (packToDelete != null) {
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            title = { Text("Delete Word Pack") },
            text = { Text("Are you sure you want to delete \"${packToDelete?.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    packToDelete?.let { gameViewModel.deleteWordPack(it) }
                    packToDelete = null
                }) {
                    Text("Delete", color = DangerRed)
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
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextOnDark)
                }
                Text(
                    "📦 Word Packs",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextOnDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = onCreateNew,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ImposterPrimary,
                        contentColor = TextOnPrimary,
                    ),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(wordPacks) { pack ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pack.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextOnDark,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    if (pack.isBuiltIn) "Built-in" else "Custom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (pack.isBuiltIn) ImposterSecondary else ImposterTertiary,
                                )
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
