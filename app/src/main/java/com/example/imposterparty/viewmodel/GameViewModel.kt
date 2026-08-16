package com.example.imposterparty.viewmodel

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imposterparty.data.local.LocalDataManager
import com.example.imposterparty.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val dataManager = LocalDataManager(application)

    private fun defaultPlayers(): List<Player> = listOf(
        Player(id = 0, name = "Player 1"),
        Player(id = 1, name = "Player 2"),
        Player(id = 2, name = "Player 3"),
    )

    private var currentMatchId: Long = System.currentTimeMillis()

    private val _gameState = MutableStateFlow(GameState(players = defaultPlayers()))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    val wordPacks = dataManager.allPacks
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val leaderboard = dataManager.leaderboard
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allScores = dataManager.allScores
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allHistory = dataManager.allHistory
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allMatches = dataManager.allMatches
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeMatch = dataManager.activeMatch
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private var countDownTimer: CountDownTimer? = null

    // Accumulated scores across rounds in the current session
    private val _sessionScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val sessionScores: StateFlow<Map<String, Int>> = _sessionScores.asStateFlow()

    init {
        viewModelScope.launch {
            dataManager.initialize()
            val savedInProgressState = dataManager.loadActiveGameState()
            if (savedInProgressState != null && savedInProgressState.phase != GamePhase.SETUP) {
                _gameState.value = savedInProgressState
                if (savedInProgressState.phase == GamePhase.DISCUSSION &&
                    savedInProgressState.settings.isTimerEnabled &&
                    savedInProgressState.timerRemainingSeconds > 0
                ) {
                    startTimer()
                }
            } else {
                val savedNames = dataManager.lastPlayerNames.first()
                if (savedNames.isNotEmpty()) {
                    val finalNames = if (savedNames.size < 3) {
                        val m = savedNames.toMutableList()
                        while (m.size < 3) m.add("Player ${m.size + 1}")
                        m
                    } else savedNames
                    val players = finalNames.mapIndexed { index, name ->
                        Player(id = index, name = name)
                    }
                    _gameState.update { it.copy(players = players) }
                }
            }
        }
    }

    private fun persistCurrentState(state: GameState) {
        viewModelScope.launch {
            dataManager.saveActiveGameState(state)
        }
    }

    // ── Setup ──────────────────────────────────────────────

    fun updateSettings(settings: GameSettings) {
        _gameState.update { it.copy(settings = settings) }
    }

    fun setPlayerNames(names: List<String>) {
        val finalNames = if (names.isEmpty()) {
            listOf("Player 1", "Player 2", "Player 3")
        } else {
            names.mapIndexed { index, name ->
                if (name.isBlank()) "Player ${index + 1}" else name.trim()
            }
        }
        val players = finalNames.mapIndexed { index, name ->
            Player(id = index, name = name)
        }
        _gameState.update { it.copy(players = players) }
        viewModelScope.launch {
            dataManager.saveLastPlayerNames(finalNames)
        }
    }

    // ── Resume / New Match ─────────────────────────────────

    fun startNewGame() {
        countDownTimer?.cancel()
        currentMatchId = System.currentTimeMillis()
        _sessionScores.value = emptyMap()
        val currentPlayers = _gameState.value.players
        val playerList = if (currentPlayers.isNotEmpty() && currentPlayers.size >= 3) {
            currentPlayers.mapIndexed { index, p ->
                p.copy(id = index, role = Role.CIVILIAN, hasRevealed = false, hasVoted = false, votedForId = null)
            }
        } else defaultPlayers()

        val newState = GameState(
            players = playerList,
            roundNumber = 1,
            settings = _gameState.value.settings
        )
        _gameState.value = newState
        persistCurrentState(newState)
    }

    fun resumeMatch(match: MatchSession) {
        countDownTimer?.cancel()
        currentMatchId = match.id
        _sessionScores.value = match.cumulativeScores
        val restoredPlayers = match.playerNames.mapIndexed { index, name ->
            Player(id = index, name = name, role = Role.CIVILIAN, hasRevealed = false, hasVoted = false)
        }
        val newState = GameState(
            players = restoredPlayers,
            roundNumber = match.totalRounds + 1,
            settings = match.settings
        )
        _gameState.value = newState
        persistCurrentState(newState)
    }

    // ── Start Game ─────────────────────────────────────────

    fun startGame() {
        viewModelScope.launch {
            val state = _gameState.value
            val settings = state.settings
            var players = state.players

            // Ensure at least 3 players
            if (players.size < 3) {
                val names = players.map { it.name }.toMutableList()
                while (names.size < 3) {
                    names.add("Player ${names.size + 1}")
                }
                players = names.mapIndexed { index, name ->
                    Player(id = index, name = if (name.isBlank()) "Player ${index + 1}" else name.trim())
                }
            } else {
                // Ensure no empty names
                players = players.mapIndexed { index, player ->
                    player.copy(name = if (player.name.isBlank()) "Player ${index + 1}" else player.name.trim())
                }
            }

            // Determine imposter count (respecting player-count ratio limits)
            val maxByRatio = maxImpostersForPlayerCount(players.size)
            val imposterCount = when (settings.imposterMode) {
                ImposterMode.MANUAL -> settings.manualImposterCount.coerceIn(1, minOf(maxByRatio, players.size - 1))
                ImposterMode.AUTO_RANGE -> {
                    val cappedMax = minOf(settings.autoRange.max, maxByRatio, players.size - 1)
                    val minAllowed = settings.autoRange.min.coerceAtMost(cappedMax)
                    (minAllowed..cappedMax).random()
                }
            }

            // Pick a random word from selected categories (or all categories if none selected)
            val entries = if (settings.selectedCategoryIds.isNotEmpty()) {
                val selectedEntries = settings.selectedCategoryIds.flatMap { packId ->
                    dataManager.getEntriesForPackOnce(packId)
                }
                if (selectedEntries.isNotEmpty()) selectedEntries else {
                    val packs = wordPacks.value
                    packs.flatMap { dataManager.getEntriesForPackOnce(it.id) }
                }
            } else {
                val packs = wordPacks.value
                packs.flatMap { dataManager.getEntriesForPackOnce(it.id) }
            }

            if (entries.isEmpty()) return@launch

            val selectedEntry = entries.random()
            val categoryName = dataManager.getPackById(selectedEntry.packId)?.name ?: ""

            // Assign roles
            val shuffledIndices = players.indices.shuffled()
            val imposterIndices = shuffledIndices.take(imposterCount).toSet()

            val assignedPlayers = players.mapIndexed { index, player ->
                player.copy(
                    role = if (index in imposterIndices) Role.IMPOSTER else Role.CIVILIAN,
                    hasRevealed = false,
                    hasVoted = false,
                    votedForId = null,
                )
            }

            // Card reveal order: Keep SAME order as initially given in the Lobby (0, 1, 2, ...)
            val revealOrder = assignedPlayers.indices.toList()

            // Randomly select ONE person who should start the conversation
            val startingSpeakerIndex = assignedPlayers.indices.random()

            // Timer
            val timerSeconds = if (settings.isTimerEnabled) {
                when (settings.timerDuration) {
                    TimerDuration.CUSTOM -> settings.customTimerSeconds
                    else -> settings.timerDuration.seconds
                }
            } else {
                0
            }

            val newState = state.copy(
                phase = GamePhase.REVEALING,
                players = assignedPlayers,
                secretWord = selectedEntry.word,
                secretClue = selectedEntry.clue,
                categoryName = categoryName,
                currentRevealIndex = 0,
                revealOrder = revealOrder,
                startingSpeakerIndex = startingSpeakerIndex,
                timerRemainingSeconds = timerSeconds,
                isTimerRunning = false,
                currentVoterIndex = 0,
                votes = emptyMap(),
                actualImposterCount = imposterCount,
                accusedPlayerId = null,
                isImposterFound = false,
            )
            _gameState.value = newState
            persistCurrentState(newState)
        }
    }

    // ── Reveal Phase ───────────────────────────────────────

    fun markCurrentPlayerRevealed() {
        val state = _gameState.value
        val revealOrder = state.revealOrder
        val currentIdx = state.currentRevealIndex

        if (currentIdx >= revealOrder.size) return

        val playerIdx = revealOrder[currentIdx]
        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[playerIdx] = updatedPlayers[playerIdx].copy(hasRevealed = true)

        val nextIndex = currentIdx + 1
        val allRevealed = nextIndex >= revealOrder.size

        val newState = state.copy(
            players = updatedPlayers,
            currentRevealIndex = nextIndex,
            phase = if (allRevealed) GamePhase.DISCUSSION else GamePhase.REVEALING,
        )
        _gameState.value = newState
        persistCurrentState(newState)

        if (allRevealed && state.settings.isTimerEnabled) {
            startTimer()
        }
    }

    fun getCurrentRevealPlayer(): Player? {
        val state = _gameState.value
        val idx = state.currentRevealIndex
        if (idx >= state.revealOrder.size) return null
        return state.players[state.revealOrder[idx]]
    }

    // ── Discussion Phase ───────────────────────────────────

    private fun startTimer() {
        countDownTimer?.cancel()
        val state = _gameState.value
        _gameState.update { it.copy(isTimerRunning = true) }

        countDownTimer = object : CountDownTimer(
            state.timerRemainingSeconds * 1000L, 1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                _gameState.update {
                    it.copy(timerRemainingSeconds = (millisUntilFinished / 1000).toInt())
                }
            }

            override fun onFinish() {
                _gameState.update {
                    it.copy(
                        timerRemainingSeconds = 0,
                        isTimerRunning = false,
                    )
                }
            }
        }.start()
    }

    fun endDiscussion() {
        countDownTimer?.cancel()
        val newState = _gameState.value.copy(
            phase = GamePhase.VOTING,
            isTimerRunning = false,
            currentVoterIndex = 0,
        )
        _gameState.value = newState
        persistCurrentState(newState)
    }

    // ── Voting Phase ───────────────────────────────────────

    fun getCurrentVoter(): Player? {
        val state = _gameState.value
        if (state.currentVoterIndex >= state.players.size) return null
        return state.players[state.currentVoterIndex]
    }

    fun castVote(votedForPlayerId: Int) {
        val state = _gameState.value
        val voterIndex = state.currentVoterIndex
        if (voterIndex >= state.players.size) return

        val voter = state.players[voterIndex]
        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[voterIndex] = voter.copy(hasVoted = true, votedForId = votedForPlayerId)

        val updatedVotes = state.votes.toMutableMap()
        updatedVotes[voter.id] = votedForPlayerId

        val nextVoterIndex = voterIndex + 1
        val allVoted = nextVoterIndex >= state.players.size

        val newState = state.copy(
            players = updatedPlayers,
            votes = updatedVotes,
            currentVoterIndex = nextVoterIndex,
        )
        _gameState.value = newState
        persistCurrentState(newState)

        if (allVoted) {
            tallyVotes()
        }
    }

    private fun tallyVotes() {
        val state = _gameState.value
        val voteCounts = mutableMapOf<Int, Int>()
        for ((_, votedFor) in state.votes) {
            voteCounts[votedFor] = (voteCounts[votedFor] ?: 0) + 1
        }

        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        val topVoted = voteCounts.filter { it.value == maxVotes }.keys.toList()
        val accusedId = topVoted.random()

        val accusedPlayer = state.players.find { it.id == accusedId }
        val isImposterFound = accusedPlayer?.role == Role.IMPOSTER

        val newState = state.copy(
            phase = GamePhase.RESULT,
            accusedPlayerId = accusedId,
            isImposterFound = isImposterFound,
        )
        _gameState.value = newState
        persistCurrentState(newState)

        // Save scores and game history locally
        saveRoundScores(isImposterFound)
    }

    private fun saveRoundScores(isImposterFound: Boolean) {
        viewModelScope.launch {
            val state = _gameState.value
            val roundScoresMap = mutableMapOf<String, Int>()

            val scoreRecords = state.players.map { player ->
                val isImposter = player.role == Role.IMPOSTER
                val points = if (isImposterFound) {
                    if (isImposter) 0 else 1
                } else {
                    if (isImposter) 1 else 0
                }
                roundScoresMap[player.name] = points

                // Update session scores
                _sessionScores.update { scores ->
                    val current = scores.getOrDefault(player.name, 0)
                    scores + (player.name to (current + points))
                }

                ScoreRecord(
                    playerName = player.name,
                    points = points,
                    roundNumber = state.roundNumber,
                    wasImposter = isImposter,
                    won = if (isImposter) !isImposterFound else isImposterFound,
                    secretWord = state.secretWord,
                    categoryName = state.categoryName,
                )
            }

            val historyRecord = GameHistoryRecord(
                id = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                roundNumber = state.roundNumber,
                categoryName = state.categoryName,
                secretWord = state.secretWord,
                secretClue = state.secretClue,
                imposterNames = state.players.filter { it.role == Role.IMPOSTER }.map { it.name },
                civilianNames = state.players.filter { it.role == Role.CIVILIAN }.map { it.name },
                accusedPlayerName = state.players.find { it.id == state.accusedPlayerId }?.name,
                isImposterFound = isImposterFound,
                scores = roundScoresMap,
            )

            // Save match session
            val existingMatches = dataManager.allMatches.first()
            val currentMatch = existingMatches.find { it.id == currentMatchId }

            val updatedMatch = MatchSession(
                id = currentMatchId,
                createdAt = currentMatch?.createdAt ?: System.currentTimeMillis(),
                lastPlayedAt = System.currentTimeMillis(),
                totalRounds = state.roundNumber,
                playerNames = state.players.map { it.name },
                cumulativeScores = _sessionScores.value,
                rounds = (currentMatch?.rounds ?: emptyList()) + historyRecord,
                settings = state.settings,
            )

            dataManager.saveScoresAndHistory(scoreRecords, historyRecord)
            dataManager.saveMatchSession(updatedMatch)
            dataManager.saveLastPlayerNames(state.players.map { it.name })
        }
    }

    // ── Next Round / Reset ─────────────────────────────────

    fun nextRound() {
        val state = _gameState.value
        val newState = state.copy(
            phase = GamePhase.SETUP,
            roundNumber = state.roundNumber + 1,
            secretWord = "",
            secretClue = null,
            accusedPlayerId = null,
            isImposterFound = false,
            votes = emptyMap(),
            currentRevealIndex = 0,
            currentVoterIndex = 0,
            startingSpeakerIndex = 0,
            isTimerRunning = false,
            players = state.players.map {
                p -> p.copy(role = Role.CIVILIAN, hasRevealed = false, hasVoted = false, votedForId = null)
            },
        )
        _gameState.value = newState
        persistCurrentState(newState)
    }

    fun resetGame() {
        countDownTimer?.cancel()
        val newState = _gameState.value.copy(
            phase = GamePhase.SETUP,
            secretWord = "",
            secretClue = null,
            accusedPlayerId = null,
            isImposterFound = false,
            votes = emptyMap(),
            currentRevealIndex = 0,
            currentVoterIndex = 0,
            startingSpeakerIndex = 0,
            isTimerRunning = false,
            players = _gameState.value.players.map { p ->
                p.copy(role = Role.CIVILIAN, hasRevealed = false, hasVoted = false, votedForId = null)
            }
        )
        _gameState.value = newState
        persistCurrentState(newState)
    }

    fun deletePlayerScores(playerName: String) {
        viewModelScope.launch {
            dataManager.deletePlayerScores(playerName)
        }
    }

    fun deleteMatchSession(matchId: Long) {
        viewModelScope.launch {
            dataManager.deleteMatchSession(matchId)
            if (currentMatchId == matchId) {
                _sessionScores.value = emptyMap()
                currentMatchId = System.currentTimeMillis()
            }
        }
    }

    fun clearScoreHistory() {
        viewModelScope.launch {
            dataManager.clearAllScores()
            _sessionScores.value = emptyMap()
        }
    }

    // ── Word Pack Management ───────────────────────────────

    fun getEntriesForPack(packId: Long) = dataManager.getEntriesForPack(packId)

    fun saveWordPack(name: String, entries: List<Pair<String, String?>>) {
        viewModelScope.launch {
            dataManager.saveWordPack(name, entries)
        }
    }

    fun updateWordPack(packId: Long, name: String, entries: List<Pair<String, String?>>) {
        viewModelScope.launch {
            dataManager.updateWordPack(packId, name, entries)
        }
    }

    fun deleteWordPack(pack: WordPack) {
        viewModelScope.launch {
            dataManager.deleteWordPack(pack)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}
