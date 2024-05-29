package com.example.authuifirestoredemo.data.repos

import com.example.authuifirestoredemo.data.models.City
import kotlinx.coroutines.flow.Flow

interface CityRepository {
    suspend fun addCity(city: City)
    suspend fun fetchAllCities(): List<City>
    suspend fun fetchAllCitiesWithListener(): Flow<List<City>>
    suspend fun fetchAllCitiesAndIdWithListener(): Flow<List<Pair<City, String>>>
    suspend fun deleteCity(cityId: String)
}