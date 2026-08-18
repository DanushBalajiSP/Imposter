package com.example.imposterparty

import com.example.imposterparty.data.model.GameHistoryRecord
import com.example.imposterparty.data.model.Player
import com.example.imposterparty.data.model.Role
import com.example.imposterparty.data.model.WordEntry
import com.example.imposterparty.data.randomizer.FairRandomizer
import com.example.imposterparty.data.randomizer.FairRandomizerSimulator
import com.example.imposterparty.data.randomizer.PlayerImposterHistory
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class FairRandomizerTest {

    // ── Test 1: No 3 Consecutive Imposter Appearances (Hard Rule) ──
    @Test
    fun `test 1 - no player becomes imposter for 3 consecutive rounds`() {
        val simResult = FairRandomizerSimulator.simulate(
            playerCount = 10,
            imposterCount = 2,
            rounds = 1000,
            seed = 42L,
        )

        assertEquals("No 3-consecutive imposter violations should occur", 0, simResult.threeConsecutiveViolations)
        assertTrue("Max streak should be <= 2", simResult.maxConsecutiveStreakOverall <= 2)
    }

    // ── Test 2: Two Consecutive Appearances Are Possible (Soft Rule) ──
    @Test
    fun `test 2 - two consecutive imposter appearances are possible and observed`() {
        val simResult = FairRandomizerSimulator.simulate(
            playerCount = 10,
            imposterCount = 2,
            rounds = 1000,
            seed = 12345L,
        )

        assertTrue("Two consecutive appearances should occur occasionally", simResult.twoConsecutiveOccurrences > 0)
        assertEquals("Three consecutive violations must remain 0", 0, simResult.threeConsecutiveViolations)
    }

    // ── Test 3: Long-term Fairness Distribution ──
    @Test
    fun `test 3 - long term imposter assignments are statistically balanced`() {
        val playerCount = 10
        val imposterCount = 2
        val rounds = 10000

        val simResult = FairRandomizerSimulator.simulate(
            playerCount = playerCount,
            imposterCount = imposterCount,
            rounds = rounds,
            seed = 99999L,
        )

        val targetPct = (imposterCount.toDouble() / playerCount.toDouble()) * 100.0 // 20.0%
        val maxAllowedDeviation = 4.0 // within ±4%

        simResult.playerStats.values.forEach { stat ->
            val dev = kotlin.math.abs(stat.imposterPercentage - targetPct)
            assertTrue(
                "Player ${stat.playerName} percentage ${stat.imposterPercentage}% deviated $dev% from target $targetPct%",
                dev <= maxAllowedDeviation
            )
        }
        assertEquals(0, simResult.threeConsecutiveViolations)
    }

    // ── Test 4: Global Word Cooldown ──
    @Test
    fun `test 4 - word cannot appear again within 10 rounds cooldown`() {
        val words = (1..30).map { "Word_$it" }
        val simResult = FairRandomizerSimulator.simulate(
            playerCount = 8,
            imposterCount = 2,
            rounds = 500,
            words = words,
            cooldownRounds = 10,
            seed = 777L,
        )

        assertEquals("No word cooldown violations when pool is large", 0, simResult.wordCooldownViolations)
    }

    // ── Test 5: Cross-Category Duplicate Global Cooldown ──
    @Test
    fun `test 5 - duplicate word across different categories shares global cooldown`() {
        val moviePopcorn = WordEntry(id = 1, packId = 100, word = "Popcorn", clue = "Movie snack")
        val foodPopcorn = WordEntry(id = 2, packId = 200, word = "  popcorn  ", clue = "Food item")
        val foodBurger = WordEntry(id = 3, packId = 200, word = "Burger", clue = "Fast food")
        val foodPizza = WordEntry(id = 4, packId = 200, word = "Pizza", clue = "Italian dish")

        val wordHistory = mutableMapOf<String, Int>()

        // Round 1: "Popcorn" is used in Movies pack
        val normMovie = FairRandomizer.normalizeWord(moviePopcorn.word)
        wordHistory[normMovie] = 1

        // In Rounds 2 through 11 (within 10 rounds), foodPopcorn must also be in cooldown
        for (round in 2..11) {
            val normFood = FairRandomizer.normalizeWord(foodPopcorn.word)
            val isCooldown = FairRandomizer.isWordOnCooldown(
                lastUsedRound = wordHistory[normFood] ?: -1,
                currentRound = round,
                cooldownRounds = 10,
            )
            assertTrue("Popcorn in Food pack should share global cooldown in round $round", isCooldown)
        }

        // When selecting from Food pack during cooldown (round 2), Popcorn must not be chosen
        val foodEntries = listOf(foodPopcorn, foodBurger, foodPizza)
        for (seed in 1..20) {
            val selected = FairRandomizer.selectSecretWord(
                candidateEntries = foodEntries,
                wordUsageHistory = wordHistory,
                currentRound = 2,
                cooldownRounds = 10,
                random = Random(seed.toLong()),
            )
            assertNotEquals(
                "Popcorn should not be selected while under global cooldown",
                "popcorn",
                FairRandomizer.normalizeWord(selected.word)
            )
        }

        // In Round 12, Popcorn becomes available again
        val normFood = FairRandomizer.normalizeWord(foodPopcorn.word)
        val isCooldownRound12 = FairRandomizer.isWordOnCooldown(
            lastUsedRound = wordHistory[normFood] ?: -1,
            currentRound = 12,
            cooldownRounds = 10,
        )
        assertFalse("Popcorn should be available in round 12", isCooldownRound12)
    }

    // ── Test 6: Small Word Pack Graceful Fallback ──
    @Test
    fun `test 6 - small word pack of 8 words does not crash over 30 rounds`() {
        val smallWords = listOf("Dog", "Cat", "Bird", "Fish", "Bear", "Wolf", "Lion", "Tiger")
        val simResult = FairRandomizerSimulator.simulate(
            playerCount = 6,
            imposterCount = 1,
            rounds = 35,
            words = smallWords,
            cooldownRounds = 10,
            seed = 555L,
        )

        assertEquals("Should complete all rounds", 35, simResult.totalRounds)
    }

    // ── Test 7: Multiple Imposters (24 Players, 5 Imposters) ──
    @Test
    fun `test 7 - sequential weighted selection picks unique valid imposters`() {
        val players = (1..24).map { Player(id = it - 1, name = "Player $it") }
        val playerHistories = mutableMapOf<Int, PlayerImposterHistory>()

        for (round in 1..200) {
            val imposterIds = FairRandomizer.selectImposters(
                players = players,
                imposterCount = 5,
                playerHistories = playerHistories,
                random = Random(round.toLong()),
            )

            assertEquals("Must select exactly 5 imposters", 5, imposterIds.size)
            imposterIds.forEach { id ->
                assertTrue("Selected ID must be a valid player", id in 0..23)
            }
        }
    }

    // ── Test 8: Match Reset Clears Fairness History ──
    @Test
    fun `test 8 - match reset creates clean slate for player fairness`() {
        val players = (1..6).map { Player(id = it - 1, name = "Player $it") }

        // Simulated match with round history
        val completedRounds = listOf(
            GameHistoryRecord(
                id = 1,
                roundNumber = 1,
                imposterNames = listOf("Player 1"),
                civilianNames = listOf("Player 2", "Player 3", "Player 4", "Player 5", "Player 6"),
            ),
            GameHistoryRecord(
                id = 2,
                roundNumber = 2,
                imposterNames = listOf("Player 1"),
                civilianNames = listOf("Player 2", "Player 3", "Player 4", "Player 5", "Player 6"),
            ),
        )

        val historyBeforeReset = FairRandomizer.buildPlayerHistories(players, completedRounds, 3)
        val p1History = historyBeforeReset[0]!!
        assertTrue(p1History.previousRoundWasImposter)
        assertTrue(p1History.twoRoundsAgoWasImposter)

        // Hard exclusion applies
        val weightBeforeReset = FairRandomizer.calculateImposterWeight(p1History, 6, 2)
        assertEquals(0.0, weightBeforeReset, 0.0001)

        // Starting a new match (empty completed rounds)
        val historyAfterReset = FairRandomizer.buildPlayerHistories(players, emptyList(), 1)
        val p1HistoryAfter = historyAfterReset[0]!!
        assertFalse(p1HistoryAfter.previousRoundWasImposter)
        assertFalse(p1HistoryAfter.twoRoundsAgoWasImposter)
        assertEquals(0, p1HistoryAfter.totalImposterCount)

        val weightAfterReset = FairRandomizer.calculateImposterWeight(p1HistoryAfter, 6, 0)
        assertTrue(weightAfterReset > 0.5)
    }

    // ── Multi-Lobby Size Simulation Tests (6, 10, 12, 18, 24 players) ──
    @Test
    fun `test multi-lobby simulations for 6, 10, 12, 18, and 24 players`() {
        val testConfigs = listOf(
            Pair(6, 1),
            Pair(10, 2),
            Pair(12, 3),
            Pair(18, 4),
            Pair(24, 5),
        )

        testConfigs.forEach { (players, imposters) ->
            val result = FairRandomizerSimulator.simulate(
                playerCount = players,
                imposterCount = imposters,
                rounds = 1000,
                seed = 42L + players,
            )

            assertEquals("Lobby ($players players, $imposters imposters) must have 0 three-consecutive violations", 0, result.threeConsecutiveViolations)
            assertTrue("Lobby ($players players, $imposters imposters) max consecutive streak must be <= 2", result.maxConsecutiveStreakOverall <= 2)
            assertEquals("Lobby ($players players, $imposters imposters) must have 0 word cooldown violations", 0, result.wordCooldownViolations)
        }
    }
}
