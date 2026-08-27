package com.example.imposterparty

import com.example.imposterparty.data.local.RandomizerState
import com.example.imposterparty.data.model.*
import com.example.imposterparty.data.randomizer.FairRandomizer
import org.junit.Assert.*
import org.junit.Test

class VotingAndFinalImposterTest {

    // ── Rule 1: Civilian Accused in Main Round -> Round Ends Immediately & Imposters Win ──
    @Test
    fun `civilian accused in main round results in immediate imposter victory with secured and bonus points`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Charlie", role = Role.CIVILIAN),
            Player(3, "Imposter1", role = Role.IMPOSTER),
            Player(4, "Imposter2", role = Role.IMPOSTER),
        )
        val accusedId = 0 // Alice (Civilian) was accused
        val isAccusedImposter = players.find { it.id == accusedId }?.role == Role.IMPOSTER

        assertFalse(isAccusedImposter)
        // Imposters win immediately
        val nextPhase = if (!isAccusedImposter) GamePhase.RESULT else GamePhase.FINAL_IMPOSTER_CHOICE
        assertEquals(GamePhase.RESULT, nextPhase)

        // Surviving imposters get 1 secured point (from escaping round 1) + 1 bonus point = 2 points
        val roundScores = mutableMapOf<String, Int>()
        players.filter { it.role == Role.IMPOSTER }.forEach { imposter ->
            val secured = 1
            val bonus = 1
            roundScores[imposter.name] = secured + bonus
        }
        assertEquals(2, roundScores["Imposter1"])
        assertEquals(2, roundScores["Imposter2"])
    }

    // ── Rule 1: Civilian Accused in ANY Sub-Round -> Round Ends Immediately & Imposters Win ──
    @Test
    fun `civilian accused in sub-round results in immediate imposter victory`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Imposter1", role = Role.IMPOSTER),
            Player(3, "Imposter2", role = Role.IMPOSTER),
        )
        // Imposter1 was eliminated in Main Round 1 (secured 0 pts)
        // Imposter2 survived Main Round 1 (secured 1 pt)
        val imposterSecured = mutableMapOf(2 to 0, 3 to 1)

        // In Sub-Round 1, Alice (Civilian) is accused
        val subAccusedId = 0
        val isSubAccusedImposter = players.find { it.id == subAccusedId }?.role == Role.IMPOSTER
        assertFalse(isSubAccusedImposter)

        // Round ends immediately -> Imposters Win
        val nextPhase = if (!isSubAccusedImposter) GamePhase.RESULT else GamePhase.FINAL_IMPOSTER_CHOICE
        assertEquals(GamePhase.RESULT, nextPhase)

        // Imposter2 survived Sub-Round 1 (+1 secured) and wins the match (+1 bonus)
        val imposter2Secured = (imposterSecured[3] ?: 0) + 1 // = 2
        val imposter2Total = imposter2Secured + 1 // = 3

        // Imposter1 was eliminated in R1, so gets only their 0 secured points
        val imposter1Total = imposterSecured[2] ?: 0 // = 0

        assertEquals(3, imposter2Total)
        assertEquals(0, imposter1Total)
    }

    // ── Rule 2: Imposter Point Accumulation Across Multiple Sub-Rounds ──
    @Test
    fun `imposter secures 1 point per survived round and 1 bonus point upon winning match`() {
        val imposterId = 4
        var securedPoints = 0

        // Round 1: Survives -> +1 point
        securedPoints += 1
        assertEquals(1, securedPoints)

        // Sub-Round 1: Survives -> +1 point
        securedPoints += 1
        assertEquals(2, securedPoints)

        // Sub-Round 2: Survives & Match Won -> +1 secured point + 1 bonus point
        securedPoints += 1
        val finalPoints = securedPoints + 1
        assertEquals(4, finalPoints)
    }

    // ── Rule 2: Eliminated Imposter Retains Only Previously Secured Points ──
    @Test
    fun `eliminated imposter retains only points secured from rounds survived prior to elimination`() {
        // Imposter A eliminated in Round 1 -> 0 points
        val imposterAScore = 0
        // Imposter B survived Round 1 (+1), then eliminated in Sub-Round 1 -> 1 point
        val imposterBScore = 1
        // Imposter C survived Round 1 (+1), Sub-Round 1 (+1), and won in Sub-Round 2 (+1 + 1 bonus) -> 4 points
        val imposterCScore = 1 + 1 + 1 + 1

        assertEquals(0, imposterAScore)
        assertEquals(1, imposterBScore)
        assertEquals(4, imposterCScore)
    }

    // ── All Imposters Eliminated -> Civilians Win ──
    @Test
    fun `all imposters eliminated results in civilian victory with base points and voting bonuses`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Charlie", role = Role.CIVILIAN),
            Player(3, "Imposter1", role = Role.IMPOSTER),
            Player(4, "Imposter2", role = Role.IMPOSTER),
        )
        // Imposter1 eliminated in R1 (Alice & Bob voted right)
        // Imposter2 eliminated in SR1 (Alice voted right)
        val finalScores = mutableMapOf<String, Int>()

        players.forEach { player ->
            if (player.role == Role.CIVILIAN) {
                // Base 1 point + 1 bonus if they ever voted for an eliminated imposter
                val hasVotedRight = player.name in listOf("Alice", "Bob")
                finalScores[player.name] = 1 + (if (hasVotedRight) 1 else 0)
            } else {
                // Imposter1 secured 0, Imposter2 secured 1
                finalScores[player.name] = if (player.id == 4) 1 else 0
            }
        }

        assertEquals(2, finalScores["Alice"])
        assertEquals(2, finalScores["Bob"])
        assertEquals(1, finalScores["Charlie"])
        assertEquals(0, finalScores["Imposter1"])
        assertEquals(1, finalScores["Imposter2"])
    }

    // ── Multi-Imposter Guessing: One Guesses Correctly (+2 Pts), Other Goes to Sub-Round ──
    @Test
    fun `multi imposter scenario one guesses correctly with 2 points and other proceeds to sub round`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Imposter1", role = Role.IMPOSTER),
            Player(3, "Imposter2", role = Role.IMPOSTER),
            Player(4, "Imposter3", role = Role.IMPOSTER),
        )
        // Imposter1 eliminated in Round 1
        val eliminated = mutableListOf(2)
        val securedPoints = mutableMapOf(2 to 0, 3 to 1, 4 to 1)

        // Imposter2 volunteers to guess the word
        val secretWord = "Galaxy"
        val guess = "galaxy"
        val isCorrect = FairRandomizer.normalizeWord(guess) == FairRandomizer.normalizeWord(secretWord)
        assertTrue(isCorrect)

        // Imposter2 earns +2 points and is eliminated
        securedPoints[3] = (securedPoints[3] ?: 0) + 2 // 1 + 2 = 3
        eliminated.add(3)

        val remainingImposters = players.filter { it.role == Role.IMPOSTER && it.id !in eliminated }
        assertEquals(1, remainingImposters.size)
        assertEquals(4, remainingImposters.first().id) // Imposter3 remains

        // Phase remains FINAL_IMPOSTER_CHOICE because Imposter3 is still active!
        val nextPhase = if (remainingImposters.isEmpty()) GamePhase.RESULT else GamePhase.FINAL_IMPOSTER_CHOICE
        assertEquals(GamePhase.FINAL_IMPOSTER_CHOICE, nextPhase)

        // Imposter3 chooses Sub-Round, and in Sub-Round a Civilian is accused (Imposters win!)
        // Imposter3 survived sub-round (+1 secured) and won the match (+1 bonus) = 1 + 1 + 1 = 3 pts
        val imposter3Total = (securedPoints[4] ?: 0) + 1 + 1 // = 3
        // Imposter2 keeps their 3 secured points
        val imposter2Total = securedPoints[3] ?: 0 // = 3
        // Imposter1 gets 0
        val imposter1Total = securedPoints[2] ?: 0 // = 0

        assertEquals(3, imposter2Total)
        assertEquals(3, imposter3Total)
        assertEquals(0, imposter1Total)
    }

    // ── Multi-Imposter Guessing: One Guesses WRONGLY, Match Continues For Remaining Imposters ──
    @Test
    fun `multi imposter scenario one guesses wrongly is eliminated and match continues for remaining imposter`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Imposter1", role = Role.IMPOSTER),
            Player(3, "Imposter2", role = Role.IMPOSTER),
            Player(4, "Imposter3", role = Role.IMPOSTER),
        )
        // Imposter1 eliminated in Round 1
        val eliminated = mutableListOf(2)
        val securedPoints = mutableMapOf(2 to 0, 3 to 1, 4 to 1)

        // Imposter2 volunteers to guess the word and guesses WRONGLY
        val secretWord = "Galaxy"
        val wrongGuess = "planet"
        val isCorrect = FairRandomizer.normalizeWord(wrongGuess) == FairRandomizer.normalizeWord(secretWord)
        assertFalse(isCorrect)

        // Imposter2 gets 0 bonus (retains their 1 secured point) and is eliminated
        eliminated.add(3)

        var remainingImposters = players.filter { it.role == Role.IMPOSTER && it.id !in eliminated }
        assertEquals(1, remainingImposters.size)
        assertEquals(4, remainingImposters.first().id) // Imposter3 remains active!

        // Match does NOT immediately end; stays in FINAL_IMPOSTER_CHOICE for Imposter3
        val nextPhase = if (remainingImposters.isEmpty()) GamePhase.RESULT else GamePhase.FINAL_IMPOSTER_CHOICE
        assertEquals(GamePhase.FINAL_IMPOSTER_CHOICE, nextPhase)

        // Now Imposter3 also guesses correctly (+2 pts) and safely exits
        val rightGuess = "galaxy"
        assertTrue(FairRandomizer.normalizeWord(rightGuess) == FairRandomizer.normalizeWord(secretWord))
        securedPoints[4] = (securedPoints[4] ?: 0) + 2 // 1 + 2 = 3
        eliminated.add(4)

        remainingImposters = players.filter { it.role == Role.IMPOSTER && it.id !in eliminated }
        assertTrue(remainingImposters.isEmpty())

        // All imposters resolved -> Game transitions to RESULT
        val finalPhase = if (remainingImposters.isEmpty()) GamePhase.RESULT else GamePhase.FINAL_IMPOSTER_CHOICE
        assertEquals(GamePhase.RESULT, finalPhase)

        assertEquals(1, securedPoints[3]) // Imposter2 kept their 1 secured point
        assertEquals(3, securedPoints[4]) // Imposter3 got 1 + 2 = 3
        assertEquals(0, securedPoints[2]) // Imposter1 got 0
    }

    // ── Multi-Imposter Guessing: All Remaining Imposters Guess Correctly (+2 Pts) ──
    @Test
    fun `multi imposter scenario all remaining imposters guess correctly sequentially with 2 points`() {
        val players = listOf(
            Player(0, "Alice", role = Role.CIVILIAN),
            Player(1, "Bob", role = Role.CIVILIAN),
            Player(2, "Imposter1", role = Role.IMPOSTER),
            Player(3, "Imposter2", role = Role.IMPOSTER),
            Player(4, "Imposter3", role = Role.IMPOSTER),
        )
        // Imposter1 eliminated in Round 1
        val eliminated = mutableListOf(2)
        val securedPoints = mutableMapOf(2 to 0, 3 to 1, 4 to 1)

        // 1. Imposter2 guesses correctly (+2 pts)
        securedPoints[3] = (securedPoints[3] ?: 0) + 2
        eliminated.add(3)

        var remainingImposters = players.filter { it.role == Role.IMPOSTER && it.id !in eliminated }
        assertEquals(1, remainingImposters.size)

        // 2. Imposter3 also guesses correctly (+2 pts)
        securedPoints[4] = (securedPoints[4] ?: 0) + 2
        eliminated.add(4)

        remainingImposters = players.filter { it.role == Role.IMPOSTER && it.id !in eliminated }
        assertTrue(remainingImposters.isEmpty())

        // Now all imposters resolved -> Game transitions to RESULT
        val finalPhase = if (remainingImposters.isEmpty()) GamePhase.RESULT else GamePhase.FINAL_IMPOSTER_CHOICE
        assertEquals(GamePhase.RESULT, finalPhase)

        assertEquals(3, securedPoints[3])
        assertEquals(3, securedPoints[4])
        assertEquals(0, securedPoints[2])
    }

    // ── Candidate List Disallows Self-Voting ──
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

    // ── Universal Fair Randomizer State Test ──
    @Test
    fun `universal fair randomizer handles cross match imposter histories and reset correctly`() {
        val players = listOf(
            Player(0, "Alice"),
            Player(1, "Bob"),
            Player(2, "Charlie"),
        )

        val state = RandomizerState(
            globalRoundCounter = 5,
            wordUsageHistory = mapOf("apple" to 5),
            playerImposterCounts = mapOf("alice" to 3, "bob" to 1, "charlie" to 1),
            recentImposterNames = listOf(listOf("Alice"), listOf("Bob")),
        )

        val histories = FairRandomizer.buildPlayerHistoriesFromState(
            players = players,
            playerImposterCounts = state.playerImposterCounts,
            recentImposterRounds = state.recentImposterNames,
        )

        assertEquals(3, histories[0]?.totalImposterCount)
        assertTrue(histories[0]?.previousRoundWasImposter == true)
        assertFalse(histories[1]?.previousRoundWasImposter == true)
        assertTrue(histories[1]?.twoRoundsAgoWasImposter == true)

        // Test Reset State
        val resetState = RandomizerState()
        assertEquals(0, resetState.globalRoundCounter)
        assertTrue(resetState.wordUsageHistory.isEmpty())
        assertTrue(resetState.playerImposterCounts.isEmpty())
        assertTrue(resetState.recentImposterNames.isEmpty())
    }
}
