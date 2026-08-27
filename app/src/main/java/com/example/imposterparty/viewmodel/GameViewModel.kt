package com.example.imposterparty.viewmodel

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imposterparty.data.local.LocalDataManager
import com.example.imposterparty.data.model.*
import com.example.imposterparty.data.remote.CloudWordPackRepository
import com.example.imposterparty.data.remote.UserManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val dataManager = LocalDataManager(application)
    val userManager = UserManager(application)
    val cloudWordPackRepo = CloudWordPackRepository()

    val currentUser: StateFlow<UserProfile?> = userManager.currentUser

    val communityPacks: StateFlow<List<CommunityWordPack>> = cloudWordPackRepo.getCommunityPacks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

            // Retrieve universal persistent RandomizerState for cross-match dynamic fairness
            val randomizerState = dataManager.getRandomizerState()

            // Category & Word Selection using Fair Randomizer (cooldown + shuffle-bag + fallback)
            val availablePackIds: List<Long> = if (settings.selectedCategoryIds.isNotEmpty()) {
                settings.selectedCategoryIds.toList()
            } else {
                wordPacks.value.map { it.id }
            }

            val chosenPackId = availablePackIds.randomOrNull() ?: wordPacks.value.firstOrNull()?.id ?: 1L

            val candidateEntries = dataManager.getEntriesForPackOnce(chosenPackId).ifEmpty {
                availablePackIds.flatMap { dataManager.getEntriesForPackOnce(it) }
            }.ifEmpty {
                wordPacks.value.flatMap { dataManager.getEntriesForPackOnce(it.id) }
            }

            if (candidateEntries.isEmpty()) return@launch

            val selectedEntry = com.example.imposterparty.data.randomizer.FairRandomizer.selectSecretWord(
                candidateEntries = candidateEntries,
                wordUsageHistory = randomizerState.wordUsageHistory,
                currentRound = randomizerState.globalRoundCounter + 1,
            )
            val categoryName = dataManager.getPackById(selectedEntry.packId)?.name ?: ""

            // Universal Fair Imposter Selection across ALL matches
            val playerHistories = com.example.imposterparty.data.randomizer.FairRandomizer.buildPlayerHistoriesFromState(
                players = players,
                playerImposterCounts = randomizerState.playerImposterCounts,
                recentImposterRounds = randomizerState.recentImposterNames,
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

        val playerId = revealOrder[currentIdx]
        val updatedPlayers = state.players.map { player ->
            if (player.id == playerId) player.copy(hasRevealed = true) else player
        }

        val nextIndex = currentIdx + 1
        val allRevealed = nextIndex >= revealOrder.size

        val nextPhase = if (allRevealed) {
            if (state.isSubRound) GamePhase.SUB_ROUND_DISCUSSION else GamePhase.DISCUSSION
        } else {
            GamePhase.REVEALING
        }

        val newState = state.copy(
            players = updatedPlayers,
            currentRevealIndex = nextIndex,
            phase = nextPhase,
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
        val playerId = state.revealOrder[idx]
        return state.players.find { it.id == playerId }
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
        return state.players.filter { it.id !in state.eliminatedPlayerIds }
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
        val updatedEliminated = listOf(accusedId)
        val remainingImposters = allImposters.filter { it.id !in updatedEliminated }

        // Update secured points: every imposter who survived this main round secures +1 point
        val updatedSecured = state.imposterSecuredPoints.toMutableMap()
        remainingImposters.forEach { imposter ->
            updatedSecured[imposter.id] = (updatedSecured[imposter.id] ?: 0) + 1
        }

        val roundScoresMap = mutableMapOf<String, Int>()

        if (!isAccusedImposter) {
            // Civilian was accused -> Imposters Win immediately!
            allImposters.forEach { imposter ->
                val isSurviving = imposter.id !in updatedEliminated
                val secured = updatedSecured[imposter.id] ?: 0
                // Surviving winning imposters get +1 bonus point on top of their secured points
                roundScoresMap[imposter.name] = if (isSurviving) secured + 1 else secured
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
                eliminatedPlayerIds = updatedEliminated,
                imposterSecuredPoints = updatedSecured,
                finalPhaseDescription = "${accusedPlayer?.name ?: "A Civilian"} was eliminated! Imposters Win!",
                pendingRoundScores = roundScoresMap,
            )
            _gameState.value = newState
            persistCurrentState(newState)
            saveRoundScores(isCivilianWin = false, roundScoresMap = roundScoresMap)
        } else {
            // An imposter was accused and eliminated!
            if (remainingImposters.isEmpty()) {
                // All imposters eliminated -> Civilians Win!
                state.players.forEach { player ->
                    if (player.role == Role.CIVILIAN) {
                        val votedForId = state.votes[player.id]
                        val votedForEliminatedImposter = (votedForId == accusedId)
                        // Base 1 point for Civilians winning + 1 bonus if voted for eliminated imposter
                        roundScoresMap[player.name] = if (votedForEliminatedImposter) 2 else 1
                    } else {
                        roundScoresMap[player.name] = updatedSecured[player.id] ?: 0
                    }
                }
                val newState = state.copy(
                    phase = GamePhase.RESULT,
                    accusedPlayerId = accusedId,
                    isImposterFound = true,
                    eliminatedPlayerIds = updatedEliminated,
                    imposterSecuredPoints = updatedSecured,
                    finalPhaseDescription = "All Imposters Were Eliminated! Civilians Win!",
                    pendingRoundScores = roundScoresMap,
                )
                _gameState.value = newState
                persistCurrentState(newState)
                saveRoundScores(isCivilianWin = true, roundScoresMap = roundScoresMap)
            } else {
                // Other imposter(s) remain -> Enter Imposter Choice Phase (Guess Word or Sub-Round)!
                state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                    val votedForId = state.votes[civilian.id]
                    val votedForEliminatedImposter = (votedForId == accusedId)
                    roundScoresMap[civilian.name] = if (votedForEliminatedImposter) 1 else 0
                }
                allImposters.forEach { imposter ->
                    roundScoresMap[imposter.name] = updatedSecured[imposter.id] ?: 0
                }

                val newState = state.copy(
                    phase = GamePhase.FINAL_IMPOSTER_CHOICE,
                    accusedPlayerId = accusedId,
                    isImposterFound = true,
                    eliminatedPlayerIds = updatedEliminated,
                    imposterSecuredPoints = updatedSecured,
                    remainingImposterId = remainingImposters.first().id,
                    finalPhaseDescription = "${accusedPlayer?.name ?: "An Imposter"} was eliminated! ${remainingImposters.size} Imposter(s) remain.",
                    pendingRoundScores = roundScoresMap,
                )
                _gameState.value = newState
                persistCurrentState(newState)
            }
        }
    }

    // ── Imposter Guess / Sub-Round Phase Handlers ──────────

    fun submitImposterWordGuess(guess: String, volunteerPlayerId: Int? = null): Boolean {
        val state = _gameState.value
        val normalizedGuess = com.example.imposterparty.data.randomizer.FairRandomizer.normalizeWord(guess)
        val normalizedSecret = com.example.imposterparty.data.randomizer.FairRandomizer.normalizeWord(state.secretWord)
        val isCorrect = normalizedGuess == normalizedSecret

        val activeImposters = state.players.filter { it.role == Role.IMPOSTER && it.id !in state.eliminatedPlayerIds }
        val volunteer = (if (volunteerPlayerId != null) state.players.find { it.id == volunteerPlayerId } else null)
            ?: state.players.find { it.id == state.remainingImposterId }
            ?: activeImposters.firstOrNull()

        // Volunteer imposter who guesses is eliminated at that time
        val updatedEliminated = if (volunteer != null) {
            (state.eliminatedPlayerIds + volunteer.id).distinct()
        } else {
            state.eliminatedPlayerIds
        }

        val allImposters = state.players.filter { it.role == Role.IMPOSTER }
        val remainingImposters = allImposters.filter { it.id !in updatedEliminated }

        val updatedSecured = state.imposterSecuredPoints.toMutableMap()
        val finalScores = mutableMapOf<String, Int>()

        if (isCorrect) {
            // Volunteer gets +2 points added to their secured points
            if (volunteer != null) {
                updatedSecured[volunteer.id] = (state.imposterSecuredPoints[volunteer.id] ?: 0) + 2
            }

            allImposters.forEach { imposter ->
                val secured = updatedSecured[imposter.id] ?: 0
                finalScores[imposter.name] = secured
            }
            state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                val priorBonus = state.pendingRoundScores.getOrDefault(civilian.name, 0)
                finalScores[civilian.name] = if (priorBonus > 0) 1 else 0
            }

            if (remainingImposters.isEmpty()) {
                // All imposters resolved -> Round Ends
                val newState = state.copy(
                    phase = GamePhase.RESULT,
                    isImposterFound = false,
                    eliminatedPlayerIds = updatedEliminated,
                    imposterSecuredPoints = updatedSecured,
                    volunteerImposterId = volunteer?.id,
                    imposterGuessWord = guess.trim(),
                    wasWordGuessedCorrectly = true,
                    finalPhaseDescription = "${volunteer?.name ?: "Imposter"} correctly guessed \"${state.secretWord}\" (+2 pts)! All imposters resolved.",
                    pendingRoundScores = finalScores,
                )
                _gameState.value = newState
                persistCurrentState(newState)
                saveRoundScores(isCivilianWin = false, roundScoresMap = finalScores)
            } else {
                // More imposters remain -> Stay in FINAL_IMPOSTER_CHOICE for remaining imposters
                val updatedPending = state.pendingRoundScores.toMutableMap()
                allImposters.forEach { imposter ->
                    updatedPending[imposter.name] = updatedSecured[imposter.id] ?: 0
                }

                val newState = state.copy(
                    phase = GamePhase.FINAL_IMPOSTER_CHOICE,
                    isImposterFound = false,
                    eliminatedPlayerIds = updatedEliminated,
                    imposterSecuredPoints = updatedSecured,
                    volunteerImposterId = volunteer?.id,
                    remainingImposterId = remainingImposters.first().id,
                    imposterGuessWord = guess.trim(),
                    wasWordGuessedCorrectly = true,
                    finalPhaseDescription = "${volunteer?.name ?: "An Imposter"} correctly guessed \"${state.secretWord}\" (+2 pts) and safely exited! ${remainingImposters.size} Imposter(s) remain.",
                    pendingRoundScores = updatedPending,
                )
                _gameState.value = newState
                persistCurrentState(newState)
            }
            return true
        } else {
            // Volunteer Guessed Wrong -> Eliminated with 0 bonus (retains secured points)
            allImposters.forEach { imposter ->
                finalScores[imposter.name] = state.imposterSecuredPoints[imposter.id] ?: 0
            }
            state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                val priorBonus = state.pendingRoundScores.getOrDefault(civilian.name, 0)
                // Civilians win base 1 pt + 1 bonus if they voted for an imposter earlier
                finalScores[civilian.name] = 1 + (if (priorBonus > 0) 1 else 0)
            }

            if (remainingImposters.isEmpty()) {
                // All imposters have been eliminated -> Civilians Win!
                val newState = state.copy(
                    phase = GamePhase.RESULT,
                    isImposterFound = true,
                    eliminatedPlayerIds = updatedEliminated,
                    imposterSecuredPoints = updatedSecured,
                    volunteerImposterId = volunteer?.id,
                    imposterGuessWord = guess.trim(),
                    wasWordGuessedCorrectly = false,
                    finalPhaseDescription = "${volunteer?.name ?: "Imposter"} guessed incorrectly (\"${guess.trim()}\")! All imposters eliminated! Civilians Win!",
                    pendingRoundScores = finalScores,
                )
                _gameState.value = newState
                persistCurrentState(newState)
                saveRoundScores(isCivilianWin = true, roundScoresMap = finalScores)
            } else {
                // More imposters remain -> Stay in FINAL_IMPOSTER_CHOICE for remaining imposters
                val updatedPending = state.pendingRoundScores.toMutableMap()
                allImposters.forEach { imposter ->
                    updatedPending[imposter.name] = updatedSecured[imposter.id] ?: 0
                }

                val newState = state.copy(
                    phase = GamePhase.FINAL_IMPOSTER_CHOICE,
                    isImposterFound = true,
                    eliminatedPlayerIds = updatedEliminated,
                    imposterSecuredPoints = updatedSecured,
                    volunteerImposterId = volunteer?.id,
                    remainingImposterId = remainingImposters.first().id,
                    imposterGuessWord = guess.trim(),
                    wasWordGuessedCorrectly = false,
                    finalPhaseDescription = "${volunteer?.name ?: "An Imposter"} guessed incorrectly (\"${guess.trim()}\") and was eliminated! ${remainingImposters.size} Imposter(s) remain.",
                    pendingRoundScores = updatedPending,
                )
                _gameState.value = newState
                persistCurrentState(newState)
            }
            return false
        }
    }

    fun startSubRound() {
        countDownTimer?.cancel()
        viewModelScope.launch {
            val state = _gameState.value
            val settings = state.settings
            val activePlayers = state.players.filter { it.id !in state.eliminatedPlayerIds }

            // Pick a new secret word from chosen packs, avoiding the current secret word
            val availablePackIds: List<Long> = if (settings.selectedCategoryIds.isNotEmpty()) {
                settings.selectedCategoryIds.toList()
            } else {
                wordPacks.value.map { it.id }
            }
            val candidateEntries = availablePackIds.flatMap { dataManager.getEntriesForPackOnce(it) }
                .ifEmpty { wordPacks.value.flatMap { dataManager.getEntriesForPackOnce(it.id) } }
            val randomizerState = dataManager.getRandomizerState()
            val chosenEntry = if (candidateEntries.isNotEmpty()) {
                com.example.imposterparty.data.randomizer.FairRandomizer.selectSecretWord(
                    candidateEntries = candidateEntries,
                    wordUsageHistory = randomizerState.wordUsageHistory,
                    currentRound = randomizerState.globalRoundCounter + 1,
                )
            } else null

            val newSecretWord = chosenEntry?.word ?: state.secretWord
            val newSecretClue = chosenEntry?.clue ?: state.secretClue
            val newCategoryName = if (chosenEntry != null) {
                dataManager.getPackById(chosenEntry.packId)?.name ?: state.categoryName
            } else {
                state.categoryName
            }

            val timerSeconds = if (state.settings.isTimerEnabled) {
                when (state.settings.timerDuration) {
                    TimerDuration.CUSTOM -> state.settings.customTimerSeconds
                    else -> state.settings.timerDuration.seconds
                }
            } else {
                0
            }

            val resetPlayers = state.players.map { player ->
                if (player.id in state.eliminatedPlayerIds) {
                    player
                } else {
                    player.copy(hasRevealed = false, hasVoted = false, votedForId = null)
                }
            }

            val revealOrder = activePlayers.map { it.id }
            val startingSpeaker = activePlayers.randomOrNull()?.id ?: 0

            val newState = state.copy(
                phase = GamePhase.REVEALING,
                isSubRound = true,
                subRoundNumber = state.subRoundNumber + 1,
                secretWord = newSecretWord,
                secretClue = newSecretClue,
                categoryName = newCategoryName,
                players = resetPlayers,
                revealOrder = revealOrder,
                currentRevealIndex = 0,
                startingSpeakerIndex = startingSpeaker,
                votes = emptyMap(),
                currentVoterIndex = 0,
                timerRemainingSeconds = timerSeconds,
                isTimerRunning = false,
            )
            _gameState.value = newState
            persistCurrentState(newState)
        }
    }

    fun endSubRoundDiscussion() {
        countDownTimer?.cancel()
        val resetPlayers = _gameState.value.players.map { player ->
            if (player.id in _gameState.value.eliminatedPlayerIds) {
                player
            } else {
                player.copy(hasVoted = false, votedForId = null)
            }
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

        val subAccusedPlayer = state.players.find { it.id == subAccusedId }
        val isSubAccusedImposter = subAccusedPlayer?.role == Role.IMPOSTER
        val updatedEliminated = (state.eliminatedPlayerIds + subAccusedId).distinct()

        val allImposters = state.players.filter { it.role == Role.IMPOSTER }
        val remainingImposters = allImposters.filter { it.id !in updatedEliminated }

        // Update secured points: every imposter who survived this sub-round secures +1 point
        val updatedSecured = state.imposterSecuredPoints.toMutableMap()
        remainingImposters.forEach { imposter ->
            updatedSecured[imposter.id] = (updatedSecured[imposter.id] ?: 0) + 1
        }

        val finalScores = mutableMapOf<String, Int>()

        if (!isSubAccusedImposter) {
            // Rule 1: Civilian was accused in sub-round -> Round ends immediately and Imposters Win!
            allImposters.forEach { imposter ->
                val isSurviving = imposter.id !in updatedEliminated
                val secured = updatedSecured[imposter.id] ?: 0
                finalScores[imposter.name] = if (isSurviving) secured + 1 else secured
            }
            state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                val priorBonus = state.pendingRoundScores.getOrDefault(civilian.name, 0)
                finalScores[civilian.name] = if (priorBonus > 0) 1 else 0
            }

            val newState = state.copy(
                phase = GamePhase.RESULT,
                accusedPlayerId = subAccusedId,
                isImposterFound = false,
                eliminatedPlayerIds = updatedEliminated,
                imposterSecuredPoints = updatedSecured,
                finalPhaseDescription = "${subAccusedPlayer?.name ?: "A Civilian"} was eliminated in Sub-Round ${state.subRoundNumber}! Imposters Win!",
                pendingRoundScores = finalScores,
            )
            _gameState.value = newState
            persistCurrentState(newState)
            saveRoundScores(isCivilianWin = false, roundScoresMap = finalScores)
        } else {
            // An imposter was accused in sub-round!
            if (remainingImposters.isEmpty()) {
                // All imposters eliminated -> Civilians Win!
                state.players.forEach { player ->
                    if (player.role == Role.CIVILIAN) {
                        val priorBonus = state.pendingRoundScores.getOrDefault(player.name, 0)
                        val subVoteId = state.votes[player.id]
                        val votedForAccusedImposter = (subVoteId == subAccusedId)
                        val subBonus = if (votedForAccusedImposter) 1 else 0
                        finalScores[player.name] = 1 + (if (priorBonus > 0 || subBonus > 0) 1 else 0)
                    } else {
                        finalScores[player.name] = updatedSecured[player.id] ?: 0
                    }
                }
                val newState = state.copy(
                    phase = GamePhase.RESULT,
                    accusedPlayerId = subAccusedId,
                    isImposterFound = true,
                    eliminatedPlayerIds = updatedEliminated,
                    imposterSecuredPoints = updatedSecured,
                    finalPhaseDescription = "All Imposters Were Eliminated in Sub-Round ${state.subRoundNumber}! Civilians Win!",
                    pendingRoundScores = finalScores,
                )
                _gameState.value = newState
                persistCurrentState(newState)
                saveRoundScores(isCivilianWin = true, roundScoresMap = finalScores)
            } else {
                // Other imposter(s) remain -> Enter Imposter Choice Phase for next sub-round!
                val updatedPending = state.pendingRoundScores.toMutableMap()
                state.players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
                    val subVoteId = state.votes[civilian.id]
                    if (subVoteId == subAccusedId) {
                        updatedPending[civilian.name] = 1
                    }
                }
                allImposters.forEach { imposter ->
                    updatedPending[imposter.name] = updatedSecured[imposter.id] ?: 0
                }

                val newState = state.copy(
                    phase = GamePhase.FINAL_IMPOSTER_CHOICE,
                    accusedPlayerId = subAccusedId,
                    isImposterFound = true,
                    eliminatedPlayerIds = updatedEliminated,
                    imposterSecuredPoints = updatedSecured,
                    remainingImposterId = remainingImposters.first().id,
                    finalPhaseDescription = "${subAccusedPlayer?.name ?: "An Imposter"} was eliminated in Sub-Round ${state.subRoundNumber}! ${remainingImposters.size} Imposter(s) remain.",
                    pendingRoundScores = updatedPending,
                )
                _gameState.value = newState
                persistCurrentState(newState)
            }
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
            dataManager.recordRoundPlayed(
                secretWord = state.secretWord,
                imposterNames = state.players.filter { it.role == Role.IMPOSTER }.map { it.name }
            )
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
            subRoundNumber = 0,
            eliminatedPlayerIds = emptyList(),
            remainingImposterId = null,
            volunteerImposterId = null,
            imposterGuessWord = null,
            wasWordGuessedCorrectly = null,
            finalPhaseDescription = null,
            pendingRoundScores = emptyMap(),
            imposterSecuredPoints = emptyMap(),
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
            subRoundNumber = 0,
            eliminatedPlayerIds = emptyList(),
            remainingImposterId = null,
            volunteerImposterId = null,
            imposterGuessWord = null,
            wasWordGuessedCorrectly = null,
            finalPhaseDescription = null,
            pendingRoundScores = emptyMap(),
            imposterSecuredPoints = emptyMap(),
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

    fun saveWordPack(name: String, entries: List<Pair<String, String?>>, authorName: String? = null) {
        viewModelScope.launch {
            val author = authorName ?: currentUser.value?.username
            dataManager.saveWordPack(name, entries, author)
        }
    }

    fun updateWordPack(packId: Long, name: String, entries: List<Pair<String, String?>>, authorName: String? = null) {
        viewModelScope.launch {
            val author = authorName ?: currentUser.value?.username
            dataManager.updateWordPack(packId, name, entries, author)
        }
    }

    fun deleteWordPack(pack: WordPack) {
        viewModelScope.launch {
            dataManager.deleteWordPack(pack)
        }
    }

    fun resetRandomizerWeightage() {
        viewModelScope.launch {
            dataManager.resetRandomizerWeightage()
        }
    }

    // ── User Profile & Firebase ────────────────────────────

    fun createProfile(username: String, pin: String, onResult: (Result<UserProfile>) -> Unit) {
        viewModelScope.launch {
            val result = userManager.createProfile(username, pin)
            onResult(result)
        }
    }

    fun loginProfile(username: String, pin: String, onResult: (Result<UserProfile>) -> Unit) {
        viewModelScope.launch {
            val result = userManager.login(username, pin)
            onResult(result)
        }
    }

    fun logoutProfile() {
        userManager.logout()
    }

    // ── Community Word Packs ────────────────────────────────

    fun publishWordPackToCommunity(pack: WordPack, onResult: (Result<String>) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onResult(Result.failure(IllegalStateException("Please set up or log into your profile before publishing word packs.")))
            return
        }

        viewModelScope.launch {
            val entries = dataManager.getEntriesForPackOnce(pack.id)
            val result = cloudWordPackRepo.publishWordPack(pack, entries, user)
            onResult(result)
        }
    }

    fun downloadCommunityPack(pack: CommunityWordPack, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = cloudWordPackRepo.downloadCommunityPack(pack, dataManager)
            onResult(result)
        }
    }

    fun deleteCommunityPack(packId: String, onResult: (Result<Unit>) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onResult(Result.failure(IllegalStateException("Not logged in.")))
            return
        }

        viewModelScope.launch {
            val result = cloudWordPackRepo.deletePublishedPack(packId, user.userId)
            onResult(result)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}
