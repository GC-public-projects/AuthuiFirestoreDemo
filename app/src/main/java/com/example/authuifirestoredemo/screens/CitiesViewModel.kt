package com.example.authuifirestoredemo.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.authuifirestoredemo.data.models.City
import com.example.authuifirestoredemo.data.repos.CityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CitiesViewModel(
    private val cityRepository: CityRepository
) : ViewModel() {
    companion object {
        fun provideFactory(
            cityRepository: CityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
            ): T {
                if (modelClass.isAssignableFrom(CitiesViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return CitiesViewModel(cityRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    // FireStore properties purpose
    private val _citiesList = mutableStateListOf<City>()
    val citiesList: List<City> = _citiesList

    private val _citiesFlowWithListener = MutableStateFlow(emptyList<City>())
    val citiesFlowWithListener: StateFlow<List<City>> = _citiesFlowWithListener.asStateFlow()

    private val _citiesAndIdFlowWithListener = MutableStateFlow(emptyList<Pair<City, String>>())
    val citiesAndIdFlowWithListener: StateFlow<List<Pair<City, String>>> = _citiesAndIdFlowWithListener.asStateFlow()

    init {
        // get all cities
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // cites without listener affectation
                cityRepository.fetchAllCities().forEach {
                    _citiesList.add(it)
                }
            }
        }
        // get all cities with listener
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // flow collection of cities with listener
                cityRepository.fetchAllCitiesWithListener().collect {
                    _citiesFlowWithListener.value = it
                }
            }
        }
        // get all cities with listener and ID
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // flow collection of cities and ID with listener
                cityRepository.fetchAllCitiesAndIdWithListener().collect {
                    _citiesAndIdFlowWithListener.value = it
                }
            }
        }
    }
    // FireStore methods purpose
    fun createCityToDatabase(name: String, state: String, country: String) {
        val city = City(name, state, country, System.currentTimeMillis())
        // use of launch > no return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cityRepository.addCity(city)
            }
        }
    }
    fun deleteCity(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cityRepository.deleteCity(id)
            }
        }
    }
}