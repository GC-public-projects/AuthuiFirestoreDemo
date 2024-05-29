package com.example.authuifirestoredemo.data.repos.firestore

import android.content.ContentValues.TAG
import android.util.Log
import com.example.authuifirestoredemo.data.models.City
import com.example.authuifirestoredemo.data.repos.CityRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCityRepository(private val db: FirebaseFirestore): CityRepository {
    override suspend fun addCity(city: City) {
        db
            .collection("cities")

            // for custom ID
            //.document("${city.name}")
            //.set(city)

            // for auto ID
            .add(city)
    }

    override suspend fun fetchAllCities(): List<City> {
        val cities = mutableListOf<City>()
        val querySnapshot = db
            .collection("cities")
            .get()
            .await()

        for (document in querySnapshot.documents) {
            val city = document.toObject(City::class.java)
            city?.let {
                cities.add(city)
            }
        }
        return cities
    }

    override suspend fun fetchAllCitiesWithListener(): Flow<List<City>> = callbackFlow {
        val citiesCollection = db.collection("cities")

        val subscription = citiesCollection.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            // cities must be in the snapshotListener in order it gets update from DB
            val cities = mutableListOf<City>()

            querySnapshot?.documents?.forEach { document ->
                val city = document.toObject(City::class.java)
                city?.let {
                    cities.add(city)
                }
            }
            trySend(cities).isSuccess
        }

        awaitClose { subscription.remove() }
    }

    override suspend fun fetchAllCitiesAndIdWithListener(): Flow<List<Pair<City, String>>> = callbackFlow{
        val citiesCollection = db.collection("cities")

        val subscription = citiesCollection.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            // citiesAndId must be in the snapshotListener in order it gets update from DB
            val citiesAndId = mutableListOf<Pair<City, String>>()

            querySnapshot?.documents?.forEach { document ->
                val city = document.toObject(City::class.java)
                val id = document.id
                city?.let {
                    citiesAndId.add(Pair(city, id))
                }
            }
            trySend(citiesAndId).isSuccess

        }

        awaitClose { subscription.remove() }
    }

    override suspend fun deleteCity(cityId: String) {
        // do not delete the collections inside the document !
        db.collection("cities").document(cityId)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully deleted!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }
    }
}