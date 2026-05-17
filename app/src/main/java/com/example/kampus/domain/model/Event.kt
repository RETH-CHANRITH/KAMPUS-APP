package com.example.kampus.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    @SerialName("id")
    val id: String? = null,

    @SerialName("title")
    val title: String = "",

    @SerialName("description")
    val description: String? = null,

    @SerialName("location")
    val location: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("owner_id")
    val ownerId: String = "",

    @SerialName("allow_guest")
    val allowGuest: Boolean = false,

    @SerialName("start_date")
    val startDate: Long? = null,

    @SerialName("end_date")
    val endDate: Long? = null,

    @SerialName("created_at")
    val createdAt: Long? = null,

    // Extended fields for comprehensive event details
    @SerialName("event_type")
    val eventType: String? = null,

    @SerialName("capacity")
    val capacity: Int? = null,

    @SerialName("registration_deadline")
    val registrationDeadline: String? = null,

    @SerialName("website")
    val website: String? = null,

    @SerialName("online_event")
    val onlineEvent: Boolean = false,

    @SerialName("certificate_available")
    val certificateAvailable: Boolean = false,

    @SerialName("paid_event")
    val paidEvent: Boolean = false,

    @SerialName("speaker")
    val speaker: String? = null,

    @SerialName("tags")
    val tags: List<String>? = null,
)