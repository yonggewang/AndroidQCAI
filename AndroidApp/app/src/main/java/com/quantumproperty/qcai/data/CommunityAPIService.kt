package com.quantumproperty.qcai.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.io.File

class CommunityAPIService {

    @Serializable
    private data class InsertEvent(
        val title: String,
        val description: String,
        val location: String,
        @SerialName("event_date") val eventDate: String,
        @SerialName("author_username") val authorUsername: String,
        @SerialName("author_id") val authorId: String,
        @SerialName("image_url") val imageUrl: String?
    )

    @Serializable
    private data class InsertItem(
        val title: String,
        val description: String,
        val price: Double,
        val condition: String,
        @SerialName("is_sold") val isSold: Boolean,
        @SerialName("author_username") val authorUsername: String,
        @SerialName("author_id") val authorId: String,
        @SerialName("image_url") val imageUrl: String?
    )

    @Serializable
    private data class InsertRental(
        val title: String,
        val description: String,
        val price: Double,
        val location: String,
        @SerialName("rental_type") val rentalType: String,
        @SerialName("contact_info") val contactInfo: String?,
        @SerialName("author_username") val authorUsername: String,
        @SerialName("author_id") val authorId: String,
        @SerialName("image_url") val imageUrl: String?
    )

    @Serializable
    private data class ContentReport(
        val reporter: String,
        @SerialName("reported_user") val reportedUser: String,
        val type: String,
        @SerialName("content_type") val contentType: String,
        @SerialName("content_id") val contentId: Int,
        val reason: String
    )

    private suspend fun getAuthorDetails(): Pair<String, String> {
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")
        val uid = user.id
        val profile = supabase.postgrest["users"]
            .select {
                filter {
                    eq("id", uid)
                }
            }
            .decodeSingle<UserProfile>()
        return Pair(uid, profile.username)
    }

    // MARK: - Events
    
    suspend fun fetchEvents(page: Int = 0): List<EventModel> = withContext(Dispatchers.IO) {
        supabase.postgrest["events"]
            .select {
                order("event_date", Order.ASCENDING)
                range(page * 20L, (page + 1) * 20L - 1)
            }
            .decodeList<EventModel>()
    }
    
    suspend fun createEvent(
        title: String,
        description: String,
        eventDate: String,
        location: String,
        imageUrl: String?
    ) = withContext(Dispatchers.IO) {
        val (uid, username) = getAuthorDetails()
        val newEvent = InsertEvent(
            title = title,
            description = description,
            eventDate = eventDate,
            location = location,
            authorUsername = username,
            authorId = uid,
            imageUrl = imageUrl
        )
        supabase.postgrest["events"].insert(newEvent)
    }
    
    suspend fun deleteEvent(id: Int) = withContext(Dispatchers.IO) {
        supabase.postgrest["events"]
            .delete {
                filter {
                    eq("id", id)
                }
            }
    }
    
    // MARK: - Marketplace
    
    suspend fun fetchMarketplaceItems(page: Int = 0): List<MarketplaceItemModel> = withContext(Dispatchers.IO) {
        supabase.postgrest["used_items"]
            .select {
                order("created_at", Order.DESCENDING)
                range(page * 20L, (page + 1) * 20L - 1)
            }
            .decodeList<MarketplaceItemModel>()
    }
    
    suspend fun createMarketplaceItem(
        title: String,
        description: String,
        price: Double,
        condition: String,
        imageUrl: String?
    ) = withContext(Dispatchers.IO) {
        val (uid, username) = getAuthorDetails()
        val newItem = InsertItem(
            title = title,
            description = description,
            price = price,
            condition = condition,
            isSold = false,
            authorUsername = username,
            authorId = uid,
            imageUrl = imageUrl
        )
        supabase.postgrest["used_items"].insert(newItem)
    }
    
    suspend fun deleteMarketplaceItem(id: Int) = withContext(Dispatchers.IO) {
        supabase.postgrest["used_items"]
            .delete {
                filter {
                    eq("id", id)
                }
            }
    }
    
    // MARK: - Rentals
    
    suspend fun fetchRentals(page: Int = 0, type: String? = null): List<RentalModel> = withContext(Dispatchers.IO) {
        supabase.postgrest["rentals"]
            .select {
                if (type != null) {
                    filter {
                        eq("rental_type", type)
                    }
                }
                order("created_at", Order.DESCENDING)
                range(page * 20L, (page + 1) * 20L - 1)
            }
            .decodeList<RentalModel>()
    }
    
    suspend fun createRental(
        title: String,
        description: String,
        price: Double,
        location: String,
        rentalType: String,
        contactInfo: String?,
        imageUrl: String?
    ) = withContext(Dispatchers.IO) {
        val (uid, username) = getAuthorDetails()
        val newRental = InsertRental(
            title = title,
            description = description,
            price = price,
            location = location,
            rentalType = rentalType,
            contactInfo = contactInfo,
            authorUsername = username,
            authorId = uid,
            imageUrl = imageUrl
        )
        supabase.postgrest["rentals"].insert(newRental)
    }
    
    suspend fun deleteRental(id: Int) = withContext(Dispatchers.IO) {
        supabase.postgrest["rentals"]
            .delete {
                filter {
                    eq("id", id)
                }
            }
    }
    
    // MARK: - Image Upload
    
    suspend fun uploadImage(imageFile: File): String = withContext(Dispatchers.IO) {
        val filename = "${java.util.UUID.randomUUID().toString().lowercase()}.jpg"
        val bytes = imageFile.readBytes()
        val bucket = supabase.storage.from("uploads")
        bucket.upload(filename, bytes)
        bucket.publicUrl(filename)
    }

    // MARK: - Content Reporting
    
    suspend fun reportContent(
        contentType: String,
        contentId: Int,
        authorUsername: String,
        reason: String = "Objectionable content"
    ) = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext
            val uid = user.id
            val profile = supabase.postgrest["users"]
                .select {
                    filter {
                        eq("id", uid)
                    }
                }
                .decodeSingle<UserProfile>()
            
            val report = ContentReport(
                reporter = profile.username,
                reportedUser = authorUsername,
                type = "report",
                contentType = contentType,
                contentId = contentId,
                reason = reason
            )
            
            supabase.postgrest["reports"].insert(report)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    companion object {
        val instance = CommunityAPIService()
    }
}
