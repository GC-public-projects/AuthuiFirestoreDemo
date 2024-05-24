package com.example.authuifirestoredemo.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel: ViewModel() {
    // AuthUI properties purpose
    private val _signInStatus = MutableStateFlow("Not Signed-in")
    val signInStatus = _signInStatus.asStateFlow()
    private val _signedInUser = MutableStateFlow<FirebaseUser?>(null)
    val signedInUser = _signedInUser.asStateFlow()

    init {
        getUser()
    }

    // AuthUi methods purpose

    private fun getUser() { // when app reopens
        // !! without "?" before ".let" the code is executed whatever the var is null or not
        FirebaseAuth.getInstance().currentUser?.let {
            _signInStatus.value = "Signed In"
            _signedInUser.value = FirebaseAuth.getInstance().currentUser
        }
    }
    fun onSignedIn() {
        getUser()
    }
    fun onSignOut() {
        FirebaseAuth.getInstance().signOut()
        _signInStatus.value = "Not Signed-in"
        _signedInUser.value = null
    }
    fun onSignInCancel() {
        onSignOut()
    }

    fun onSignInError(errorCode: Int?) {
        _signInStatus.value = "Failed - Error Code: $errorCode"
        _signedInUser.value = null
    }
}