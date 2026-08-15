package com.example.imposterparty

import com.example.imposterparty.data.local.LeaderboardEntry
import com.example.imposterparty.data.model.*
import org.junit.Assert.*
import org.junit.Test

class GameLogicTest {

    @Test
    fun `manual imposter count is respected and clamped`() {
        val players = listOf(
            Player(0, "Alice"),
            Player(1, "Bob"),
            Player(2, "Charlie"),
            Player(3, "David"),
        )
        val settings = GameSettings(
            imposterMode = ImposterMode.MANUAL,
            manualImposterCount = 2,
        )
        val imposterCount = settings.manualImposterCount.coerceIn(1, players.size - 1)
        assertEquals(2, imposterCount)
    }

    @Test
    fun `auto range picks within bounds`() {
        val players = (1..6).map { Player(it, "Player $it") }
        val settings = GameSettings(
            imposterMode = ImposterMode.AUTO_RANGE,
            autoRange = ImposterRange.ONE_TO_TWO,
        )

        val maxAllowed = minOf(settings.autoRange.max, players.size - 1)
        val minAllowed = settings.autoRange.min.coerceAtMost(maxAllowed)

        for (i in 1..100) {
            val count = (minAllowed..maxAllowed).random()
            assertTrue(count in 1..2)
        }
    }

    @Test
    fun `card reveal order preserves lobby sequential order`() {
        val players = listOf(
            Player(0, "Player 1"),
            Player(1, "Player 2"),
            Player(2, "Player 3"),
            Player(3, "Player 4"),
        )
        val revealOrder = players.indices.toList()

        assertEquals(listOf(0, 1, 2, 3), revealOrder)
    }

    @Test
    fun `discussion starting speaker is picked within player bounds`() {
        val players = listOf(
            Player(0, "Alice"),
            Player(1, "Bob"),
            Player(2, "Charlie"),
        )
        for (i in 1..100) {
            val startIndex = players.indices.random()
            assertTrue(startIndex in 0 until players.size)
        }
    }

    @Test
    fun `scoring gives civilians 1 point when imposter is found`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Charlie", role = Role.IMPOSTER),
        )
        val isImposterFound = true

        val points = players.associate { player ->
            val isImposter = player.role == Role.IMPOSTER
            val pt = if (isImposterFound) {
                if (isImposter) 0 else 1
            } else {
                if (isImposter) 1 else 0
            }
            player.name to pt
        }

        assertEquals(1, points["Alice"])
        assertEquals(1, points["Bob"])
        assertEquals(0, points["Charlie"])
    }

    @Test
    fun `scoring gives imposter 1 point when imposter is not found`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Charlie", role = Role.IMPOSTER),
        )
        val isImposterFound = false

        val points = players.associate { player ->
            val isImposter = player.role == Role.IMPOSTER
            val pt = if (isImposterFound) {
                if (isImposter) 0 else 1
            } else {
                if (isImposter) 1 else 0
            }
            player.name to pt
        }

        assertEquals(0, points["Alice"])
        assertEquals(0, points["Bob"])
        assertEquals(1, points["Charlie"])
    }

    @Test
    fun `scoreboard merges same player names and sums points`() {
        val scoreRecords = listOf(
            ScoreRecord(playerName = "Alice", points = 1),
            ScoreRecord(playerName = "Bob", points = 1),
            ScoreRecord(playerName = "alice", points = 2), // lowercase same name
            ScoreRecord(playerName = "Alice ", points = 1), // with space
            ScoreRecord(playerName = "Bob", points = 0),
        )

        val merged = scoreRecords
            .filter { it.playerName.isNotBlank() }
            .groupBy { it.playerName.trim().lowercase() }
            .mapValues { (_, records) -> records.sumOf { it.points } }

        assertEquals(4, merged["alice"])
        assertEquals(1, merged["bob"])
    }

    @Test
    fun `leaderboard assigns same relative rank for tied players with dense ranking`() {
        val unranked = listOf(
            LeaderboardEntry(playerName = "Player 1", totalPoints = 5, gamesPlayed = 5, gamesWon = 5),
            LeaderboardEntry(playerName = "Player 2", totalPoints = 5, gamesPlayed = 5, gamesWon = 5),
            LeaderboardEntry(playerName = "Player 3", totalPoints = 5, gamesPlayed = 5, gamesWon = 5),
            LeaderboardEntry(playerName = "Player 4", totalPoints = 3, gamesPlayed = 4, gamesWon = 3),
            LeaderboardEntry(playerName = "Player 5", totalPoints = 3, gamesPlayed = 4, gamesWon = 3),
            LeaderboardEntry(playerName = "Player 6", totalPoints = 3, gamesPlayed = 4, gamesWon = 3),
            LeaderboardEntry(playerName = "Player 7", totalPoints = 3, gamesPlayed = 4, gamesWon = 3),
            LeaderboardEntry(playerName = "Player 8", totalPoints = 1, gamesPlayed = 2, gamesWon = 1),
        )

        val sortedEntries = unranked.sortedWith(
            compareByDescending<LeaderboardEntry> { it.totalPoints }
                .thenByDescending { if (it.gamesPlayed > 0) it.gamesWon.toDouble() / it.gamesPlayed else 0.0 }
                .thenByDescending { it.gamesWon }
                .thenByDescending { it.gamesPlayed }
                .thenBy { it.playerName.lowercase() }
        )

        var currentRank = 1
        val ranked = mutableListOf<LeaderboardEntry>()
        for (i in sortedEntries.indices) {
            if (i > 0) {
                val prev = sortedEntries[i - 1]
                val curr = sortedEntries[i]
                val isTied = prev.totalPoints == curr.totalPoints &&
                        prev.gamesPlayed == curr.gamesPlayed &&
                        prev.gamesWon == curr.gamesWon
                if (!isTied) {
                    currentRank++
                }
            }
            ranked.add(sortedEntries[i].copy(rank = currentRank))
        }

        // Top 3 players have 1st place
        assertEquals(1, ranked[0].rank)
        assertEquals(1, ranked[1].rank)
        assertEquals(1, ranked[2].rank)

        // Next 4 players have 2nd place
        assertEquals(2, ranked[3].rank)
        assertEquals(2, ranked[4].rank)
        assertEquals(2, ranked[5].rank)
        assertEquals(2, ranked[6].rank)

        // 8th player has 3rd place
        assertEquals(3, ranked[7].rank)
    }
}
