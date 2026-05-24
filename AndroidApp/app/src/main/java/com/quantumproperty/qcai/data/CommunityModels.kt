package com.quantumproperty.qcai.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Event Model
@Serializable
data class EventModel(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("location") val location: String,
    @SerializedName("event_date") @SerialName("event_date") val eventDate: String,
    @SerializedName("author_username") @SerialName("author_username") val authorUsername: String,
    @SerializedName("author_id") @SerialName("author_id") val authorId: String = "",
    @SerializedName("image_url") @SerialName("image_url") val imageUrl: String? = null
)

// Marketplace Item Model
@Serializable
data class MarketplaceItemModel(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("price") val price: Double,
    @SerialName("condition") val condition: String,
    @SerializedName("is_sold") @SerialName("is_sold") val isSold: Boolean = false,
    @SerializedName("author_username") @SerialName("author_username") val authorUsername: String,
    @SerializedName("author_id") @SerialName("author_id") val authorId: String = "",
    @SerializedName("image_url") @SerialName("image_url") val imageUrl: String? = null
)

// Rental Model
@Serializable
data class RentalModel(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("price") val price: Double,
    @SerialName("location") val location: String,
    @SerializedName("rental_type") @SerialName("rental_type") val rentalType: String, // "offer" or "request"
    @SerializedName("contact_info") @SerialName("contact_info") val contactInfo: String? = null,
    @SerializedName("author_username") @SerialName("author_username") val authorUsername: String,
    @SerializedName("author_id") @SerialName("author_id") val authorId: String = "",
    @SerializedName("image_url") @SerialName("image_url") val imageUrl: String? = null
)
