package com.example.authuifirestoredemo.screens

import androidx.lifecycle.ViewModel
import com.example.authuifirestoredemo.tools.AuthManager
import com.google.firebase.auth.FirebaseAuth


class MainViewModel: ViewModel() {
    val signedInUser = AuthManager.signedInUser
    val signInStatus = AuthManager.signInStatus
    
    fun onSignOut() {
        FirebaseAuth.getInstance().signOut()
    }
    fun onSignInCancel() {
        onSignOut()
    }
    fun onSignInError(errorCode: Int?) {
        AuthManager.updateSignInStatus("Failed - Error Code: $errorCode")
    }
}