package com.example.imposterparty.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents an authenticated user profile in Imposter Party.
 * Authenticated purely via username + 4-digit PIN (no Google OAuth).
 */
data class UserProfile(
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username") var username: String = "",
    @get:PropertyName("usernameLower") @set:PropertyName("usernameLower") var usernameLower: String = "",
    @get:PropertyName("pinHash") @set:PropertyName("pinHash") var pinHash: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = 0L,
)
