package com.example.authuifirestoredemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.authuifirestoredemo.screens.MainScreen
import com.example.authuifirestoredemo.ui.theme.AuthuiFirestoreDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuthuiFirestoreDemoTheme {
//                val navController = rememberNavController()
//                NavHost(
//                    navController = navController,
//                    startDestination = NavRoutes.Home.route
//                ) {
//                    composable(NavRoutes.Home.route) {
//                        MainScreen(
//                            navController = navController,
//                        )
//                    }
//                    composable(NavRoutes.Profile.route) {
//                        ProfileScreen(
//                            userDataRepository = userDataRepository
//                        )
//                    }
//                    composable(NavRoutes.Cities.route) {
//                        CitiesScreen(
//                            cityRepository = cityRepository
//                        )
//                    }
//                }
                MainScreen()
            }
        }
    }
}

