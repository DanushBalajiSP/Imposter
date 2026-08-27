package com.example.imposterparty.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.imposterparty.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class UserManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("imposter_user_prefs", Context.MODE_PRIVATE)

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        loadSavedUser()
    }

    private fun loadSavedUser() {
        val userId = prefs.getString("user_id", null)
        val username = prefs.getString("username", null)
        val usernameLower = prefs.getString("username_lower", null)
        val pinHash = prefs.getString("pin_hash", null)
        val createdAt = prefs.getLong("created_at", 0L)

        if (!userId.isNullOrBlank() && !username.isNullOrBlank() && !pinHash.isNullOrBlank()) {
            _currentUser.value = UserProfile(
                userId = userId,
                username = username,
                usernameLower = usernameLower ?: username.lowercase(),
                pinHash = pinHash,
                createdAt = createdAt,
            )
        }
    }

    private fun saveUserToLocal(profile: UserProfile) {
        prefs.edit()
            .putString("user_id", profile.userId)
            .putString("username", profile.username)
            .putString("username_lower", profile.usernameLower)
            .putString("pin_hash", profile.pinHash)
            .putLong("created_at", profile.createdAt)
            .apply()
        _currentUser.value = profile
    }

    private fun clearLocalUser() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private suspend fun ensureFirebaseAuth() {
        try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
        } catch (e: Exception) {
            // Non-fatal if offline or anonymous auth not enabled yet
            e.printStackTrace()
        }
    }

    /**
     * Hashes a 4-digit PIN using SHA-256 for secure verification.
     */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Creates a new user profile with a unique username and a 4-digit PIN.
     * Enforces username uniqueness in Firestore.
     */
    suspend fun createProfile(rawUsername: String, pin: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmed = rawUsername.trim()
        if (trimmed.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Username must be at least 3 characters long."))
        }
        if (trimmed.length > 20) {
            return@withContext Result.failure(IllegalArgumentException("Username must not exceed 20 characters."))
        }
        if (!trimmed.matches(Regex("^[a-zA-Z0-9_ ]+$"))) {
            return@withContext Result.failure(IllegalArgumentException("Username can only contain letters, numbers, spaces, and underscores."))
        }
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("PIN must be exactly 4 digits."))
        }

        val usernameLower = trimmed.lowercase()
        val pinHash = hashPin(pin)

        try {
            ensureFirebaseAuth()

            val usernameDocRef = firestore.collection("usernames").document(usernameLower)

            // Check if username is already taken via transaction
            val newProfile = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(usernameDocRef)
                if (snapshot.exists()) {
                    throw IllegalStateException("Username '$trimmed' is already taken. Please choose another username.")
                }

                val generatedUserId = "usr_" + UUID.randomUUID().toString().replace("-", "").take(16)
                val profile = UserProfile(
                    userId = generatedUserId,
                    username = trimmed,
                    usernameLower = usernameLower,
                    pinHash = pinHash,
                    createdAt = System.currentTimeMillis(),
                )

                val userDocRef = firestore.collection("users").document(generatedUserId)

                // 1. Claim username
                transaction.set(usernameDocRef, profile)
                // 2. Create user document
                transaction.set(userDocRef, profile)

                profile
            }.await()

            saveUserToLocal(newProfile)
            Result.success(newProfile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Logs into an existing profile using the username and 4-digit PIN.
     */
    suspend fun login(rawUsername: String, pin: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmed = rawUsername.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your username."))
        }
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("PIN must be exactly 4 digits."))
        }

        val usernameLower = trimmed.lowercase()
        val enteredPinHash = hashPin(pin)

        try {
            ensureFirebaseAuth()

            val usernameDocRef = firestore.collection("usernames").document(usernameLower)
            val snapshot = usernameDocRef.get().await()

            if (!snapshot.exists()) {
                return@withContext Result.failure(
                    NoSuchElementException("No account found with username '$trimmed'. Check the spelling or create a new profile.")
                )
            }

            val profile = snapshot.toObject(UserProfile::class.java)
                ?: return@withContext Result.failure(IllegalStateException("Failed to parse user profile."))

            if (profile.pinHash != enteredPinHash) {
                return@withContext Result.failure(
                    IllegalArgumentException("Incorrect 4-digit PIN for account '$trimmed'.")
                )
            }

            saveUserToLocal(profile)
            Result.success(profile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Logs out the current user on this device.
     */
    fun logout() {
        clearLocalUser()
    }
}
