package com.example.imposterparty.data.randomizer

import com.example.imposterparty.data.model.GameHistoryRecord
import com.example.imposterparty.data.model.Player
import com.example.imposterparty.data.model.WordEntry
import java.util.Random

data class PlayerSimStats(
    val playerName: String,
    val imposterAppearances: Int,
    val imposterPercentage: Double,
    val maxConsecutiveStreak: Int,
    val twoConsecutiveCount: Int,
    val threeOrMoreConsecutiveCount: Int,
)

data class SimulationResult(
    val lobbySize: Int,
    val imposterCountPerRound: Int,
    val totalRounds: Int,
    val playerStats: Map<String, PlayerSimStats>,
    val maxConsecutiveStreakOverall: Int,
    val threeConsecutiveViolations: Int,
    val twoConsecutiveOccurrences: Int,
    val wordCooldownViolations: Int,
    val expectedTargetPercentage: Double,
    val maxPercentageDeviation: Double,
)

object FairRandomizerSimulator {

    /**
     * Simulates [rounds] of game loops with [playerCount] players and [imposterCount] imposters per round.
     */
    fun simulate(
        playerCount: Int,
        imposterCount: Int,
        rounds: Int,
        words: List<String> = defaultSimulationWords(),
        cooldownRounds: Int = FairRandomizer.DEFAULT_WORD_COOLDOWN_ROUNDS,
        seed: Long? = null,
    ): SimulationResult {
        val random = if (seed != null) Random(seed) else Random()
        val playerList = (0 until playerCount).map { Player(id = it, name = "Player ${it + 1}") }
        val wordEntries = words.mapIndexed { idx, w -> WordEntry(id = idx.toLong() + 1, packId = 1L, word = w) }

        val completedRounds = mutableListOf<GameHistoryRecord>()
        val wordUsageHistory = mutableMapOf<String, Int>()

        val playerConsecutiveStreak = mutableMapOf<Int, Int>()
        val playerMaxStreak = mutableMapOf<Int, Int>()
        val playerImposterCounts = mutableMapOf<Int, Int>()
        val playerTwoConsecutiveCounts = mutableMapOf<Int, Int>()
        val playerThreeOrMoreViolations = mutableMapOf<Int, Int>()

        var totalThreeConsecutiveViolations = 0
        var totalTwoConsecutiveOccurrences = 0
        var wordCooldownViolations = 0

        playerList.forEach { p ->
            playerConsecutiveStreak[p.id] = 0
            playerMaxStreak[p.id] = 0
            playerImposterCounts[p.id] = 0
            playerTwoConsecutiveCounts[p.id] = 0
            playerThreeOrMoreViolations[p.id] = 0
        }

        for (roundNum in 1..rounds) {
            // Build histories from completed rounds in current match
            val histories = FairRandomizer.buildPlayerHistories(playerList, completedRounds, roundNum)

            // Select imposters
            val chosenImposterIds = FairRandomizer.selectImposters(
                players = playerList,
                imposterCount = imposterCount,
                playerHistories = histories,
                random = random,
            )

            // Select word
            val chosenEntry = FairRandomizer.selectSecretWord(
                candidateEntries = wordEntries,
                wordUsageHistory = wordUsageHistory,
                currentRound = roundNum,
                cooldownRounds = cooldownRounds,
                random = random,
            )

            val normWord = FairRandomizer.normalizeWord(chosenEntry.word)
            val lastUsed = wordUsageHistory[normWord] ?: -1
            if (lastUsed > 0 && (roundNum - lastUsed) <= cooldownRounds) {
                // If the total pool was large enough to avoid cooldown fallback, this is a violation
                if (wordEntries.size > cooldownRounds + 3) {
                    wordCooldownViolations++
                }
            }
            wordUsageHistory[normWord] = roundNum

            // Track imposter streaks and stats
            val imposterNames = mutableListOf<String>()
            val civilianNames = mutableListOf<String>()

            playerList.forEach { p ->
                val isImposter = p.id in chosenImposterIds
                if (isImposter) {
                    imposterNames.add(p.name)
                    playerImposterCounts[p.id] = (playerImposterCounts[p.id] ?: 0) + 1
                    val newStreak = (playerConsecutiveStreak[p.id] ?: 0) + 1
                    playerConsecutiveStreak[p.id] = newStreak

                    val currMax = playerMaxStreak[p.id] ?: 0
                    if (newStreak > currMax) {
                        playerMaxStreak[p.id] = newStreak
                    }

                    if (newStreak == 2) {
                        playerTwoConsecutiveCounts[p.id] = (playerTwoConsecutiveCounts[p.id] ?: 0) + 1
                        totalTwoConsecutiveOccurrences++
                    } else if (newStreak >= 3) {
                        playerThreeOrMoreViolations[p.id] = (playerThreeOrMoreViolations[p.id] ?: 0) + 1
                        totalThreeConsecutiveViolations++
                    }
                } else {
                    civilianNames.add(p.name)
                    playerConsecutiveStreak[p.id] = 0
                }
            }

            val record = GameHistoryRecord(
                id = roundNum.toLong(),
                timestamp = System.currentTimeMillis(),
                roundNumber = roundNum,
                categoryName = "Category 1",
                secretWord = chosenEntry.word,
                secretClue = chosenEntry.clue,
                imposterNames = imposterNames,
                civilianNames = civilianNames,
            )
            completedRounds.add(record)
        }

        val expectedTargetPercentage = (imposterCount.toDouble() / playerCount.toDouble()) * 100.0
        var maxDev = 0.0

        val statsMap = playerList.associate { p ->
            val count = playerImposterCounts[p.id] ?: 0
            val pct = (count.toDouble() / rounds.toDouble()) * 100.0
            val dev = kotlin.math.abs(pct - expectedTargetPercentage)
            if (dev > maxDev) {
                maxDev = dev
            }
            p.name to PlayerSimStats(
                playerName = p.name,
                imposterAppearances = count,
                imposterPercentage = pct,
                maxConsecutiveStreak = playerMaxStreak[p.id] ?: 0,
                twoConsecutiveCount = playerTwoConsecutiveCounts[p.id] ?: 0,
                threeOrMoreConsecutiveCount = playerThreeOrMoreViolations[p.id] ?: 0,
            )
        }

        val maxStreakOverall = playerMaxStreak.values.maxOrNull() ?: 0

        return SimulationResult(
            lobbySize = playerCount,
            imposterCountPerRound = imposterCount,
            totalRounds = rounds,
            playerStats = statsMap,
            maxConsecutiveStreakOverall = maxStreakOverall,
            threeConsecutiveViolations = totalThreeConsecutiveViolations,
            twoConsecutiveOccurrences = totalTwoConsecutiveOccurrences,
            wordCooldownViolations = wordCooldownViolations,
            expectedTargetPercentage = expectedTargetPercentage,
            maxPercentageDeviation = maxDev,
        )
    }

    private fun defaultSimulationWords(): List<String> {
        return listOf(
            "Apple", "Banana", "Cherry", "Date", "Elderberry",
            "Fig", "Grape", "Honeydew", "Kiwi", "Lemon",
            "Mango", "Nectarine", "Orange", "Papaya", "Quince",
            "Raspberry", "Strawberry", "Tangerine", "Ugli", "Vanilla",
            "Watermelon", "Xigua", "Yellow Passionfruit", "Zucchini",
            "Airplane", "Bicycle", "Car", "Drone", "Elevator", "Ferry"
        )
    }
}
