package com.example.imposterparty.data.model

/**
 * Full game state tracked through the game loop.
 */
data class GameState(
    val phase: GamePhase = GamePhase.SETUP,
    val players: List<Player> = emptyList(),
    val settings: GameSettings = GameSettings(),
    val secretWord: String = "",
    val secretClue: String? = null,
    val categoryName: String = "",
    val currentRevealIndex: Int = 0,
    val revealOrder: List<Int> = emptyList(), // Sequential lobby player indices
    val startingSpeakerIndex: Int = 0, // Randomly selected person who starts the conversation
    val timerRemainingSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val currentVoterIndex: Int = 0,
    val votes: Map<Int, Int> = emptyMap(), // voterId -> votedForId
    val roundNumber: Int = 1,
    val actualImposterCount: Int = 0,
    val accusedPlayerId: Int? = null,
    val isImposterFound: Boolean = false,
    // ── Final Imposter & Sub-Round fields ──
    val subRoundNumber: Int = 0,
    val eliminatedPlayerIds: List<Int> = emptyList(),
    val remainingImposterId: Int? = null,
    val volunteerImposterId: Int? = null,
    val imposterGuessWord: String? = null,
    val wasWordGuessedCorrectly: Boolean? = null,
    val finalPhaseDescription: String? = null,
    val pendingRoundScores: Map<String, Int> = emptyMap(),
    val imposterSecuredPoints: Map<Int, Int> = emptyMap(), // imposterPlayerId -> points secured from surviving rounds
    val isSubRound: Boolean = false,
    val subRoundVoterIndices: List<Int> = emptyList(),
)

enum class GamePhase {
    SETUP,
    REVEALING,
    DISCUSSION,
    VOTING,
    FINAL_IMPOSTER_CHOICE, // Surviving imposter chooses: Guess Word or Play Sub-Round
    SUB_ROUND_DISCUSSION,  // Smaller final clue/discussion
    SUB_ROUND_VOTING,      // Smaller final voting
    RESULT,
}
