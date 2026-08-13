package com.example.imposterparty.data.local

import android.content.Context
import com.example.imposterparty.data.model.ScoreRecord
import com.example.imposterparty.data.model.WordEntry
import com.example.imposterparty.data.model.WordPack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

data class LeaderboardEntry(
    val playerName: String,
    val totalPoints: Int,
)

class LocalDataManager(private val context: Context) {

    private val gson = Gson()
    private val customPacksFile = File(context.filesDir, "custom_packs.json")
    private val customEntriesFile = File(context.filesDir, "custom_entries.json")
    private val scoresFile = File(context.filesDir, "scores.json")

    private val _allPacks = MutableStateFlow<List<WordPack>>(emptyList())
    val allPacks: Flow<List<WordPack>> = _allPacks.asStateFlow()

    private val _allEntries = MutableStateFlow<List<WordEntry>>(emptyList())
    val allEntries: Flow<List<WordEntry>> = _allEntries.asStateFlow()

    private val _allScores = MutableStateFlow<List<ScoreRecord>>(emptyList())
    val allScores: Flow<List<ScoreRecord>> = _allScores.asStateFlow()

    val leaderboard: Flow<List<LeaderboardEntry>> = _allScores.map { scores ->
        scores.groupBy { it.playerName }
            .map { (name, playerScores) ->
                LeaderboardEntry(playerName = name, totalPoints = playerScores.sumOf { it.points })
            }
            .sortedByDescending { it.totalPoints }
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Load built-in packs from assets
        val builtInPacks = mutableListOf<WordPack>()
        val builtInEntries = mutableListOf<WordEntry>()

        val packFiles = context.assets.list("wordpacks") ?: emptyArray()
        var currentPackId = 1L
        var currentEntryId = 1L

        for (file in packFiles) {
            if (!file.endsWith(".json")) continue
            try {
                val json = context.assets.open("wordpacks/$file").bufferedReader().use { it.readText() }
                val parsed = gson.fromJson(json, JsonWordPack::class.java)
                val pack = WordPack(id = currentPackId, name = parsed.name, isBuiltIn = true)
                builtInPacks.add(pack)

                for (w in parsed.words) {
                    builtInEntries.add(
                        WordEntry(id = currentEntryId++, packId = currentPackId, word = w.word, clue = w.clue)
                    )
                }
                currentPackId++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load custom packs
        val customPacks = loadCustomPacks()
        val customEntries = loadCustomEntries()

        _allPacks.value = builtInPacks + customPacks
        _allEntries.value = builtInEntries + customEntries

        // Load scores
        _allScores.value = loadScores()
    }

    fun getPackById(packId: Long): WordPack? {
        return _allPacks.value.find { it.id == packId }
    }

    fun getEntriesForPack(packId: Long): Flow<List<WordEntry>> {
        return _allEntries.map { entries -> entries.filter { it.packId == packId } }
    }

    fun getEntriesForPackOnce(packId: Long): List<WordEntry> {
        return _allEntries.value.filter { it.packId == packId }
    }

    suspend fun saveWordPack(name: String, entries: List<Pair<String, String?>>) = withContext(Dispatchers.IO) {
        val newPackId = System.currentTimeMillis()
        val newPack = WordPack(id = newPackId, name = name, isBuiltIn = false)

        val newEntries = entries.mapIndexed { index, (word, clue) ->
            WordEntry(
                id = newPackId + index + 1,
                packId = newPackId,
                word = word,
                clue = clue?.takeIf { it.isNotBlank() },
            )
        }

        val updatedPacks = _allPacks.value + newPack
        val updatedEntries = _allEntries.value + newEntries

        _allPacks.value = updatedPacks
        _allEntries.value = updatedEntries

        saveCustomPacksToDisk(updatedPacks.filter { !it.isBuiltIn })
        saveCustomEntriesToDisk(updatedEntries.filter { entry ->
            val pack = updatedPacks.find { it.id == entry.packId }
            pack != null && !pack.isBuiltIn
        })
    }

    suspend fun updateWordPack(packId: Long, name: String, entries: List<Pair<String, String?>>) = withContext(Dispatchers.IO) {
        val updatedPacks = _allPacks.value.map {
            if (it.id == packId) it.copy(name = name) else it
        }

        val filteredEntries = _allEntries.value.filter { it.packId != packId }
        val newEntries = entries.mapIndexed { index, (word, clue) ->
            WordEntry(
                id = packId + index + 1,
                packId = packId,
                word = word,
                clue = clue?.takeIf { it.isNotBlank() },
            )
        }
        val updatedEntries = filteredEntries + newEntries

        _allPacks.value = updatedPacks
        _allEntries.value = updatedEntries

        saveCustomPacksToDisk(updatedPacks.filter { !it.isBuiltIn })
        saveCustomEntriesToDisk(updatedEntries.filter { entry ->
            val pack = updatedPacks.find { it.id == entry.packId }
            pack != null && !pack.isBuiltIn
        })
    }

    suspend fun deleteWordPack(pack: WordPack) = withContext(Dispatchers.IO) {
        val updatedPacks = _allPacks.value.filter { it.id != pack.id }
        val updatedEntries = _allEntries.value.filter { it.packId != pack.id }

        _allPacks.value = updatedPacks
        _allEntries.value = updatedEntries

        saveCustomPacksToDisk(updatedPacks.filter { !it.isBuiltIn })
        saveCustomEntriesToDisk(updatedEntries.filter { entry ->
            val p = updatedPacks.find { it.id == entry.packId }
            p != null && !p.isBuiltIn
        })
    }

    suspend fun saveScores(scores: List<ScoreRecord>) = withContext(Dispatchers.IO) {
        val updated = _allScores.value + scores
        _allScores.value = updated
        scoresFile.writeText(gson.toJson(updated))
    }

    suspend fun clearAllScores() = withContext(Dispatchers.IO) {
        _allScores.value = emptyList()
        if (scoresFile.exists()) scoresFile.delete()
    }

    private fun loadCustomPacks(): List<WordPack> {
        if (!customPacksFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<WordPack>>() {}.type
            gson.fromJson(customPacksFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadCustomEntries(): List<WordEntry> {
        if (!customEntriesFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<WordEntry>>() {}.type
            gson.fromJson(customEntriesFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadScores(): List<ScoreRecord> {
        if (!scoresFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<ScoreRecord>>() {}.type
            gson.fromJson(scoresFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomPacksToDisk(packs: List<WordPack>) {
        try {
            customPacksFile.writeText(gson.toJson(packs))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCustomEntriesToDisk(entries: List<WordEntry>) {
        try {
            customEntriesFile.writeText(gson.toJson(entries))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private data class JsonWordPack(
        val name: String,
        val words: List<JsonWordEntry> = emptyList(),
    )

    private data class JsonWordEntry(
        val word: String,
        val clue: String? = null,
    )
}
