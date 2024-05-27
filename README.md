# AuthuiFirestoreDemo
mini project that mix Authui and Firestore

# Init

- create FireBase project in https://console.firebase.google.com/ and enable Google Analytics if needed
- create Jetpack Compose project in Android Studio


## Firebase Auth
- enable in Firebase site > projects > build > Authentication
- in Settings > User actions > untick "Email Enumeration protection" (in order to login with existing account with Authui)


## Firestore
- enable in Firebase site > projects > build > Firestore Database
- create database
- choose db location (irreversible)
- start in test mode (will be modifyed later)

## Link app to project with dependencies
- again in Firebase site go to settings (gear icon next to "project Overview")
- in General > your apps 
- click on Android Icon
- insert app package name : "com.example.authuifirestoredemo"
- Register app
- download Google-services.json and paste it in "App" folder of the project
- follow the procedure to add the dependencies in the project (Firebase SDK)  
in "root level "build.gradle.kts 
``` kotlin
plugins {
	...
	id("com.google.gms.google-services") version "4.4.1" apply false

}
```
in "app level" build.gradle.kts 
``` kotlin
plugins {
	...
	id("com.google.gms.google-services")

}


dependencies {
	...

 	// Import the Firebase BoM

	implementation(platform("com.google.firebase:firebase-bom:33.0.0"))


	// When using the BoM, don't specify versions in Firebase dependencies

	implementation("com.google.firebase:firebase-analytics")

}

```

## AuthUI dependencies
in "app level" build.gradle.kts 
``` kotlin
dependencies {dependencies {
	...
	implementation ("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation("com.google.android.gms:play-services-auth:20.7.0") // ! v21 not compatible with AuthUi (2024/05)
}
```

## ViewModel and compose lifecycle dependencies
As all androidx.lifecycle:lifecycle v2.8 are not compatible for the moment, in addition to add the 2 dependecies in 2.7, "androidx.lifecycle.runtime.ktx" must be changed to 2.7 too ! (with Android Studio Jellyfish, go to libs.versions.toml to change it).  
in "app level" build.gradle.kts
```
	...
	// ! all androidx.lifecycle:lifecycle v2.8 not compatible (2024/05)
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0") 
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
}
```

# AuthUI implementation

## 1. Authui SignIn screen composable
This composable take an onSignInresult function as param taht will be executed later by rememberLauncherForActivityResult. We will pass the content of the function during the call of the composable later.  
  
### Purpose 
- setup the authentication providers (Mail, google, Github, etc...) (Just "mail" is setup)
- create the Intent of the sign-in/Sign-up and launch the AuthUI Activity
- thanks to the function as param, the result of the tasks done in the activity will be handled later.

### function content
- create package "screens" in the main package (the one of the MainActivity)
- create Kotlin class/file inside with the content :
- import the dependencies
``` kotlin
@Composable
fun SignInScreen(onSignInResult: (FirebaseAuthUIAuthenticationResult) -> Unit) {
    // (1) Create ActivityResultLauncher
    val launcher = rememberLauncherForActivityResult(
        contract = FirebaseAuthUIActivityResultContract(),
        onResult = onSignInResult
    )
    // (2) Choose authentication providers
    val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build(),
    )

    // (3) Create and launch sign-in intent
    val intent = AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers)
        .build()

    LaunchedEffect(true) {
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("SignInScreen", "Error launching sign-in intent: ${e.message}")
        }
    }
}
```
## 2. AuthManager
Singleton (object) taht implements FirebaseAuth.AuthStateListener 

### Purpose
The object contents as flow the signed in user (FirebasUser) from FirebaseAuth.getInstance() and the signiedInStatus crated by us.  
By implementing the AuthStateListener we ensure we have the status and the data in real time of the Firebase user. The singleton will let us get the Auth data in many places without needing to use the dependency injection.XXXXXX??? or better with XXXXXXXXXXXXXXXXXXXX

### Components explanations


### object content
``` kotlin
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
```


## 3. MainViewModel
Viewmodel attached to the main screen


### Class creation
```kotlin
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
```

## 4. MainScreen & MyColumn composables

### Purpose 
- show the SIgnin status and some info of the Signed in user
- call of SignInScreen composable via Button

### Components explanations
- both collectAsStateWithLifecycle() and collectAsState() can be used, collectAsStateWithLifecycle() is better because it stop collecting when the composable is not active.
- use of "MyColumn" in order to make the composables steless
- showSignIn boolean flag obligatory because the Onclick from a button cannot contains a composable. So the composable SIgnInScreen is displayed after the flag becomes true

### functions content
``` kotlin
@Composable
fun MainScreen() {
    val viewModel: MainViewModel = viewModel()
    var showSignIn by remember { mutableStateOf(false) }
    val modifyShowSignIn = { value: Boolean -> showSignIn = value }
    val signInStatus by viewModel.signInStatus.collectAsStateWithLifecycle()
    val signedInUser by viewModel.signedInUser.collectAsStateWithLifecycle()

    MyColumn(
        signInStatus = signInStatus,
        signedInUser = signedInUser,
        modifyShowSignIn = modifyShowSignIn,
        viewModel = viewModel
    )


    // AuthUi signIn Activity call
    // --------------------------------
    if (showSignIn) {
        SignInScreen { result ->
            // (4) Handle the sign-in result callback if not OK
            // no need to handle when RESULT_OK thanks to the AuthListener
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
    signInStatus: String,
    signedInUser: FirebaseUser?,
    modifyShowSignIn: (Boolean) -> Unit,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text("Sign-in Status: $signInStatus")
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
    }
}
```



## MainActivity composable
For the moment, the purpose is just to call MainScreen()  

### Class creation
``` kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuthuiFirestoreDemoTheme {
                MainScreen()
            }
        }
    }
}
```


