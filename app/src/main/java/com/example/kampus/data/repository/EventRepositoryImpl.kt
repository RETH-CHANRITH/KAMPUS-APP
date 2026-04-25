package com.example.kampus.data.repository

import com.example.kampus.ui.events.EventItem
import com.example.kampus.ui.events.EventCategory
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class EventRepositoryImpl(private val firestore: FirebaseFirestore) {

    fun getEvents(): Flow<Result<List<EventItem>>> = callbackFlow {
        val listener = firestore.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val categoryStr = doc.getString("category") ?: "CAMPUS"
                        val category = try {
                            EventCategory.valueOf(categoryStr)
                        } catch (e: Exception) {
                            EventCategory.CAMPUS
                        }
                        
                        EventItem(
                            id = (doc.get("id") as? Number)?.toInt() ?: doc.id.hashCode(),
                            title = doc.getString("title") ?: "Untitled",
                            category = category,
                            date = doc.getString("date") ?: "",
                            time = doc.getString("time") ?: "",
                            location = doc.getString("location") ?: "",
                            interested = (doc.get("interested") as? Number)?.toInt() ?: 0,
                            likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                            comments = (doc.get("comments") as? Number)?.toInt() ?: 0,
                            shares = (doc.get("shares") as? Number)?.toInt() ?: 0,
                            coverEmoji = doc.getString("coverEmoji") ?: "🎉",
                            coverColor1 = Color(0xFF1A0A2E),
                            coverColor2 = Color(0xFF4A1060),
                            description = doc.getString("description") ?: "",
                            organizer = doc.getString("organizer") ?: "Unknown",
                            organizerEmoji = doc.getString("organizerEmoji") ?: "👤",
                            organizerTime = doc.getString("organizerTime") ?: "now"
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Result.success(events))
            }

        awaitClose { listener.remove() }
    }

    suspend fun createEvent(event: Map<String, Any>): Result<String> = try {
        val ref = firestore.collection("events").add(event).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateEvent(eventId: String, data: Map<String, Any>): Result<Unit> = try {
        firestore.collection("events").document(eventId).update(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = try {
        firestore.collection("events").document(eventId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getEventById(eventId: String): Flow<Result<EventItem?>> = callbackFlow {
        val listener = firestore.collection("events").document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val event = if (snapshot != null && snapshot.exists()) {
                    try {
                        val categoryStr = snapshot.getString("category") ?: "CAMPUS"
                        val category = try {
                            EventCategory.valueOf(categoryStr)
                        } catch (e: Exception) {
                            EventCategory.CAMPUS
                        }
                        
                        EventItem(
                            id = (snapshot.get("id") as? Number)?.toInt() ?: snapshot.id.hashCode(),
                            title = snapshot.getString("title") ?: "Untitled",
                            category = category,
                            date = snapshot.getString("date") ?: "",
                            time = snapshot.getString("time") ?: "",
                            location = snapshot.getString("location") ?: "",
                            interested = (snapshot.get("interested") as? Number)?.toInt() ?: 0,
                            likes = (snapshot.get("likes") as? Number)?.toInt() ?: 0,
                            comments = (snapshot.get("comments") as? Number)?.toInt() ?: 0,
                            shares = (snapshot.get("shares") as? Number)?.toInt() ?: 0,
                            coverEmoji = snapshot.getString("coverEmoji") ?: "🎉",
                            coverColor1 = Color(0xFF1A0A2E),
                            coverColor2 = Color(0xFF4A1060),
                            description = snapshot.getString("description") ?: "",
                            organizer = snapshot.getString("organizer") ?: "Unknown",
                            organizerEmoji = snapshot.getString("organizerEmoji") ?: "👤",
                            organizerTime = snapshot.getString("organizerTime") ?: "now"
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else null

                trySend(Result.success(event))
            }

        awaitClose { listener.remove() }
    }
}
