package com.example.imposterparty.data.model

/**
 * A word pack / category containing secret words.
 */
data class WordPack(
    val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean = false,
)

/**
 * A single word entry within a word pack, with an optional clue.
 */
data class WordEntry(
    val id: Long = 0,
    val packId: Long,
    val word: String,
    val clue: String? = null,
)
