package com.quantumproperty.qcai.data

import com.google.gson.annotations.SerializedName

// Event Model
data class EventModel(
    val id: Int,
    val title: String,
    val description: String,
    val location: String,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("author_username") val authorUsername: String,
    @SerializedName("image_url") val imageUrl: String?
)

// Marketplace Item Model
data class MarketplaceItemModel(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val condition: String,
    @SerializedName("is_sold") val isSold: Boolean,
    @SerializedName("author_username") val authorUsername: String,
    @SerializedName("image_url") val imageUrl: String?
)

// Rental Model
data class RentalModel(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    @SerializedName("rental_type") val rentalType: String, // "offer" or "request"
    @SerializedName("contact_info") val contactInfo: String?,
    @SerializedName("author_username") val authorUsername: String,
    @SerializedName("image_url") val imageUrl: String?
)
