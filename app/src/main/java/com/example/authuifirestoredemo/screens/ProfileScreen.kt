package com.example.authuifirestoredemo.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.authuifirestoredemo.data.models.UserData
import com.example.authuifirestoredemo.data.repos.UserDataRepository
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileScreen(
    userDataRepository: UserDataRepository,
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(userDataRepository)
    )
    val signedInUser by viewModel.signedInUser.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val targetedUserData by viewModel.targetedUserData
    var nickName by remember { mutableStateOf("") }
    val modifyNickName = { name: String -> nickName = name }
    var age by remember { mutableStateOf("") }
    val modifyAge = { value: String ->
        val onlyNumbers = value.filter { it.isDigit() }
        age = onlyNumbers
    }

    // targeted userData init (id of Test user)
    viewModel.getUserDataFromDb("yC9aI07rdEQjPurivYtplYPWdsF3") // TO MODIFY !!

    MyColumn(
        viewModel = viewModel,
        signedInUser = signedInUser ,
        userData = userData,
        targetedUserData = targetedUserData,
        nickName = nickName,
        modifyNickName = modifyNickName,
        age = age,
        modifyAge = modifyAge
    )
}

@Composable
fun MyColumn(
    viewModel: ProfileViewModel,
    signedInUser: FirebaseUser?,
    userData: UserData,
    targetedUserData: UserData,
    nickName: String,
    modifyNickName: (String) -> Unit,
    age: String,
    modifyAge: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // logged user Data
        // ----------------------
        Text(text = "Data of current user", textDecoration = TextDecoration.Underline)
        Spacer(Modifier.padding(2.dp))

        Text("User name: ${ signedInUser?.displayName ?: "not logged in"}")
        Text("User Id: ${ signedInUser?.uid ?: "not logged in"}")

        Spacer(modifier = Modifier.padding(5.dp))
        Text(text = "Nickname: ${ userData.nickname ?: "not logged in or setup yet" }")
        Text(text = "Age: ${ userData.age ?: "not logged in or setup yet" }")


        // create/update userdata
        // -------------------------
        Spacer(modifier = Modifier.padding(5.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Nickname :")
            TextField(value = nickName, onValueChange = modifyNickName)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Age :")
            TextField(value = age, onValueChange = modifyAge)
        }

        Button(onClick = {
            viewModel.createUserDataToDatabase(nickName = nickName, age = age.toInt())
        }) {
            Text(text = "Add/update user Data to DB")
        }

        // Show targeted user data (if allowed)
        // ---------------------------------------
        Divider(
            color = Color.Gray,
            thickness = 2.dp,
            modifier = Modifier.padding(vertical = 20.dp)
        )
        Text(text = "Data of user \"Test\"", textDecoration = TextDecoration.Underline)
        Spacer(Modifier.padding(2.dp))
        Text(text = "Nickname: ${ targetedUserData.nickname ?: "" }")
        Text(text = "Age: ${ targetedUserData.age ?: "" }")
    }
}