package com.example.imposterparty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun WordPackEditScreen(
    gameViewModel: GameViewModel,
    packId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditing = packId > 0

    var packName by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(listOf(Pair("", ""))) }
    var initialized by remember { mutableStateOf(false) }

    // Load existing pack and entries when editing
    LaunchedEffect(packId) {
        if (isEditing) {
            val packs = gameViewModel.wordPacks.value
            val pack = packs.find { it.id == packId }
            if (pack != null) {
                packName = pack.name
            }
            gameViewModel.getEntriesForPack(packId).collect { loaded ->
                if (!initialized && loaded.isNotEmpty()) {
                    entries = loaded.map { Pair(it.word, it.clue ?: "") }
                    initialized = true
                }
            }
        }
    }

    val canSave = packName.isNotBlank() && entries.any { it.first.isNotBlank() }

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
                    if (isEditing) "Edit Word Pack" else "New Word Pack",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextOnDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = {
                        val validEntries = entries
                            .filter { it.first.isNotBlank() }
                            .map { Pair(it.first, it.second.takeIf { c -> c.isNotBlank() }) }

                        if (isEditing) {
                            gameViewModel.updateWordPack(packId, packName, validEntries)
                        } else {
                            gameViewModel.saveWordPack(packName, validEntries)
                        }
                        onSaved()
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SuccessGreen,
                        contentColor = DarkBackground,
                    ),
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Pack name
                item {
                    OutlinedTextField(
                        value = packName,
                        onValueChange = { packName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Sports, Music, Custom...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ImposterPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.3f),
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Words & Clues",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextOnDark,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add words to your pack. Clues are optional hints shown to civilians during the game.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextOnDarkSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Word entries
                itemsIndexed(entries) { index, (word, clue) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ImposterPrimary,
                                    modifier = Modifier.width(28.dp),
                                )
                                OutlinedTextField(
                                    value = word,
                                    onValueChange = { newWord ->
                                        entries = entries.toMutableList().also {
                                            it[index] = Pair(newWord, clue)
                                        }
                                    },
                                    placeholder = { Text("Secret word") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ImposterPrimary,
                                        unfocusedBorderColor = DarkSurfaceHigh,
                                    ),
                                )
                                if (entries.size > 1) {
                                    IconButton(onClick = {
                                        entries = entries.toMutableList().also { it.removeAt(index) }
                                    }) {
                                        Icon(Icons.Default.Close, "Remove", tint = DangerRed.copy(alpha = 0.6f))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = clue,
                                onValueChange = { newClue ->
                                    entries = entries.toMutableList().also {
                                        it[index] = Pair(word, newClue)
                                    }
                                },
                                placeholder = { Text("Clue (optional)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarningYellow,
                                    unfocusedBorderColor = DarkSurfaceHigh,
                                ),
                            )
                        }
                    }
                }

                // Add word button
                item {
                    TextButton(
                        onClick = {
                            entries = entries + Pair("", "")
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, null, tint = ImposterSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Word", color = ImposterSecondary)
                    }
                }
            }
        }
    }
}
