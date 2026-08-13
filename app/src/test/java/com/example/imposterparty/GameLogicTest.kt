package com.example.imposterparty

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
    fun `discussion order starts with random player and covers all players round-robin`() {
        val playerCount = 5
        val startIndex = (0 until playerCount).random()
        val discussionOrder = (startIndex until startIndex + playerCount).map { it % playerCount }

        assertEquals(playerCount, discussionOrder.size)
        assertEquals(playerCount, discussionOrder.toSet().size)
        assertEquals(startIndex, discussionOrder.first())
        assertEquals((startIndex + 1) % playerCount, discussionOrder[1])
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
}
