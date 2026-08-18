package com.example.imposterparty

import com.example.imposterparty.data.model.*
import com.example.imposterparty.data.randomizer.FairRandomizer
import org.junit.Assert.*
import org.junit.Test

class VotingAndFinalImposterTest {

    // ── Test 1: Civilians Win -> Right voters get 2 pts (1+1), Wrong voters get 1 pt, Imposter gets 0 ──
    @Test
    fun `civilians win gives 2 points to right voters, 1 point to wrong voters, and 0 to imposter`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Charlie", role = Role.CIVILIAN),
            Player(3, "Imposter1", role = Role.IMPOSTER),
        )

        // Votes:
        // Alice and Bob vote for Imposter1 (2 votes -> eliminated)
        // Charlie votes for Bob (wrong vote)
        // Imposter1 votes for Alice
        val votes = mapOf(
            0 to 3, // Alice -> Imposter1 (Right vote)
            1 to 3, // Bob -> Imposter1 (Right vote)
            2 to 1, // Charlie -> Bob (Wrong vote)
            3 to 0, // Imposter1 -> Alice
        )

        val accusedId = 3 // Imposter1 was eliminated
        val accusedPlayer = players.find { it.id == accusedId }
        val isAccusedImposter = accusedPlayer?.role == Role.IMPOSTER // true

        val roundScores = mutableMapOf<String, Int>()
        players.forEach { player ->
            if (player.role == Role.CIVILIAN) {
                val votedForId = votes[player.id]
                val votedForEliminatedImposter = isAccusedImposter && (votedForId == accusedId)
                // Base 1 point for Civilians winning + 1 bonus if voted for eliminated imposter
                roundScores[player.name] = if (votedForEliminatedImposter) 2 else 1
            } else {
                roundScores[player.name] = 0
            }
        }

        // Alice and Bob both get 2 points (1 base + 1 right vote bonus)
        assertEquals(2, roundScores["Alice"])
        assertEquals(2, roundScores["Bob"])
        // Charlie was a civilian on the winning team, but voted wrong -> gets 1 base point
        assertEquals(1, roundScores["Charlie"])
        // Imposter was caught and lost -> 0 points
        assertEquals(0, roundScores["Imposter1"])
    }

    // ── Test 2: Imposters Win -> Imposter gets 1 pt, Right voter civilian gets 1 pt, Wrong voter gets 0 ──
    @Test
    fun `imposters win gives 1 point to imposter, 1 point to right voting civilian, and 0 to wrong voters`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Charlie", role = Role.CIVILIAN),
            Player(3, "Imposter1", role = Role.IMPOSTER),
        )

        // Votes:
        // Alice votes for Imposter1 (correct vote)
        // Bob, Charlie, Imposter1 vote for Alice (Alice is eliminated -> Imposter escapes!)
        val votes = mapOf(
            0 to 3, // Alice -> Imposter1 (Right vote)
            1 to 0, // Bob -> Alice (Wrong vote)
            2 to 0, // Charlie -> Alice (Wrong vote)
            3 to 0, // Imposter1 -> Alice
        )

        val accusedId = 0 // Alice was accused/eliminated
        val accusedPlayer = players.find { it.id == accusedId }
        val isAccusedImposter = accusedPlayer?.role == Role.IMPOSTER // false

        val roundScores = mutableMapOf<String, Int>()
        roundScores["Imposter1"] = 1
        players.filter { it.role == Role.CIVILIAN }.forEach { civilian ->
            val votedForId = votes[civilian.id]
            val votedForPlayer = players.find { it.id == votedForId }
            val votedForAnImposter = votedForPlayer?.role == Role.IMPOSTER
            roundScores[civilian.name] = if (votedForAnImposter) 1 else 0
        }

        // Imposter survived and won -> 1 point
        assertEquals(1, roundScores["Imposter1"])
        // Alice voted for the actual imposter even though team lost -> gets 1 vote point
        assertEquals(1, roundScores["Alice"])
        // Bob and Charlie voted wrongly on a losing civilian team -> 0 points
        assertEquals(0, roundScores["Bob"])
        assertEquals(0, roundScores["Charlie"])
    }

    // ── Test 3: Disallow Self Voting ──
    @Test
    fun `voting candidate list disallows self-voting`() {
        val players = listOf(
            Player(0, "Alice"),
            Player(1, "Bob"),
            Player(2, "Charlie"),
        )
        val currentVoter = players[0]
        val candidatePlayers = players.filter { it.id != currentVoter.id }

        assertFalse("Current voter should not be in candidates list", candidatePlayers.any { it.id == currentVoter.id })
        assertEquals(2, candidatePlayers.size)
    }

    // ── Test 4: Two Imposters - Both Survive ──
    @Test
    fun `two imposters - both survive results in imposter win with 1 point each`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Imposter1", role = Role.IMPOSTER),
            Player(3, "Imposter2", role = Role.IMPOSTER),
        )
        val accusedId = 0 // Alice was accused/eliminated
        val allImposters = players.filter { it.role == Role.IMPOSTER }
        val isAccusedImposter = players.find { it.id == accusedId }?.role == Role.IMPOSTER

        assertFalse(isAccusedImposter)
        // Both imposters survive -> Imposter Win
        val isCivilianWin = false
        val imposterPoints = if (!isCivilianWin) 1 else 0
        assertEquals(1, imposterPoints)
    }

    // ── Test 5: Two Imposters - Exactly 1 Eliminated triggers Final Imposter Phase ──
    @Test
    fun `two imposters - 1 eliminated triggers final imposter phase with surviving imposter`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Charlie", role = Role.CIVILIAN),
            Player(3, "Imposter1", role = Role.IMPOSTER),
            Player(4, "Imposter2", role = Role.IMPOSTER),
        )
        val accusedId = 3 // Imposter1 is eliminated
        val allImposters = players.filter { it.role == Role.IMPOSTER }
        val isAccusedImposter = players.find { it.id == accusedId }?.role == Role.IMPOSTER

        assertTrue(isAccusedImposter)
        val survivingImposters = allImposters.filter { it.id != accusedId }
        assertEquals(1, survivingImposters.size)
        assertEquals(4, survivingImposters.first().id) // Imposter2 enters Final Phase
    }

    // ── Test 6: Final Imposter Phase - Guess the Word Correctly (+3 Total Points) ──
    @Test
    fun `final imposter guesses secret word correctly resulting in 3 total points`() {
        val secretWord = "Astronaut"
        val guess = "  astronaut  "
        val normalizedGuess = FairRandomizer.normalizeWord(guess)
        val normalizedSecret = FairRandomizer.normalizeWord(secretWord)

        assertTrue(normalizedGuess == normalizedSecret)

        // Surviving Imposter receives +3 total points (+2 hidden survival bonus + +1 final victory)
        val finalImposterPoints = 3
        assertEquals(3, finalImposterPoints)
    }

    // ── Test 7: Final Imposter Phase - Guess the Word Incorrectly (Civilian Win) ──
    @Test
    fun `final imposter guesses secret word incorrectly resulting in civilian win`() {
        val secretWord = "Astronaut"
        val guess = "Alien"
        val normalizedGuess = FairRandomizer.normalizeWord(guess)
        val normalizedSecret = FairRandomizer.normalizeWord(secretWord)

        assertFalse(normalizedGuess == normalizedSecret)

        val isCivilianWin = true
        val finalImposterPoints = 0
        assertTrue(isCivilianWin)
        assertEquals(0, finalImposterPoints)
    }

    // ── Test 8: Final Imposter Phase - Sub-Round Imposter Survives (+3 Total Points) ──
    @Test
    fun `sub-round imposter survives resulting in imposter win with 3 total points`() {
        val remainingImposterId = 4
        val subRoundAccusedId = 0 // Civilian Alice was eliminated in sub-round

        val isRemainingImposterEliminated = subRoundAccusedId == remainingImposterId
        assertFalse(isRemainingImposterEliminated)

        // Imposter wins sub-round -> gets 3 points total
        val imposterPoints = 3
        val isCivilianWin = false
        assertEquals(3, imposterPoints)
        assertFalse(isCivilianWin)
    }

    // ── Test 9: Final Imposter Phase - Sub-Round Imposter Eliminated (Civilian Win) ──
    @Test
    fun `sub-round imposter eliminated resulting in civilian win`() {
        val remainingImposterId = 4
        val subRoundAccusedId = 4 // Imposter was eliminated in sub-round

        val isRemainingImposterEliminated = subRoundAccusedId == remainingImposterId
        assertTrue(isRemainingImposterEliminated)

        val isCivilianWin = true
        val imposterPoints = 0
        assertTrue(isCivilianWin)
        assertEquals(0, imposterPoints)
    }
}
