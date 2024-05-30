package com.example.authuifirestoredemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.authuifirestoredemo.data.repos.CityRepository
import com.example.authuifirestoredemo.data.repos.UserDataRepository
import com.example.authuifirestoredemo.data.repos.firestore.FirestoreCityRepository
import com.example.authuifirestoredemo.data.repos.firestore.FirestoreDB
import com.example.authuifirestoredemo.data.repos.firestore.FirestoreUserDataRepository
import com.example.authuifirestoredemo.screens.CitiesScreen
import com.example.authuifirestoredemo.screens.MainScreen
import com.example.authuifirestoredemo.screens.ProfileScreen
import com.example.authuifirestoredemo.ui.theme.AuthuiFirestoreDemoTheme

class MainActivity : ComponentActivity() {
    private val db = FirestoreDB.instance
    private lateinit var cityRepository: CityRepository
    private lateinit var userDataRepository: UserDataRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cityRepository = FirestoreCityRepository(db)
        userDataRepository = FirestoreUserDataRepository(db)

        enableEdgeToEdge()
        setContent {
            AuthuiFirestoreDemoTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.Home.route
                ) {
                    composable(NavRoutes.Home.route) {
                        MainScreen(
                            navController = navController,
                        )
                    }
                    composable(NavRoutes.Profile.route) {
                        ProfileScreen(
                            userDataRepository = userDataRepository
                        )
                    }
                    composable(NavRoutes.Cities.route) {
                        CitiesScreen(
                            cityRepository = cityRepository
                        )
                    }
                }
            }
        }
    }
}

