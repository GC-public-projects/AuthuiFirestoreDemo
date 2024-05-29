package com.example.authuifirestoredemo

sealed class NavRoutes(val route: String) {
    data object Home: NavRoutes("home")
    data object Profile: NavRoutes("profile")
    data object Cities: NavRoutes("cities")
}