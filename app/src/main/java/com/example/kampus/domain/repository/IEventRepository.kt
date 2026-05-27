package com.example.kampus.domain.repository

import android.net.Uri
import com.example.kampus.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface IEventRepository {
    suspend fun uploadEventImage(userId: String, imageUri: Uri): Result<String>

    fun getEvents(): Flow<Result<List<Event>>>

    fun getEventById(eventId: String): Flow<Result<Event?>>

    suspend fun createEvent(event: Event): Result<Event>

    suspend fun updateEvent(eventId: String, event: Event): Result<Event>

    suspend fun deleteEvent(eventId: String): Result<Unit>
}
