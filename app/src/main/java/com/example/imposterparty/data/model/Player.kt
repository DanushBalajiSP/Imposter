package com.example.imposterparty.data.model

/**
 * Represents a player in the game.
 */
data class Player(
    val id: Int,
    val name: String,
    val role: Role = Role.CIVILIAN,
    val score: Int = 0,
    val hasVoted: Boolean = false,
    val votedForId: Int? = null,
    val hasRevealed: Boolean = false,
)

enum class Role {
    CIVILIAN,
    IMPOSTER
}
