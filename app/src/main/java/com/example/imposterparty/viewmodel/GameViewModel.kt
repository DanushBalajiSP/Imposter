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

            // Retrieve match session and completed round history for fairness calculation
            val existingMatches = dataManager.allMatches.first()
            val currentMatch = existingMatches.find { it.id == currentMatchId }
            val completedRounds = currentMatch?.rounds ?: emptyList()

            // Build global word usage history for anti-repetition cooldown
            val allHistoryList = dataManager.allHistory.first()
            val wordUsageHistory = mutableMapOf<String, Int>()
            allHistoryList.forEach { rec ->
                if (rec.secretWord.isNotBlank()) {
                    val norm = com.example.imposterparty.data.randomizer.FairRandomizer.normalizeWord(rec.secretWord)
                    val prev = wordUsageHistory[norm] ?: -1
                    if (rec.roundNumber > prev) {
                        wordUsageHistory[norm] = rec.roundNumber
                    }
                }
            }

            // Category & Word Selection using Fair Randomizer (cooldown + shuffle-bag + fallback)
            val availablePackIds: List<Long> = if (settings.selectedCategoryIds.isNotEmpty()) {
                settings.selectedCategoryIds.toList()
            } else {
                wordPacks.value.map { it.id }
            }

            val categoryLastUsed = mutableMapOf<Long, Int>()
            completedRounds.forEach { round ->
                val matchedPack = wordPacks.value.find { it.name.equals(round.categoryName, ignoreCase = true) }
                if (matchedPack != null) {
                    categoryLastUsed[matchedPack.id] = round.roundNumber
                }
            }

            val chosenPackId = com.example.imposterparty.data.randomizer.FairRandomizer.selectCategory(
                availablePackIds = availablePackIds,
                categoryLastUsedRound = categoryLastUsed,
                currentRound = state.roundNumber,
            )

            val candidateEntries = dataManager.getEntriesForPackOnce(chosenPackId).ifEmpty {
                availablePackIds.flatMap { dataManager.getEntriesForPackOnce(it) }
            }.ifEmpty {
                wordPacks.value.flatMap { dataManager.getEntriesForPackOnce(it.id) }
            }

            if (candidateEntries.isEmpty()) return@launch

            val selectedEntry = com.example.imposterparty.data.randomizer.FairRandomizer.selectSecretWord(
                candidateEntries = candidateEntries,
                wordUsageHistory = wordUsageHistory,
                currentRound = state.roundNumber,
            )
            val categoryName = dataManager.getPackById(selectedEntry.packId)?.name ?: ""

            // Fair Imposter Selection (hard 3-streak exclusion, soft 2-streak penalty, recency + match lifetime balance)
            val playerHistories = com.example.imposterparty.data.randomizer.FairRandomizer.buildPlayerHistories(
                players = players,
                completedRounds = completedRounds,
                currentRoundNumber = state.roundNumber,
            )

            val imposterIndices = com.example.imposterparty.data.randomizer.FairRandomizer.selectImposters(
                players = players,
                imposterCount = imposterCount,
                playerHistories = playerHistories,
            )

            val assignedPlayers = players.mapIndexed { index, player ->
                player.copy(
                    role = if (player.id in imposterIndices) Role.IMPOSTER else Role.CIVILIAN,
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

    fun getActivePlayers(): List<Player> {
        val state = _gameState.value
        return if (state.isSubRound) {
            state.players.filter { it.id !in state.eliminatedPlayerIds }
        } else {
            state.players
        }
    }

    fun getCurrentVoter(): Player? {
        val state = _gameState.value
        val activePlayers = getActivePlayers()
        if (state.currentVoterIndex >= activePlayers.size) return null
        return activePlayers[state.currentVoterIndex]
    }

    fun castVote(votedForPlayerId: Int) {
        val state = _gameState.value
        val activePlayers = getActivePlayers()
        val voterIndex = state.currentVoterIndex
        if (voterIndex >= activePlayers.size) return

        val voter = activePlayers[voterIndex]
        val updatedPlayers = state.players.map { player ->
            if (player.id == voter.id) {
                player.copy(hasVoted = true, votedForId = votedForPlayerId)
            } else {
                player
            }
        }

        val updatedVotes = state.votes.toMutableMap()
        updatedVotes[voter.id] = votedForPlayerId

        val nextVoterIndex = voterIndex + 1
        val allVoted = nextVoterIndex >= activePlayers.size

        val newState = state.copy(
            players = updatedPlayers,
            votes = updatedVotes,
            currentVoterIndex = nextVoterIndex,
        )
        _gameState.value = newState
        persistCurrentState(newState)

        if (allVoted) {
            if (state.isSubRound) {
                tallySubRoundVotes()
            } else {
                tallyVotes()
            }
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
        val isAccusedImposter = accusedPlayer?.role == Role.IMPOSTER
        val allImposters = state.players.filter { it.role == Role.IMPOSTER }

        val roundScoresMap = mutableMapOf<String, Int>()

        // Two-Imposter / Multi-Imposter Rule Resolution:
        if (allImposters.size >= 2) {
            if (!isAccusedImposter) {
                // All imposters survived -> Imposters Win!
                allImposters.forEach { imposter ->
                    roundScoresMap[imposter.name] = 1
                }
                state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                    val votedForId = state.votes[civilian.id]
                    val votedForPlayer = state.players.find { it.id == votedForId }
                    val votedForAnImposter = votedForPlayer?.role == Role.IMPOSTER
                    roundScoresMap[civilian.name] = if (votedForAnImposter) 1 else 0
                }
                val newState = state.copy(
                    phase = GamePhase.RESULT,
                    accusedPlayerId = accusedId,
                    isImposterFound = false,
                    eliminatedPlayerIds = listOf(accusedId),
                    finalPhaseDescription = "Both Imposters Survived!",
                    pendingRoundScores = roundScoresMap,
                )
                _gameState.value = newState
                persistCurrentState(newState)
                saveRoundScores(isCivilianWin = false, roundScoresMap = roundScoresMap)
            } else {
                // Exactly one imposter eliminated:
                val survivingImposters = allImposters.filter { it.id != accusedId }
                if (survivingImposters.size == 1) {
                    val remainingImposter = survivingImposters.first()
                    // Pending bonus points for voting the eliminated imposter:
                    state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                        val votedForId = state.votes[civilian.id]
                        val votedForEliminatedImposter = (votedForId == accusedId)
                        roundScoresMap[civilian.name] = if (votedForEliminatedImposter) 1 else 0
                    }
                    roundScoresMap[accusedPlayer.name] = 0
                    roundScoresMap[remainingImposter.name] = 0

                    // Enter Final Imposter Phase!
                    val newState = state.copy(
                        phase = GamePhase.FINAL_IMPOSTER_CHOICE,
                        accusedPlayerId = accusedId,
                        isImposterFound = true,
                        eliminatedPlayerIds = listOf(accusedId),
                        remainingImposterId = remainingImposter.id,
                        finalPhaseDescription = "${accusedPlayer?.name ?: "Imposter"} was eliminated! 1 Imposter remains.",
                        pendingRoundScores = roundScoresMap,
                    )
                    _gameState.value = newState
                    persistCurrentState(newState)
                } else {
                    // All imposters eliminated -> Civilians Win!
                    state.players.forEach { player ->
                        if (player.role == Role.CIVILIAN) {
                            val votedForId = state.votes[player.id]
                            val votedForEliminatedImposter = isAccusedImposter && (votedForId == accusedId)
                            // Base 1 point for Civilians winning + 1 bonus if voted for eliminated imposter
                            roundScoresMap[player.name] = if (votedForEliminatedImposter) 2 else 1
                        } else {
                            roundScoresMap[player.name] = 0
                        }
                    }
                    val newState = state.copy(
                        phase = GamePhase.RESULT,
                        accusedPlayerId = accusedId,
                        isImposterFound = true,
                        eliminatedPlayerIds = listOf(accusedId),
                        finalPhaseDescription = "All Imposters Were Eliminated!",
                        pendingRoundScores = roundScoresMap,
                    )
                    _gameState.value = newState
                    persistCurrentState(newState)
                    saveRoundScores(isCivilianWin = true, roundScoresMap = roundScoresMap)
                }
            }
        } else {
            // Single Imposter Game Resolution:
            val singleImposter = allImposters.firstOrNull()
            if (isAccusedImposter) {
                // Civilians Win!
                state.players.forEach { player ->
                    if (player.role == Role.CIVILIAN) {
                        val votedForId = state.votes[player.id]
                        val votedForEliminatedImposter = (votedForId == accusedId)
                        // Base 1 point for Civilians winning + 1 bonus if voted for eliminated imposter (1+1=2)
                        // Wrong voters get base 1 point, right voters get 2 points
                        roundScoresMap[player.name] = if (votedForEliminatedImposter) 2 else 1
                    } else {
                        roundScoresMap[player.name] = 0
                    }
                }
                val newState = state.copy(
                    phase = GamePhase.RESULT,
                    accusedPlayerId = accusedId,
                    isImposterFound = true,
                    eliminatedPlayerIds = listOf(accusedId),
                    finalPhaseDescription = "Imposter Was Caught!",
                    pendingRoundScores = roundScoresMap,
                )
                _gameState.value = newState
                persistCurrentState(newState)
                saveRoundScores(isCivilianWin = true, roundScoresMap = roundScoresMap)
            } else {
                // Imposter Wins!
                if (singleImposter != null) {
                    roundScoresMap[singleImposter.name] = 1
                }
                state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                    val votedForId = state.votes[civilian.id]
                    val votedForPlayer = state.players.find { it.id == votedForId }
                    val votedForAnImposter = votedForPlayer?.role == Role.IMPOSTER
                    roundScoresMap[civilian.name] = if (votedForAnImposter) 1 else 0
                }
                val newState = state.copy(
                    phase = GamePhase.RESULT,
                    accusedPlayerId = accusedId,
                    isImposterFound = false,
                    eliminatedPlayerIds = listOf(accusedId),
                    finalPhaseDescription = "Imposter Escaped Unnoticed!",
                    pendingRoundScores = roundScoresMap,
                )
                _gameState.value = newState
                persistCurrentState(newState)
                saveRoundScores(isCivilianWin = false, roundScoresMap = roundScoresMap)
            }
        }
    }

    // ── Final Imposter Phase Handlers ──────────────────────

    fun submitImposterWordGuess(guess: String) {
        val state = _gameState.value
        val normalizedGuess = com.example.imposterparty.data.randomizer.FairRandomizer.normalizeWord(guess)
        val normalizedSecret = com.example.imposterparty.data.randomizer.FairRandomizer.normalizeWord(state.secretWord)
        val isCorrect = normalizedGuess == normalizedSecret

        val finalScores = mutableMapOf<String, Int>()
        val survivingImposter = state.players.find { it.id == state.remainingImposterId }

        if (isCorrect) {
            // Imposter Wins via Guess: Receives +3 total points (+2 hidden survival + +1 victory value)
            if (survivingImposter != null) {
                finalScores[survivingImposter.name] = 3
            }
            state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                val priorBonus = state.pendingRoundScores.getOrDefault(civilian.name, 0)
                finalScores[civilian.name] = priorBonus
            }
            val newState = state.copy(
                phase = GamePhase.RESULT,
                isImposterFound = false,
                imposterGuessWord = guess.trim(),
                wasWordGuessedCorrectly = true,
                finalPhaseDescription = "${survivingImposter?.name ?: "Imposter"} correctly guessed the secret word!",
                pendingRoundScores = finalScores,
            )
            _gameState.value = newState
            persistCurrentState(newState)
            saveRoundScores(isCivilianWin = false, roundScoresMap = finalScores)
        } else {
            // Civilians Win (Imposter Guessed Wrong)
            if (survivingImposter != null) {
                finalScores[survivingImposter.name] = 0
            }
            state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                val priorBonus = state.pendingRoundScores.getOrDefault(civilian.name, 0)
                // Base 1 point for Civilians winning + prior correct vote bonus
                finalScores[civilian.name] = 1 + priorBonus
            }
            val newState = state.copy(
                phase = GamePhase.RESULT,
                isImposterFound = true,
                imposterGuessWord = guess.trim(),
                wasWordGuessedCorrectly = false,
                finalPhaseDescription = "${survivingImposter?.name ?: "Imposter"} guessed \"${guess.trim()}\" (Word was \"${state.secretWord}\")",
                pendingRoundScores = finalScores,
            )
            _gameState.value = newState
            persistCurrentState(newState)
            saveRoundScores(isCivilianWin = true, roundScoresMap = finalScores)
        }
    }

    fun startSubRound() {
        countDownTimer?.cancel()
        val state = _gameState.value
        val timerSeconds = if (state.settings.isTimerEnabled) {
            when (state.settings.timerDuration) {
                TimerDuration.CUSTOM -> state.settings.customTimerSeconds
                else -> state.settings.timerDuration.seconds
            }
        } else {
            0
        }

        val resetPlayers = state.players.map { player ->
            player.copy(hasVoted = false, votedForId = null)
        }

        val newState = state.copy(
            phase = GamePhase.SUB_ROUND_DISCUSSION,
            isSubRound = true,
            players = resetPlayers,
            votes = emptyMap(),
            currentVoterIndex = 0,
            timerRemainingSeconds = timerSeconds,
            isTimerRunning = false,
        )
        _gameState.value = newState
        persistCurrentState(newState)

        if (state.settings.isTimerEnabled) {
            startTimer()
        }
    }

    fun endSubRoundDiscussion() {
        countDownTimer?.cancel()
        val resetPlayers = _gameState.value.players.map { player ->
            player.copy(hasVoted = false, votedForId = null)
        }
        val newState = _gameState.value.copy(
            phase = GamePhase.SUB_ROUND_VOTING,
            players = resetPlayers,
            isTimerRunning = false,
            currentVoterIndex = 0,
            votes = emptyMap(),
        )
        _gameState.value = newState
        persistCurrentState(newState)
    }

    private fun tallySubRoundVotes() {
        val state = _gameState.value
        val voteCounts = mutableMapOf<Int, Int>()
        for ((_, votedFor) in state.votes) {
            voteCounts[votedFor] = (voteCounts[votedFor] ?: 0) + 1
        }

        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        val topVoted = voteCounts.filter { it.value == maxVotes }.keys.toList()
        val subAccusedId = topVoted.random()

        val remainingImposterId = state.remainingImposterId
        val isRemainingImposterEliminated = subAccusedId == remainingImposterId
        val survivingImposter = state.players.find { it.id == remainingImposterId }
        val subAccusedPlayer = state.players.find { it.id == subAccusedId }

        val finalScores = mutableMapOf<String, Int>()

        if (isRemainingImposterEliminated) {
            // Civilians Win Sub-Round!
            if (survivingImposter != null) {
                finalScores[survivingImposter.name] = 0
            }
            state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                val priorBonus = state.pendingRoundScores.getOrDefault(civilian.name, 0)
                val subVoteId = state.votes[civilian.id]
                val votedForSurvivingImposter = (subVoteId == remainingImposterId)
                val subBonus = if (votedForSurvivingImposter) 1 else 0

                // Base 1 point for Civilians winning + bonus from main round or sub-round
                finalScores[civilian.name] = 1 + (if (priorBonus > 0 || subBonus > 0) 1 else 0)
            }
            val newState = state.copy(
                phase = GamePhase.RESULT,
                accusedPlayerId = subAccusedId,
                isImposterFound = true,
                eliminatedPlayerIds = state.eliminatedPlayerIds + subAccusedId,
                finalPhaseDescription = "${survivingImposter?.name ?: "Remaining Imposter"} was eliminated in the Sub-Round!",
                pendingRoundScores = finalScores,
            )
            _gameState.value = newState
            persistCurrentState(newState)
            saveRoundScores(isCivilianWin = true, roundScoresMap = finalScores)
        } else {
            // Imposter Wins Sub-Round! Receives +3 total points (+2 hidden survival + +1 victory value)
            if (survivingImposter != null) {
                finalScores[survivingImposter.name] = 3
            }
            state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                val priorBonus = state.pendingRoundScores.getOrDefault(civilian.name, 0)
                val subVoteId = state.votes[civilian.id]
                val votedForSurvivingImposter = (subVoteId == remainingImposterId)
                val subBonus = if (votedForSurvivingImposter) 1 else 0
                finalScores[civilian.name] = (if (priorBonus > 0 || subBonus > 0) 1 else 0)
            }
            val newState = state.copy(
                phase = GamePhase.RESULT,
                accusedPlayerId = subAccusedId,
                isImposterFound = false,
                eliminatedPlayerIds = state.eliminatedPlayerIds + subAccusedId,
                finalPhaseDescription = "${survivingImposter?.name ?: "Remaining Imposter"} survived the Sub-Round!",
                pendingRoundScores = finalScores,
            )
            _gameState.value = newState
            persistCurrentState(newState)
            saveRoundScores(isCivilianWin = false, roundScoresMap = finalScores)
        }
    }

    private fun saveRoundScores(isCivilianWin: Boolean, roundScoresMap: Map<String, Int>) {
        viewModelScope.launch {
            val state = _gameState.value

            val scoreRecords = state.players.map { player ->
                val isImposter = player.role == Role.IMPOSTER
                val points = roundScoresMap.getOrDefault(player.name, 0)

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
                    won = if (isImposter) !isCivilianWin else isCivilianWin,
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
                isImposterFound = isCivilianWin,
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
            eliminatedPlayerIds = emptyList(),
            remainingImposterId = null,
            imposterGuessWord = null,
            wasWordGuessedCorrectly = null,
            finalPhaseDescription = null,
            pendingRoundScores = emptyMap(),
            isSubRound = false,
            subRoundVoterIndices = emptyList(),
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
            eliminatedPlayerIds = emptyList(),
            remainingImposterId = null,
            imposterGuessWord = null,
            wasWordGuessedCorrectly = null,
            finalPhaseDescription = null,
            pendingRoundScores = emptyMap(),
            isSubRound = false,
            subRoundVoterIndices = emptyList(),
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
