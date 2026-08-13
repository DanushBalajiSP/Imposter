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

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    val wordPacks = dataManager.allPacks
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val leaderboard = dataManager.leaderboard
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allScores = dataManager.allScores
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var countDownTimer: CountDownTimer? = null

    // Accumulated scores across rounds in the current session
    private val _sessionScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val sessionScores: StateFlow<Map<String, Int>> = _sessionScores.asStateFlow()

    init {
        viewModelScope.launch {
            dataManager.initialize()
        }
    }

    // ── Setup ──────────────────────────────────────────────

    fun updateSettings(settings: GameSettings) {
        _gameState.update { it.copy(settings = settings) }
    }

    fun setPlayerNames(names: List<String>) {
        val players = names.mapIndexed { index, name ->
            Player(id = index, name = name)
        }
        _gameState.update { it.copy(players = players) }
    }

    // ── Start Game ─────────────────────────────────────────

    fun startGame() {
        viewModelScope.launch {
            val state = _gameState.value
            val settings = state.settings
            val players = state.players

            if (players.size < 3) return@launch

            // Determine imposter count
            val imposterCount = when (settings.imposterMode) {
                ImposterMode.MANUAL -> settings.manualImposterCount.coerceIn(1, players.size - 1)
                ImposterMode.AUTO_RANGE -> {
                    val maxAllowed = minOf(settings.autoRange.max, players.size - 1)
                    val minAllowed = settings.autoRange.min.coerceAtMost(maxAllowed)
                    (minAllowed..maxAllowed).random()
                }
            }

            // Pick a random word from the selected category
            val entries = if (settings.selectedCategoryId > 0) {
                dataManager.getEntriesForPackOnce(settings.selectedCategoryId)
            } else {
                val packs = wordPacks.value
                if (packs.isNotEmpty()) {
                    dataManager.getEntriesForPackOnce(packs.random().id)
                } else {
                    emptyList()
                }
            }

            if (entries.isEmpty()) return@launch

            val selectedEntry = entries.random()
            val categoryName = if (settings.selectedCategoryId > 0) {
                dataManager.getPackById(settings.selectedCategoryId)?.name ?: ""
            } else ""

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

            // Shuffle reveal order
            val revealOrder = assignedPlayers.indices.shuffled()

            // Discussion order: random start, then round-robin
            val startIndex = players.indices.random()
            val discussionOrder = (startIndex until startIndex + players.size).map { it % players.size }

            // Timer
            val timerSeconds = when (settings.timerDuration) {
                TimerDuration.CUSTOM -> settings.customTimerSeconds
                else -> settings.timerDuration.seconds
            }

            _gameState.update {
                it.copy(
                    phase = GamePhase.REVEALING,
                    players = assignedPlayers,
                    secretWord = selectedEntry.word,
                    secretClue = selectedEntry.clue,
                    categoryName = categoryName,
                    currentRevealIndex = 0,
                    revealOrder = revealOrder,
                    discussionOrder = discussionOrder,
                    currentSpeakerIndex = 0,
                    timerRemainingSeconds = timerSeconds,
                    isTimerRunning = false,
                    currentVoterIndex = 0,
                    votes = emptyMap(),
                    actualImposterCount = imposterCount,
                    accusedPlayerId = null,
                    isImposterFound = false,
                )
            }
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

        _gameState.update {
            it.copy(
                players = updatedPlayers,
                currentRevealIndex = nextIndex,
                phase = if (allRevealed) GamePhase.DISCUSSION else GamePhase.REVEALING,
            )
        }

        if (allRevealed) {
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

    fun advanceSpeaker() {
        val state = _gameState.value
        val nextSpeaker = state.currentSpeakerIndex + 1
        if (nextSpeaker < state.discussionOrder.size) {
            _gameState.update { it.copy(currentSpeakerIndex = nextSpeaker) }
        }
    }

    fun endDiscussion() {
        countDownTimer?.cancel()
        _gameState.update {
            it.copy(
                phase = GamePhase.VOTING,
                isTimerRunning = false,
                currentVoterIndex = 0,
            )
        }
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

        _gameState.update {
            it.copy(
                players = updatedPlayers,
                votes = updatedVotes,
                currentVoterIndex = nextVoterIndex,
            )
        }

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

        _gameState.update {
            it.copy(
                phase = GamePhase.RESULT,
                accusedPlayerId = accusedId,
                isImposterFound = isImposterFound,
            )
        }

        // Save scores
        saveRoundScores(isImposterFound)
    }

    private fun saveRoundScores(isImposterFound: Boolean) {
        viewModelScope.launch {
            val state = _gameState.value
            val scoreRecords = state.players.map { player ->
                val isImposter = player.role == Role.IMPOSTER
                val points = if (isImposterFound) {
                    if (isImposter) 0 else 1
                } else {
                    if (isImposter) 1 else 0
                }

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
                )
            }
            dataManager.saveScores(scoreRecords)
        }
    }

    // ── Next Round / Reset ─────────────────────────────────

    fun nextRound() {
        val state = _gameState.value
        _gameState.update {
            it.copy(
                phase = GamePhase.SETUP,
                roundNumber = state.roundNumber + 1,
                secretWord = "",
                secretClue = null,
                accusedPlayerId = null,
                isImposterFound = false,
                votes = emptyMap(),
                currentRevealIndex = 0,
                currentVoterIndex = 0,
                currentSpeakerIndex = 0,
                isTimerRunning = false,
                players = state.players.map {
                    p -> p.copy(role = Role.CIVILIAN, hasRevealed = false, hasVoted = false, votedForId = null)
                },
            )
        }
    }

    fun resetGame() {
        countDownTimer?.cancel()
        _gameState.value = GameState()
        _sessionScores.value = emptyMap()
    }

    fun clearScoreHistory() {
        viewModelScope.launch {
            dataManager.clearAllScores()
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
