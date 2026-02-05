package com.quantumproperty.qcai.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class CommunityAPIService {
    private val baseUrl = "https://cyberpandaapp.com"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    private suspend fun getAuthToken(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw Exception("Not logged in")
        return user.getIdToken(false).await().token
            ?: throw Exception("Failed to get auth token")
    }

    // MARK: - Events
    
    suspend fun fetchEvents(page: Int = 0): List<EventModel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/events/?skip=${page * 20}&limit=20"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch events: ${response.code}")
        }
        
        val json = response.body?.string() ?: "[]"
        gson.fromJson(json, Array<EventModel>::class.java).toList()
    }
    
    suspend fun createEvent(
        title: String,
        description: String,
        eventDate: String,
        location: String,
        imageUrl: String?
    ) = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        val url = "$baseUrl/events/"
        
        val jsonBody = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("event_date", eventDate)
            put("location", location)
            if (imageUrl != null) put("image_url", imageUrl)
        }
        
        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to create event: ${response.code}")
        }
    }
    
    suspend fun deleteEvent(id: Int) = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        val url = "$baseUrl/events/$id"
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to delete event: ${response.code}")
        }
    }
    
    // MARK: - Marketplace
    
    suspend fun fetchMarketplaceItems(page: Int = 0): List<MarketplaceItemModel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/marketplace/?skip=${page * 20}&limit=20"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch marketplace items: ${response.code}")
        }
        
        val json = response.body?.string() ?: "[]"
        gson.fromJson(json, Array<MarketplaceItemModel>::class.java).toList()
    }
    
    suspend fun createMarketplaceItem(
        title: String,
        description: String,
        price: Double,
        condition: String,
        imageUrl: String?
    ) = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        val url = "$baseUrl/marketplace/"
        
        val jsonBody = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("price", price)
            put("condition", condition)
            put("is_sold", false)
            if (imageUrl != null) put("image_url", imageUrl)
        }
        
        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to create marketplace item: ${response.code}")
        }
    }
    
    suspend fun deleteMarketplaceItem(id: Int) = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        val url = "$baseUrl/marketplace/$id"
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to delete marketplace item: ${response.code}")
        }
    }
    
    // MARK: - Rentals
    
    suspend fun fetchRentals(page: Int = 0, type: String? = null): List<RentalModel> = withContext(Dispatchers.IO) {
        var url = "$baseUrl/rentals/?skip=${page * 20}&limit=20"
        if (type != null) {
            url += "&rental_type=$type"
        }
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch rentals: ${response.code}")
        }
        
        val json = response.body?.string() ?: "[]"
        gson.fromJson(json, Array<RentalModel>::class.java).toList()
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
        val token = getAuthToken()
        val url = "$baseUrl/rentals/"
        
        val jsonBody = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("price", price)
            put("location", location)
            put("rental_type", rentalType)
            if (contactInfo != null) put("contact_info", contactInfo)
            if (imageUrl != null) put("image_url", imageUrl)
        }
        
        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to create rental: ${response.code}")
        }
    }
    
    suspend fun deleteRental(id: Int) = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        val url = "$baseUrl/rentals/$id"
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to delete rental: ${response.code}")
        }
    }
    
    // MARK: - Image Upload
    
    suspend fun uploadImage(imageFile: File): String = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        val url = "$baseUrl/upload/image"
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorText = response.body?.string() ?: "Unknown error"
            throw Exception("Upload failed: $errorText")
        }
        
        val json = response.body?.string() ?: throw Exception("Empty response")
        val uploadResponse = gson.fromJson(json, UploadResponse::class.java)
        "$baseUrl${uploadResponse.url}"
    }
    
    private data class UploadResponse(val url: String)
    
    companion object {
        val instance = CommunityAPIService()
    }
}
