package com.example.authuifirestoredemo.data.repos

import com.example.authuifirestoredemo.data.models.UserData
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    suspend fun addOrUpdateUserData(firebaseUser: FirebaseUser, userData: UserData)
    suspend fun fetchUserData(userId: String): UserData
    suspend fun fetchUserDataWithListener(userId: String): Flow<UserData>
}