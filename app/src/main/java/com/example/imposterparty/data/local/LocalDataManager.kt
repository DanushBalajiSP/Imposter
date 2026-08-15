package com.example.imposterparty.data.local

import android.content.Context
import com.example.imposterparty.data.model.*
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
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val rank: Int = 1,
)

class LocalDataManager(private val context: Context) {

    private val gson = Gson()
    private val customPacksFile = File(context.filesDir, "custom_packs.json")
    private val customEntriesFile = File(context.filesDir, "custom_entries.json")
    private val scoresFile = File(context.filesDir, "scores.json")
    private val historyFile = File(context.filesDir, "game_history.json")
    private val lastPlayersFile = File(context.filesDir, "last_players.json")
    private val matchesFile = File(context.filesDir, "matches.json")
    private val activeMatchFile = File(context.filesDir, "active_match.json")
    private val activeGameStateFile = File(context.filesDir, "active_game_state.json")

    private val _allPacks = MutableStateFlow<List<WordPack>>(emptyList())
    val allPacks: Flow<List<WordPack>> = _allPacks.asStateFlow()

    private val _allEntries = MutableStateFlow<List<WordEntry>>(emptyList())
    val allEntries: Flow<List<WordEntry>> = _allEntries.asStateFlow()

    private val _allScores = MutableStateFlow<List<ScoreRecord>>(emptyList())
    val allScores: Flow<List<ScoreRecord>> = _allScores.asStateFlow()

    private val _allHistory = MutableStateFlow<List<GameHistoryRecord>>(emptyList())
    val allHistory: Flow<List<GameHistoryRecord>> = _allHistory.asStateFlow()

    private val _allMatches = MutableStateFlow<List<MatchSession>>(emptyList())
    val allMatches: Flow<List<MatchSession>> = _allMatches.asStateFlow()

    private val _activeMatch = MutableStateFlow<MatchSession?>(null)
    val activeMatch: Flow<MatchSession?> = _activeMatch.asStateFlow()

    private val _lastPlayerNames = MutableStateFlow<List<String>>(emptyList())
    val lastPlayerNames: Flow<List<String>> = _lastPlayerNames.asStateFlow()

    val leaderboard: Flow<List<LeaderboardEntry>> = _allScores.map { scores ->
        val unranked = scores
            .filter { it.playerName.isNotBlank() }
            .groupBy { it.playerName.trim().lowercase() }
            .map { (_, playerScores) ->
                val displayName = playerScores.lastOrNull()?.playerName?.trim() ?: "Player"
                val totalPoints = playerScores.sumOf { it.points }
                val gamesPlayed = playerScores.size
                val gamesWon = playerScores.count { it.won }
                LeaderboardEntry(
                    playerName = displayName,
                    totalPoints = totalPoints,
                    gamesPlayed = gamesPlayed,
                    gamesWon = gamesWon,
                )
            }
            .sortedWith(
                compareByDescending<LeaderboardEntry> { it.totalPoints }
                    .thenByDescending { if (it.gamesPlayed > 0) it.gamesWon.toDouble() / it.gamesPlayed else 0.0 }
                    .thenByDescending { it.gamesWon }
                    .thenByDescending { it.gamesPlayed }
                    .thenBy { it.playerName.lowercase() }
            )

        var currentRank = 1
        val ranked = mutableListOf<LeaderboardEntry>()
        for (i in unranked.indices) {
            if (i > 0) {
                val prev = unranked[i - 1]
                val curr = unranked[i]
                val isTied = prev.totalPoints == curr.totalPoints &&
                        prev.gamesPlayed == curr.gamesPlayed &&
                        prev.gamesWon == curr.gamesWon
                if (!isTied) {
                    currentRank++
                }
            }
            ranked.add(unranked[i].copy(rank = currentRank))
        }
        ranked
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

        // Load scores, history, and matches
        _allScores.value = loadScores()
        _allHistory.value = loadHistory()
        _lastPlayerNames.value = loadLastPlayerNames()
        _allMatches.value = loadMatches()
        _activeMatch.value = loadActiveMatch()
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

    suspend fun saveScoresAndHistory(scores: List<ScoreRecord>, history: GameHistoryRecord?) = withContext(Dispatchers.IO) {
        val updatedScores = _allScores.value + scores
        _allScores.value = updatedScores
        try {
            scoresFile.writeText(gson.toJson(updatedScores))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (history != null) {
            val updatedHistory = listOf(history) + _allHistory.value
            _allHistory.value = updatedHistory
            try {
                historyFile.writeText(gson.toJson(updatedHistory))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveMatchSession(match: MatchSession) = withContext(Dispatchers.IO) {
        _activeMatch.value = match
        val existing = _allMatches.value.filter { it.id != match.id }
        val updatedMatches = listOf(match) + existing
        _allMatches.value = updatedMatches
        try {
            matchesFile.writeText(gson.toJson(updatedMatches))
            activeMatchFile.writeText(gson.toJson(match))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveLastPlayerNames(names: List<String>) = withContext(Dispatchers.IO) {
        val filtered = names.filter { it.isNotBlank() }
        _lastPlayerNames.value = filtered
        try {
            lastPlayersFile.writeText(gson.toJson(filtered))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePlayerScores(playerName: String) = withContext(Dispatchers.IO) {
        val normalizedTarget = playerName.trim().lowercase()
        val updatedScores = _allScores.value.filter { it.playerName.trim().lowercase() != normalizedTarget }
        _allScores.value = updatedScores
        try {
            scoresFile.writeText(gson.toJson(updatedScores))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteMatchSession(matchId: Long) = withContext(Dispatchers.IO) {
        val updatedMatches = _allMatches.value.filter { it.id != matchId }
        _allMatches.value = updatedMatches
        if (_activeMatch.value?.id == matchId) {
            _activeMatch.value = null
            if (activeMatchFile.exists()) activeMatchFile.delete()
        }
        try {
            matchesFile.writeText(gson.toJson(updatedMatches))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveActiveGameState(state: GameState) = withContext(Dispatchers.IO) {
        try {
            if (state.phase == GamePhase.SETUP) {
                if (activeGameStateFile.exists()) activeGameStateFile.delete()
            } else {
                activeGameStateFile.writeText(gson.toJson(state))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadActiveGameState(): GameState? = withContext(Dispatchers.IO) {
        if (!activeGameStateFile.exists()) return@withContext null
        try {
            gson.fromJson(activeGameStateFile.readText(), GameState::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearActiveGameState() = withContext(Dispatchers.IO) {
        try {
            if (activeGameStateFile.exists()) activeGameStateFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAllScores() = withContext(Dispatchers.IO) {
        _allScores.value = emptyList()
        _allHistory.value = emptyList()
        _allMatches.value = emptyList()
        _activeMatch.value = null
        if (scoresFile.exists()) scoresFile.delete()
        if (historyFile.exists()) historyFile.delete()
        if (matchesFile.exists()) matchesFile.delete()
        if (activeMatchFile.exists()) activeMatchFile.delete()
        if (activeGameStateFile.exists()) activeGameStateFile.delete()
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

    private fun loadHistory(): List<GameHistoryRecord> {
        if (!historyFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<GameHistoryRecord>>() {}.type
            gson.fromJson(historyFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadMatches(): List<MatchSession> {
        if (!matchesFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<MatchSession>>() {}.type
            gson.fromJson(matchesFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadActiveMatch(): MatchSession? {
        if (!activeMatchFile.exists()) return null
        return try {
            gson.fromJson(activeMatchFile.readText(), MatchSession::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadLastPlayerNames(): List<String> {
        if (!lastPlayersFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(lastPlayersFile.readText(), type) ?: emptyList()
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
