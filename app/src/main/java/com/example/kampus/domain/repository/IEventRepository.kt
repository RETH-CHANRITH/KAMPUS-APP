package com.example.kampus.domain.repository

import com.example.kampus.ui.events.EventItem
import kotlinx.coroutines.flow.Flow

interface IEventRepository {
	fun getEvents(): Flow<Result<List<EventItem>>>
	fun getEventById(eventId: String): Flow<Result<EventItem?>>
}
