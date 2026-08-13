package com.example.imposterparty.data.model

/**
 * Persistent score record for a player across game sessions.
 */
data class ScoreRecord(
    val id: Long = 0,
    val playerName: String,
    val points: Int,
    val roundNumber: Int,
    val sessionTimestamp: Long = System.currentTimeMillis(),
    val wasImposter: Boolean = false,
    val won: Boolean = false,
)
