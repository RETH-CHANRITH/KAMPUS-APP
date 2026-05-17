package com.example.kampus.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FcmTokenManager {
    private const val PREFS_NAME = "kampus_fcm_prefs"
    private const val KEY_PENDING_TOKEN = "pending_fcm_token"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    fun cachePendingToken(context: Context, token: String) {
        if (token.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_TOKEN, token)
            .apply()
    }

    fun peekPendingToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PENDING_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun clearPendingToken(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_TOKEN)
            .apply()
    }

    suspend fun syncCurrentDeviceToken(context: Context, userId: String? = auth.currentUser?.uid): Result<Unit> {
        val resolvedUserId = userId?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        return withContext(Dispatchers.IO) {
            val token = runCatching { FirebaseMessaging.getInstance().token.await() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: peekPendingToken(context)

            if (token.isNullOrBlank()) return@withContext Result.success(Unit)

            runCatching {
                val tokenData = mapOf(
                    "token" to token,
                    "platform" to "android",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastSyncedAt" to FieldValue.serverTimestamp(),
                )

                firestore.collection("users")
                    .document(resolvedUserId)
                    .collection("fcmTokens")
                    .document(token)
                    .set(tokenData, SetOptions.merge())
                    .await()

                clearPendingToken(context)
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(it) },
            )
        }
    }

    suspend fun flushPendingToken(context: Context, userId: String? = auth.currentUser?.uid): Result<Unit> {
        val token = peekPendingToken(context) ?: return Result.success(Unit)
        val resolvedUserId = userId?.takeIf { it.isNotBlank() }
            ?: return Result.success(Unit)

        return withContext(Dispatchers.IO) {
            runCatching {
                val tokenData = mapOf(
                    "token" to token,
                    "platform" to "android",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastSyncedAt" to FieldValue.serverTimestamp(),
                )

                firestore.collection("users")
                    .document(resolvedUserId)
                    .collection("fcmTokens")
                    .document(token)
                    .set(tokenData, SetOptions.merge())
                    .await()

                clearPendingToken(context)
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(it) },
            )
        }
    }
}
