package com.example.authuifirestoredemo.screens

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.authuifirestoredemo.data.models.UserData
import com.example.authuifirestoredemo.data.repos.UserDataRepository
import com.example.authuifirestoredemo.tools.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val userDataRepository: UserDataRepository
) : ViewModel() {
    companion object {
        fun provideFactory(
            userDataRepository: UserDataRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
            ): T {
                if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ProfileViewModel(userDataRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
    val signedInUser = AuthManager.signedInUser

    private val _userData = MutableStateFlow<UserData>(UserData())
    val userData = _userData.asStateFlow()

    private val _targetedUserData = mutableStateOf(UserData())
    val targetedUserData: State<UserData> = _targetedUserData


    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                signedInUser.value?.let { firebaseUser ->
                    userDataRepository.fetchUserDataWithListener(firebaseUser.uid).collect {
                        _userData.value = it
                    }
                }
            }
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _targetedUserData.value = userDataRepository.fetchUserData("yC9aI07rdEQjPurivYtplYPWdsF3") // TO MODIFY !!
            }
        }
    }

    fun createUserDataToDatabase(nickName: String, age: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                signedInUser.value?.let {
                    userDataRepository.addOrUpdateUserData(signedInUser.value!!, UserData(nickName, age))
                }
            }
        }
    }
}