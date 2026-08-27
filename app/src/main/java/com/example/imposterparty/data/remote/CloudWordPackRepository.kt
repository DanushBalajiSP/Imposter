package com.example.imposterparty.data.remote

import com.example.imposterparty.data.local.LocalDataManager
import com.example.imposterparty.data.model.CommunityWordItem
import com.example.imposterparty.data.model.CommunityWordPack
import com.example.imposterparty.data.model.UserProfile
import com.example.imposterparty.data.model.WordEntry
import com.example.imposterparty.data.model.WordPack
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class CloudWordPackRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val communityCollection = firestore.collection("community_packs")

    /**
     * Publishes a local custom wordpack to the global Firestore community repository.
     */
    suspend fun publishWordPack(
        pack: WordPack,
        entries: List<WordEntry>,
        author: UserProfile,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (entries.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Cannot publish an empty wordpack."))
            }

            val packId = "pack_${UUID.randomUUID()}"
            val communityWords = entries.map {
                CommunityWordItem(word = it.word.trim(), clue = it.clue?.trim())
            }

            val communityPack = CommunityWordPack(
                id = packId,
                name = pack.name.trim(),
                authorId = author.userId,
                authorName = author.username,
                wordCount = communityWords.size,
                words = communityWords,
                publishedAt = System.currentTimeMillis(),
                downloadCount = 0,
            )

            communityCollection.document(packId)
                .set(communityPack)
                .await()

            Result.success(packId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Listens to all community wordpacks in real time, ordered by most recently published.
     */
    fun getCommunityPacks(): Flow<List<CommunityWordPack>> = callbackFlow {
        val subscription = communityCollection
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val packs = snapshot.toObjects(CommunityWordPack::class.java)
                    trySend(packs)
                }
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Imports a community word pack into local offline storage so it can be played anytime.
     * Also increments the pack's download count on Firestore.
     */
    suspend fun downloadCommunityPack(
        communityPack: CommunityWordPack,
        localDataManager: LocalDataManager,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entries = communityPack.words.map { Pair(it.word, it.clue) }
            val localPackName = communityPack.name.trim()

            // Save to local storage with clean name and author tag
            localDataManager.saveWordPack(
                name = localPackName,
                entries = entries,
                authorName = communityPack.authorName,
            )

            // Increment download count in Firestore
            try {
                communityCollection.document(communityPack.id)
                    .update("downloadCount", FieldValue.increment(1))
                    .await()
            } catch (e: Exception) {
                // Non-fatal if download count increment fails
                e.printStackTrace()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Deletes a published word pack from the community (only permitted for the author).
     */
    suspend fun deletePublishedPack(
        packId: String,
        authorId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = communityCollection.document(packId).get().await()
            if (!doc.exists()) {
                return@withContext Result.success(Unit)
            }
            val remoteAuthorId = doc.getString("authorId")
            if (remoteAuthorId != authorId) {
                return@withContext Result.failure(IllegalAccessException("You can only delete your own published word packs."))
            }

            communityCollection.document(packId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
