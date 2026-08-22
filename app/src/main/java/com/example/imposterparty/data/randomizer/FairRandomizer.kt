package com.example.imposterparty.data.randomizer

import com.example.imposterparty.data.model.Player
import com.example.imposterparty.data.model.Role
import com.example.imposterparty.data.model.WordEntry
import java.util.Random

/**
 * Historical imposter tracking data for a player within a match.
 */
data class PlayerImposterHistory(
    val playerId: Int,
    val playerName: String,
    val totalImposterCount: Int = 0,
    val lastImposterRound: Int = -1,
    val previousRoundWasImposter: Boolean = false,
    val twoRoundsAgoWasImposter: Boolean = false,
    val roundsSinceImposter: Int = Int.MAX_VALUE,
)

/**
 * Core Fair Randomizer engine implementing controlled randomness,
 * anti-repetition constraints, and weighted distribution.
 */
object FairRandomizer {

    // Configurable constants for imposter weighting
    const val HARD_EXCLUSION_CONSECUTIVE_LIMIT = 2 // If imposter in past 2 consecutive rounds, 3rd is forbidden
    const val CONSECUTIVE_PENALTY_FACTOR = 0.15 // Soft penalty if was imposter in the immediately previous round
    const val MAX_RECENCY_WEIGHT = 1.30 // Cap for recency boost
    const val BASE_WEIGHT = 1.0

    // Configurable constant for secret word cooldown
    const val DEFAULT_WORD_COOLDOWN_ROUNDS = 10

    /**
     * Calculates the selection weight for a player becoming an Imposter in the current round.
     */
    fun calculateImposterWeight(
        history: PlayerImposterHistory,
        totalPlayers: Int,
        totalImposterAssignmentsSoFar: Int,
    ): Double {
        // ── Hard Rule: No 3 consecutive imposter rounds ──
        // If a player was imposter in the previous 2 consecutive rounds, selection weight is strictly 0.
        if (history.previousRoundWasImposter && history.twoRoundsAgoWasImposter) {
            return 0.0
        }

        // ── Soft Rule: 1 consecutive previous imposter round ──
        val streakFactor = if (history.previousRoundWasImposter) {
            CONSECUTIVE_PENALTY_FACTOR
        } else {
            1.0
        }

        // ── Recency Factor ──
        // Players who have not been imposter for several rounds gradually receive higher probability
        val recencyFactor = when {
            history.roundsSinceImposter == 0 -> 0.15 // Just was imposter in previous round
            history.roundsSinceImposter == 1 -> 0.60
            history.roundsSinceImposter == 2 -> 1.00
            history.roundsSinceImposter == 3 -> 1.15
            history.roundsSinceImposter == 4 -> 1.25
            else -> MAX_RECENCY_WEIGHT // 5+ rounds or never was imposter in this match
        }

        // ── Match Lifetime Balance Factor ──
        // Compare player's total imposter count against the lobby average
        val averageImposterCount = if (totalPlayers > 0) {
            totalImposterAssignmentsSoFar.toDouble() / totalPlayers.toDouble()
        } else {
            0.0
        }

        val diffFromAvg = history.totalImposterCount - averageImposterCount
        val lifetimeFairnessFactor = when {
            diffFromAvg <= -1.5 -> 1.50 // Far below average -> boost
            diffFromAvg <= -0.5 -> 1.25 // Below average -> moderate boost
            diffFromAvg < 0.5 -> 1.00   // Near average -> neutral
            diffFromAvg < 1.5 -> 0.75   // Above average -> moderate penalty
            else -> 0.50                // Far above average -> strong penalty
        }

        val finalWeight = BASE_WEIGHT * recencyFactor * lifetimeFairnessFactor * streakFactor
        return maxOf(0.0, finalWeight)
    }

    /**
     * Performs sequential weighted random selection for [imposterCount] unique imposters.
     */
    fun selectImposters(
        players: List<Player>,
        imposterCount: Int,
        playerHistories: Map<Int, PlayerImposterHistory>,
        random: Random = Random(),
    ): Set<Int> {
        if (players.isEmpty()) return emptySet()
        val targetCount = imposterCount.coerceIn(1, players.size)

        val totalImposterAssignmentsSoFar = playerHistories.values.sumOf { it.totalImposterCount }
        val remainingCandidates = players.map { it.id }.toMutableList()
        val selectedImposters = mutableSetOf<Int>()

        while (selectedImposters.size < targetCount && remainingCandidates.isNotEmpty()) {
            // Compute weights for remaining candidates
            val candidateWeights = remainingCandidates.map { playerId ->
                val history = playerHistories[playerId] ?: PlayerImposterHistory(
                    playerId = playerId,
                    playerName = players.find { it.id == playerId }?.name ?: "Player $playerId",
                )
                calculateImposterWeight(history, players.size, totalImposterAssignmentsSoFar)
            }

            val totalWeight = candidateWeights.sum()

            val chosenId = if (totalWeight <= 0.0001) {
                // If all remaining candidates have 0 weight (extreme edge case), fallback to uniform random
                val fallbackIdx = random.nextInt(remainingCandidates.size)
                remainingCandidates[fallbackIdx]
            } else {
                // True weighted random selection
                var roll = random.nextDouble() * totalWeight
                var selectedId = remainingCandidates.last()
                for (i in remainingCandidates.indices) {
                    roll -= candidateWeights[i]
                    if (roll <= 0.0) {
                        selectedId = remainingCandidates[i]
                        break
                    }
                }
                selectedId
            }

            selectedImposters.add(chosenId)
            remainingCandidates.remove(chosenId)
        }

        return selectedImposters
    }

    /**
     * Normalizes a word string for global identity matching across categories.
     */
    fun normalizeWord(word: String): String {
        return word.trim().lowercase()
    }

    /**
     * Checks whether a word is currently in the global cooldown window.
     * @param lastUsedRound The round number when this word was last used (-1 if never used).
     * @param currentRound The current round number.
     * @param cooldownRounds Number of rounds to enforce cooldown (default: 10).
     */
    fun isWordOnCooldown(
        lastUsedRound: Int,
        currentRound: Int,
        cooldownRounds: Int = DEFAULT_WORD_COOLDOWN_ROUNDS,
    ): Boolean {
        if (lastUsedRound < 0) return false
        val roundsSinceUse = currentRound - lastUsedRound
        return roundsSinceUse in 1..cooldownRounds
    }

    /**
     * Selects a category with recency balancing if multiple packs are available.
     */
    fun selectCategory(
        availablePackIds: List<Long>,
        categoryLastUsedRound: Map<Long, Int>,
        currentRound: Int,
        random: Random = Random(),
    ): Long {
        if (availablePackIds.isEmpty()) return -1L
        if (availablePackIds.size == 1) return availablePackIds.first()

        val weights = availablePackIds.map { packId ->
            val lastUsed = categoryLastUsedRound[packId] ?: -1
            if (lastUsed == currentRound - 1) {
                0.35 // Was used in the immediately previous round -> recency penalty
            } else if (lastUsed == currentRound - 2) {
                0.75
            } else {
                1.0
            }
        }

        val totalWeight = weights.sum()
        var roll = random.nextDouble() * totalWeight
        var chosenPackId = availablePackIds.last()
        for (i in availablePackIds.indices) {
            roll -= weights[i]
            if (roll <= 0.0) {
                chosenPackId = availablePackIds[i]
                break
            }
        }
        return chosenPackId
    }

    /**
     * Selects a secret word using global cooldown, shuffle-bag weighting, and fallback strategy.
     *
     * @param candidateEntries All word entries available for the round.
     * @param wordUsageHistory Map of normalized word -> round number when last used.
     * @param currentRound Current round number.
     * @param cooldownRounds Cooldown window (default: 10).
     * @param random Random generator.
     */
    fun selectSecretWord(
        candidateEntries: List<WordEntry>,
        wordUsageHistory: Map<String, Int>,
        currentRound: Int,
        cooldownRounds: Int = DEFAULT_WORD_COOLDOWN_ROUNDS,
        random: Random = Random(),
    ): WordEntry {
        require(candidateEntries.isNotEmpty()) { "candidateEntries cannot be empty" }

        // Deduplicate entries by normalized word for calculation while keeping full WordEntry
        val uniqueEntries = candidateEntries.distinctBy { normalizeWord(it.word) }

        // Filter out words that are within the cooldown window
        val eligibleEntries = uniqueEntries.filterNot { entry ->
            val norm = normalizeWord(entry.word)
            val lastUsed = wordUsageHistory[norm] ?: -1
            isWordOnCooldown(lastUsed, currentRound, cooldownRounds)
        }

        // Fallback strategy when eligible entries are fewer than 3
        val poolToChooseFrom = when {
            eligibleEntries.size >= 3 -> eligibleEntries
            eligibleEntries.size == 2 -> eligibleEntries
            eligibleEntries.size == 1 -> eligibleEntries
            else -> {
                // If 0 eligible words (e.g. small pack of 8 words with 10 cooldown):
                // Release the oldest-used word(s)
                val sortedByOldest = uniqueEntries.sortedBy { entry ->
                    wordUsageHistory[normalizeWord(entry.word)] ?: -1
                }
                // Take the oldest half or at least 2 entries
                val fallbackCount = maxOf(1, sortedByOldest.size / 2)
                sortedByOldest.take(fallbackCount)
            }
        }

        // Weighted shuffle-bag selection: words never used or oldest used receive higher weight
        val weights = poolToChooseFrom.map { entry ->
            val lastUsed = wordUsageHistory[normalizeWord(entry.word)] ?: -1
            if (lastUsed < 0) {
                1.5 // Never used -> higher priority
            } else {
                val roundsSince = currentRound - lastUsed
                when {
                    roundsSince > cooldownRounds * 2 -> 1.4
                    roundsSince > cooldownRounds -> 1.2
                    else -> 1.0
                }
            }
        }

        val totalWeight = weights.sum()
        var roll = random.nextDouble() * totalWeight
        var selectedEntry = poolToChooseFrom.last()
        for (i in poolToChooseFrom.indices) {
            roll -= weights[i]
            if (roll <= 0.0) {
                selectedEntry = poolToChooseFrom[i]
                break
            }
        }

        return selectedEntry
    }

    /**
     * Builds player imposter histories from the list of completed round records in a match.
     */
    fun buildPlayerHistories(
        players: List<Player>,
        completedRounds: List<com.example.imposterparty.data.model.GameHistoryRecord>,
        currentRoundNumber: Int,
    ): Map<Int, PlayerImposterHistory> {
        val totalRounds = completedRounds.size

        return players.associate { player ->
            var imposterCount = 0
            var lastImposterRound = -1

            completedRounds.forEach { round ->
                if (round.imposterNames.any { it.equals(player.name.trim(), ignoreCase = true) }) {
                    imposterCount++
                    if (round.roundNumber > lastImposterRound) {
                        lastImposterRound = round.roundNumber
                    }
                }
            }

            val lastRoundRecord = completedRounds.lastOrNull()
            val previousRoundWasImposter = lastRoundRecord?.imposterNames?.any {
                it.equals(player.name.trim(), ignoreCase = true)
            } ?: false

            val secondLastRoundRecord = if (completedRounds.size >= 2) completedRounds[completedRounds.size - 2] else null
            val twoRoundsAgoWasImposter = secondLastRoundRecord?.imposterNames?.any {
                it.equals(player.name.trim(), ignoreCase = true)
            } ?: false

            val roundsSince = if (lastImposterRound > 0) {
                currentRoundNumber - lastImposterRound - 1
            } else {
                Int.MAX_VALUE
            }

            player.id to PlayerImposterHistory(
                playerId = player.id,
                playerName = player.name,
                totalImposterCount = imposterCount,
                lastImposterRound = lastImposterRound,
                previousRoundWasImposter = previousRoundWasImposter,
                twoRoundsAgoWasImposter = twoRoundsAgoWasImposter,
                roundsSinceImposter = roundsSince,
            )
        }
    }

    /**
     * Builds player imposter histories from global persistent RandomizerState across all matches.
     */
    fun buildPlayerHistoriesFromState(
        players: List<Player>,
        playerImposterCounts: Map<String, Int>,
        recentImposterRounds: List<List<String>>, // most recent round first
    ): Map<Int, PlayerImposterHistory> {
        return players.associate { player ->
            val pName = player.name.trim().lowercase()
            val totalCount = playerImposterCounts[pName] ?: 0

            val prevRoundNames = recentImposterRounds.getOrNull(0)?.map { it.trim().lowercase() } ?: emptyList()
            val twoRoundsAgoNames = recentImposterRounds.getOrNull(1)?.map { it.trim().lowercase() } ?: emptyList()

            val previousRoundWasImposter = pName in prevRoundNames
            val twoRoundsAgoWasImposter = pName in twoRoundsAgoNames

            // Find index of most recent round where this player was imposter
            var roundsSince = Int.MAX_VALUE
            for (idx in recentImposterRounds.indices) {
                if (recentImposterRounds[idx].any { it.trim().equals(pName, ignoreCase = true) }) {
                    roundsSince = idx
                    break
                }
            }

            player.id to PlayerImposterHistory(
                playerId = player.id,
                playerName = player.name,
                totalImposterCount = totalCount,
                lastImposterRound = if (roundsSince == Int.MAX_VALUE) -1 else roundsSince,
                previousRoundWasImposter = previousRoundWasImposter,
                twoRoundsAgoWasImposter = twoRoundsAgoWasImposter,
                roundsSinceImposter = roundsSince,
            )
        }
    }
}
