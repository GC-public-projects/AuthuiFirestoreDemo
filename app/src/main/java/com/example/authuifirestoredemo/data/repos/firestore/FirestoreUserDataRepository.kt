package com.example.authuifirestoredemo.data.repos.firestore

import android.util.Log
import com.example.authuifirestoredemo.data.models.UserData
import com.example.authuifirestoredemo.data.repos.UserDataRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserDataRepository {
    class FirestoreUserDataRepository(
        private val db: FirebaseFirestore
    ) : UserDataRepository {
        override suspend fun addOrUpdateUserData(
            firebaseUser: FirebaseUser,
            userData: UserData
        ) {
            // the .set function create or update a document if it already exists
            db.collection("userdata")
                .document(firebaseUser.uid)
                .set(userData)
        }

        override suspend fun fetchUserData(userId: String): UserData {
            return try {
                val querySnapShot = db.collection("userdata")
                    .document(userId)
                    .get()
                    .await()

                // if obect is null > return of an empty UserData object
                querySnapShot.toObject(UserData::class.java) ?: UserData()
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("FetchUserData", "Unauthorized access: ${e.message}")
                    UserData("Unauthorized access", 0)
                } else {
                    Log.e("FetchUserData", "Firestore error: ${e.message}")
                    UserData("Firestore error", 0)
                }
            }
        }

        override suspend fun fetchUserDataWithListener(userId: String): Flow<UserData> =
            callbackFlow {

                val userDataRef = db.collection("userdata").document(userId)

                val subscription = userDataRef.addSnapshotListener { documentSnapshot, exception ->
                    if (exception != null) {
                        close(exception)
                        return@addSnapshotListener
                    }

                    val userData = documentSnapshot?.toObject(UserData::class.java) ?: UserData()
                    trySend(userData).isSuccess

                }

                awaitClose { subscription.remove() }
            }
    }
}