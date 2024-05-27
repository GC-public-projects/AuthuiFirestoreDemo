package com.example.authuifirestoredemo.tools

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthManager : FirebaseAuth.AuthStateListener {
    private val _firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val _signedInUser = MutableStateFlow<FirebaseUser?>(null)
    val signedInUser = _signedInUser.asStateFlow()
    private val _signInStatus = MutableStateFlow("Not Signed-in")
    val signInStatus = _signInStatus.asStateFlow()

    init {
        _firebaseAuth.addAuthStateListener(this)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        _signedInUser.value = _firebaseAuth.currentUser
        _signInStatus.value = if (_signedInUser.value == null) "Not Signed In" else "Signed in"
    }

    fun updateSignInStatus(status: String) {
        _signInStatus.value = status
    }
}