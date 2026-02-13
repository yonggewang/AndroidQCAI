package com.quantumproperty.qcai.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // Observe authentication state
    val authState: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Fetch extra data from Realtime Database
                database.child("users").child(user.uid).get()
                    .addOnSuccessListener { snapshot ->
                        val profile = snapshot.getValue(UserProfile::class.java)
                        // If profile exists in DB, emit it. Otherwise emit basic info.
                        if (profile != null) {
                            trySend(profile)
                        } else {
                            // First time or missing profile
                            val newProfile = UserProfile(
                                uid = user.uid,
                                email = user.email ?: "",
                                fullName = user.displayName ?: "",
                                vipLevel = 1
                            )
                            trySend(newProfile)
                        }
                    }
                    .addOnFailureListener {
                        // DB error, fallback to basics
                         val fallbackProfile = UserProfile(
                             uid = user.uid,
                             email = user.email ?: "",
                             vipLevel = 1
                         )
                         trySend(fallbackProfile)
                    }
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    suspend fun register(
        email: String, 
        pass: String, 
        fullName: String, 
        username: String, 
        phone: String
    ): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user
            if (user != null) {
                // Send verification email (Standard Firebase Flow)
                // Note: The user requested a "code", but Firebase native email auth sends a link.
                // We will send the link.
                user.sendEmailVerification()
                
                // Create profile in DB
                val profile = UserProfile(
                    uid = user.uid,
                    email = email,
                    username = username,
                    fullName = fullName,
                    phoneNumber = phone,
                    vipLevel = 1 // Registered user
                )
                
                database.child("users").child(user.uid).setValue(profile).await()
                Result.success("Registration successful. Please check your email for verification.")
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success("Login successful")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetPassword(email: String): Result<String> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success("Password reset email sent")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun logout() {
        auth.signOut()
    }
    
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val uid = user.uid
                // Delete from Realtime Database
                database.child("users").child(uid).removeValue().await()
                // Delete from Firebase Auth
                user.delete().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
