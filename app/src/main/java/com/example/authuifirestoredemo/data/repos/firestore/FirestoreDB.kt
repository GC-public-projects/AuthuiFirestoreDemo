package com.example.authuifirestoredemo.data.repos.firestore

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

object FirestoreDB {
    val instance: FirebaseFirestore by lazy {
        Firebase.firestore
    }
}