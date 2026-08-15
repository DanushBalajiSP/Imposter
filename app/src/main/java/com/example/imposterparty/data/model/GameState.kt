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
)

enum class GamePhase {
    SETUP,
    REVEALING,
    DISCUSSION,
    VOTING,
    RESULT,
}
