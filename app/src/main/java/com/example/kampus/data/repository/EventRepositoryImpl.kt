package com.example.kampus.data.repository

import android.net.Uri
import android.util.Log
import com.example.kampus.di.SupabaseModule
import com.example.kampus.domain.model.Event
import com.example.kampus.domain.repository.IEventRepository
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeOldRecordOrNull
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

private fun Event.cacheKey(): String = id ?: "${title}_${createdAt ?: ""}"

class EventRepositoryImpl(
    private val tableName: String = "events",
) : IEventRepository {

    private val supabase = SupabaseModule.getSupabaseClient()

    override suspend fun uploadEventImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            SupabaseModule.getStorageManager().uploadEventImage(userId, imageUri)
        } catch (e: Exception) {
            Log.e("EventRepositoryImpl", "uploadEventImage failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    @OptIn(SupabaseExperimental::class)
    override fun getEvents(): Flow<Result<List<Event>>> {
        return callbackFlow {
            val channel = supabase.channel("events-realtime")
            val cache = linkedMapOf<String, Event>()

            try {
                val initialEvents = supabase
                    .from(tableName)
                    .select()
                    .decodeList<Event>()

                initialEvents.forEach { record ->
                    cache[record.cacheKey()] = record
                }
                trySend(Result.success(cache.values.toList()))
            } catch (e: Exception) {
                trySend(Result.failure(e))
                close(e)
                return@callbackFlow
            }

            val collectJob = launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = tableName
                }.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> action.decodeRecordOrNull<Event>()?.let { record ->
                            cache[record.cacheKey()] = record
                            trySend(Result.success(cache.values.toList()))
                        }

                        is PostgresAction.Update -> action.decodeRecordOrNull<Event>()?.let { record ->
                            cache[record.cacheKey()] = record
                            trySend(Result.success(cache.values.toList()))
                        }

                        is PostgresAction.Delete -> action.decodeOldRecordOrNull<Event>()?.let { record ->
                            cache.remove(record.cacheKey())
                            trySend(Result.success(cache.values.toList()))
                        }

                        is PostgresAction.Select -> action.decodeRecordOrNull<Event>()?.let { record ->
                            cache[record.cacheKey()] = record
                            trySend(Result.success(cache.values.toList()))
                        }
                    }
                }
            }

            launch {
                channel.subscribe(true)
            }

            awaitClose {
                collectJob.cancel()
                launch {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }.catch { error ->
            emit(Result.failure(error))
        }
    }

    override suspend fun createEvent(event: Event): Result<Event> = try {
        Log.d("EventRepositoryImpl", "Inserting event into table=$tableName title=${event.title}")
        val inserted = supabase
            .from(tableName)
            .insert(event) { select() }
            .decodeSingle<Event>()

        Log.d("EventRepositoryImpl", "Insert returned id=${inserted.id}")
        Result.success(inserted)
    } catch (e: Exception) {
        Log.e("EventRepositoryImpl", "createEvent failed: ${e.message}", e)
        Result.failure(e)
    }

    override suspend fun updateEvent(eventId: String, event: Event): Result<Event> = try {
        val updated = supabase
            .from(tableName)
            .update(event) {
                select()
                filter {
                    eq("id", eventId)
                }
            }
            .decodeSingle<Event>()

        Result.success(updated)
    } catch (e: Exception) {
        Log.e("EventRepositoryImpl", "updateEvent failed: ${e.message}", e)
        Result.failure(e)
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = try {
        supabase
            .from(tableName)
            .delete {
                filter {
                    eq("id", eventId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("EventRepositoryImpl", "deleteEvent failed: ${e.message}", e)
        Result.failure(e)
    }

    @OptIn(SupabaseExperimental::class)
    override fun getEventById(eventId: String): Flow<Result<Event?>> {
        return flow {
            try {
                val event = supabase
                    .from(tableName)
                    .select {
                        filter {
                            eq("id", eventId)
                        }
                    }
                    .decodeList<Event>()
                    .firstOrNull()

                emit(Result.success(event))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }
}
