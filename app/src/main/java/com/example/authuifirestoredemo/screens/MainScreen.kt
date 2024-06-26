package com.example.authuifirestoredemo.screens

import android.app.Activity.RESULT_OK
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.authuifirestoredemo.NavRoutes
import com.google.firebase.auth.FirebaseUser

@Composable
fun MainScreen(
    navController: NavController
) {
    val viewModel: MainViewModel = viewModel()
    var showSignIn by remember { mutableStateOf(false) }
    val modifyShowSignIn = { value: Boolean -> showSignIn = value}
    val signInStatus by viewModel.signInStatus.collectAsStateWithLifecycle()
    val signedInUser by viewModel.signedInUser.collectAsStateWithLifecycle()

    MyColumn(
        navController = navController,
        signInStatus = signInStatus,
        signedInUser = signedInUser,
        modifyShowSignIn = modifyShowSignIn,
        viewModel = viewModel
    )


    // AuthUi signIn Activity call
    // --------------------------------
    if (showSignIn) {
        SignInScreen { result ->
            // (4) Handle the sign-in result callback
            // no need to handle RESULT_OK thanks to the AuthStateListener
            if (result.resultCode != RESULT_OK) {
                val response = result.idpResponse
                if (response == null) {
                    viewModel.onSignInCancel()
                } else {
                    val errorCode = response.error?.errorCode
                    viewModel.onSignInError(errorCode)
                }
            }
            showSignIn = false
        }
    }
}

@Composable
fun MyColumn(
    navController: NavController,
    signInStatus: String,
    signedInUser: FirebaseUser?,
    modifyShowSignIn: (Boolean) -> Unit,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // AUTHUI
        // ---------

        Spacer(Modifier.padding(2.dp))
        Text("Sign-in Status: $signInStatus")

        Spacer(Modifier.padding(2.dp))
        Text("User name: ${signedInUser?.displayName ?: ""}")
        Text("User Id: ${signedInUser?.uid ?: ""}")


        Spacer(Modifier.padding(2.dp))
        Button(onClick = {
            modifyShowSignIn(true)
        }) {
            Text("Sign In")
        }

        Spacer(Modifier.padding(2.dp))
        Button(onClick = {
            viewModel.onSignOut()
        }) {
            Text("Sign Out")
        }


        // FIRESTORE
        // ------------

        // Go to UserDataScreen
        // ------------------------
        Spacer(Modifier.padding(5.dp))
        Button(onClick = {
            navController.navigate(NavRoutes.Profile.route)
        }
        ) {
            Text(text = "Go to profile")
        }

        // Go to CitiesScreen
        // ------------------------
        Spacer(Modifier.padding(5.dp))
        Button(onClick = {
            navController.navigate(NavRoutes.Cities.route)
        }
        ) {
            Text(text = "Go to Cities")
        }
    }
}