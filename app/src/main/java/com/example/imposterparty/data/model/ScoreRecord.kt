package com.example.imposterparty.data.model

/**
 * Persistent score record for a player across game sessions.
 */
data class ScoreRecord(
    val id: Long = 0,
    val playerName: String,
    val points: Int,
    val roundNumber: Int = 1,
    val sessionTimestamp: Long = System.currentTimeMillis(),
    val wasImposter: Boolean = false,
    val won: Boolean = false,
    val secretWord: String = "",
    val categoryName: String = "",
)

/**
 * Full game history record for a completed round.
 */
data class GameHistoryRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val roundNumber: Int = 1,
    val categoryName: String = "",
    val secretWord: String = "",
    val secretClue: String? = null,
    val imposterNames: List<String> = emptyList(),
    val civilianNames: List<String> = emptyList(),
    val accusedPlayerName: String? = null,
    val isImposterFound: Boolean = false,
    val scores: Map<String, Int> = emptyMap(),
)

/**
 * A match session spanning multiple rounds with cumulative scores.
 */
data class MatchSession(
    val id: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val totalRounds: Int = 0,
    val playerNames: List<String> = emptyList(),
    val cumulativeScores: Map<String, Int> = emptyMap(),
    val rounds: List<GameHistoryRecord> = emptyList(),
    val settings: GameSettings = GameSettings(),
)
