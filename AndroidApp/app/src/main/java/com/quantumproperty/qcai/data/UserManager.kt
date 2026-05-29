package com.quantumproperty.qcai.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*

@Serializable
data class AppSecret(
    val key: String,
    val value: String
)

@Serializable
data class UserReport(
    val reporter: String,
    @SerialName("reported_user") val reportedUser: String,
    val type: String,
    val reason: String
)

class UserManager {

    // Observe authentication state
    val authState: Flow<UserProfile?> = callbackFlow {
        val job = launch {
            supabase.auth.sessionStatus.collectLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val uid = user?.id ?: ""
                        try {
                            var profile = supabase.postgrest["users"]
                                .select {
                                    filter {
                                        eq("id", uid)
                                    }
                                }
                                .decodeSingle<UserProfile>()
                            
                            // Populate email verification status
                            profile = profile.copy(isEmailVerified = user?.emailConfirmedAt != null)
                            
                            // Fetch and apply central API keys from secrets table
                            try {
                                loadAppSecrets()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            trySend(profile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Fallback to basics using user metadata
                            val fullName = user?.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: ""
                            val username = user?.userMetadata?.get("username")?.jsonPrimitive?.content ?: ""
                            val fallbackProfile = UserProfile(
                                uid = uid,
                                email = user?.email ?: "",
                                fullName = fullName,
                                username = username,
                                vipLevel = 1,
                                isEmailVerified = user?.emailConfirmedAt != null
                            )
                            trySend(fallbackProfile)
                        }
                    }
                    else -> {
                        trySend(null)
                    }
                }
            }
        }
        awaitClose { job.cancel() }
    }
    
    suspend fun loadAppSecrets() {
        try {
            val secrets = supabase.postgrest["app_secrets"]
                .select()
                .decodeList<AppSecret>()
                
            for (secret in secrets) {
                if (secret.key == "GEMINI_API_KEY") {
                    if (PreferenceManager.userGeminiKey.isEmpty()) {
                        PreferenceManager.supabaseGeminiKey = secret.value
                    } else {
                        PreferenceManager.supabaseGeminiKey = ""
                    }
                } else if (secret.key == "FINNHUB_API_KEY") {
                    PreferenceManager.finnhubKey = secret.value
                } else if (secret.key == "OPENAI_API_KEY") {
                    if (PreferenceManager.userOpenAIKey.isEmpty()) {
                        PreferenceManager.supabaseOpenAIKey = secret.value
                    } else {
                        PreferenceManager.supabaseOpenAIKey = ""
                    }
                } else if (secret.key == "PINECONE_API_KEY") {
                    PreferenceManager.pineconeKey = secret.value
                } else if (secret.key == "FMP_API_KEY") {
                    PreferenceManager.fmpKey = secret.value
                } else if (secret.key == "POLYGON_API_KEY") {
                    PreferenceManager.polygonKey = secret.value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun register(
        email: String, 
        pass: String, 
        fullName: String, 
        username: String, 
        phone: String
    ): Result<String> {
        return try {
            val userMetadata = buildJsonObject {
                put("full_name", fullName)
                put("username", username)
                put("phone_number", phone)
            }
            
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
                data = userMetadata
            }
            
            Result.success("Registration successful. Please check your email for verification.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            Result.success("Login successful")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetPassword(email: String): Result<String> {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            Result.success("Password reset email sent")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun logout() {
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch {
            try {
                supabase.auth.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                val uid = user.id
                // Delete user from public.users table
                supabase.postgrest["users"]
                    .delete {
                        filter {
                            eq("id", uid)
                        }
                    }
                // Sign out from auth session
                supabase.auth.signOut()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockUser(username: String): Result<Unit> {
        return try {
            val user = supabase.auth.currentUserOrNull() ?: return Result.failure(Exception("Not logged in"))
            val uid = user.id
            
            var profile = supabase.postgrest["users"]
                .select {
                    filter {
                        eq("id", uid)
                    }
                }
                .decodeSingle<UserProfile>()
                
            val blockedList = (profile.blockedUsers ?: emptyList()).toMutableList()
            if (!blockedList.contains(username)) {
                blockedList.add(username)
                profile = profile.copy(blockedUsers = blockedList)
                
                supabase.postgrest["users"]
                    .update(profile) {
                        filter {
                            eq("id", uid)
                        }
                    }
                    
                val report = UserReport(
                    reporter = profile.username,
                    reportedUser = username,
                    type = "block",
                    reason = "Blocked by reporter"
                )
                
                supabase.postgrest["reports"]
                    .insert(report)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
