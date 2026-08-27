package com.example.imposterparty.data.model

import com.google.firebase.firestore.PropertyName

/**
 * A word item in a community wordpack.
 */
data class CommunityWordItem(
    @get:PropertyName("word") @set:PropertyName("word") var word: String = "",
    @get:PropertyName("clue") @set:PropertyName("clue") var clue: String? = null,
)

/**
 * A published word pack stored in Firestore, shared globally with the community.
 * Identified by its unique ID and prominently displays the author's username.
 * Duplicate pack names are allowed across different authors.
 */
data class CommunityWordPack(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("authorId") @set:PropertyName("authorId") var authorId: String = "",
    @get:PropertyName("authorName") @set:PropertyName("authorName") var authorName: String = "",
    @get:PropertyName("wordCount") @set:PropertyName("wordCount") var wordCount: Int = 0,
    @get:PropertyName("words") @set:PropertyName("words") var words: List<CommunityWordItem> = emptyList(),
    @get:PropertyName("downloadCount") @set:PropertyName("downloadCount") var downloadCount: Int = 0,
    @get:PropertyName("publishedAt") @set:PropertyName("publishedAt") var publishedAt: Long = 0L,
)
