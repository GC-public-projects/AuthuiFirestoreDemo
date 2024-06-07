package com.example.authuifirestoredemo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.authuifirestoredemo.data.models.City
import com.example.authuifirestoredemo.data.repos.CityRepository

@Composable
fun CitiesScreen(cityRepository: CityRepository) {

    val viewModel: CitiesViewModel = viewModel(
        factory = CitiesViewModel.provideFactory(cityRepository)
    )

    var cityName by remember { mutableStateOf("") }
    val modifyCityName = { name: String -> cityName = name }
    val citiesWithoutListener = viewModel.citiesList
    val citiesWithListener by viewModel.citiesFlowWithListener.collectAsState()
    val citiesAndIdWithListener by viewModel.citiesAndIdFlowWithListener.collectAsState()

    MyColumn(
        viewModel = viewModel,
        cityName = cityName,
        modifyCityName = modifyCityName,
        citiesWithoutListener = citiesWithoutListener,
        citiesWithListener = citiesWithListener,
        citiesAndIdWithListener = citiesAndIdWithListener
    )
}

@Composable
fun MyColumn(
    viewModel: CitiesViewModel,
    cityName: String,
    modifyCityName: (String) -> Unit,
    citiesWithoutListener: List<City>,
    citiesWithListener: List<City>,
    citiesAndIdWithListener: List<Pair<City, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        // Create City
        // --------------
        Spacer(Modifier.padding(40.dp))
        Text(text = "create city Name")
        TextField(value = cityName, onValueChange = modifyCityName)
        Button(onClick = {
            viewModel.createCityToDatabase(name = cityName, state = "default", country = "default")
        }) {
            Text("Add city to DB")
        }
        Spacer(Modifier.padding(2.dp))

        // Get cities without listener
        // ----------------------------
        Text(text = "Show all cities from the DB without listener")
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(citiesWithoutListener) { city ->
                CityListItem(city = city)
            }
        }

        // Get cities with listener
        // ----------------------------
        Text(text = "Show all cities from the DB with listener")
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(citiesWithListener) { city ->
                CityListItem(city = city)
            }
        }

        // Get cities and ID with listener
        // ----------------------------------
        Text(text = "Show all cities with listener and deletable")
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(citiesAndIdWithListener) { pair ->
                CityListItemDeletable(city = pair.first, id = pair.second, viewModel = viewModel)
            }
        }
    }
}
// City list item
@Composable
fun CityListItem(city: City) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { },
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = city.name ?: "blank")
            Text(text = city.country ?: "blank")
            Text(text = city.state ?: "blank")
        }
    }
}

// City list item deletable
@Composable
fun CityListItemDeletable(city: City, id: String, viewModel: CitiesViewModel) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = city.name ?: "blank")
                Text(text = city.country ?: "blank")
                Text(text = city.state ?: "blank")
            }
            IconButton(
                onClick = { viewModel.deleteCity(id) },
                content = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            )
        }
    }
}