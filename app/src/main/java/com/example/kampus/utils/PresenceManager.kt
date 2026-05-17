package com.example.kampus.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object PresenceManager {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun markOnline(userId: String) {
        if (userId.isBlank()) return

        firestore.collection("presence").document(userId)
            .set(
                mapOf(
                    "userId" to userId,
                    "isOnline" to true,
                    "updatedAt" to System.currentTimeMillis(),
                    "lastSeenAt" to null,
                )
            )
            .await()
    }

    suspend fun markOffline(userId: String, lastSeenAt: Long = System.currentTimeMillis()) {
        if (userId.isBlank()) return

        firestore.collection("presence").document(userId)
            .set(
                mapOf(
                    "userId" to userId,
                    "isOnline" to false,
                    "updatedAt" to System.currentTimeMillis(),
                    "lastSeenAt" to lastSeenAt,
                )
            )
            .await()
    }
}