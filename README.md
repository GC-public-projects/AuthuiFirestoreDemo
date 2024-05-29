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
- get your debug sign in certificates : 
in terminal run : 
``` bash
keytool -list -v \
-alias androiddebugkey -keystore ~/.android/debug.keystore
```
the pwd is "android"  
in "SHA certificates fingerprints section, tape on "Add fingerprint" to add the SHA-256 certificate
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
dependencies {
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
## Firestore dependencies
``` kotlin
dependencies {
	...
	implementation("com.google.firebase:firebase-firestore:25.0.0")
}
```

## Navigation dependencies
```kotlin
dependencies {
	...
	implementation("androidx.navigation:navigation-compose:2.7.7")
}
```

# AuthUI implementation

## 1. Authui SignIn screen (composable)
This composable take an onSignInresult function as param taht will be executed later by rememberLauncherForActivityResult. We will pass the content of the function during the call of the composable later.  
  
### Purpose 
- setup the authentication providers (Mail, google, Github, etc...) (Just "mail" is setup)
- create the Intent of the sign-in/Sign-up and launch the AuthUI Activity
- thanks to the function as param, the result of the tasks done in the activity will be handled later.

### function content
- create package "screens" in the main package (the one of the MainActivity)
- create Kotlin class/file inside with the content :
- import the dependencies (to do all the time for each content)
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
## 2. AuthManager (object)
Singleton (object) that implements FirebaseAuth.AuthStateListener 

### Purpose
The object contents as flow the signed in user (FirebasUser) from FirebaseAuth.getInstance() and the signiedInStatus crated by us.  
By implementing the AuthStateListener we ensure we have the status and the data in real time of the Firebase user. The singleton will let us get the Auth data in many places without needing to use the dependency injection as it will be needed in the most of the composables.

### Components explanations
- FirebaseAuth.AuthStateListener : this interface can be implemented by an activity or a viewModel. In our case as the main of the composables will need the FireBaseAuth, it is better to create a singleton and call it when needed.
- _firebaseAuth.addAuthStateListener(this) : The AuthStateListener oberves the changes in real time of the auth, thanks to it, we don't need to test the auth many times each time we need it.
- signiedInUser and signInstates : are both stateFlow, convenient to get data changes in real time thank to a listener here.
-  override fun onAuthStateChanged : it's the only function obligatory implemented by the interface. It let us make some actions when the FireBaseAuth is modified

### object content
- create package "tool" in the main package
- create Kotlin class/file named "AuthManager" inside with the content :
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


## 3. MainViewModel (class)
Viewmodel attached to the main screen

### Purpose
Call the Auth Manager


### Class content
- in package "screens"
- create Kotlin class/file named "MainViewModel" inside with the content :
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

## 4. MainScreen & MyColumn (composables)

### Purpose 
- show the Signin status and some info of the Signed in user
- call of SignInScreen composable via Button

### Components explanations
- both collectAsStateWithLifecycle() and collectAsState() can be used, collectAsStateWithLifecycle() is better because it stop collecting the flow when the composable is not active.
- use of "MyColumn" in order to make the composables stateless
- showSignIn boolean flag obligatory because the Onclick from a button cannot contains a composable. So the composable SIgnInScreen is displayed after the flag becomes true

### functions content
- in package "screens"
- create Kotlin class/file named "MainScreen" inside with the content :
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

## 5. MainActivity (composable)
For the moment, the purpose is just to call MainScreen()  

### Class content
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

# Firestore implementation
Following MVVM architecture, We will create model classes to serialize the documents from or to Firestore (like a table), then the repository to handle the calls to the DB, hence some other views and viewModels to display the data. MVVM is almost like DAO but Jetpack compose oriented.

We will create 2 collections that will handle some documents with different ways :
Each collection has its own documents composition created by us.

- cities collection > handle some cities, these documents will have an auto ID as name, will be displayed for non or authenticated users and will be modified only byt the authenticated ones.

- userdata collection > hande the userdata that belongs the user. So each user will have his hown userdata taht will be readable and modifiable only by him. these documents will have as name ther own userID auto created by FIrebaseAuth after the sign-in.

We will need to create some rules in the Firestore Console (site) in order to handle the read and write access for the documents in Firestore. Verry quick to implement !

The repositories will follow the approach of non-null returned value as soon as possible. So if the requests to the repos don't return any values for some reasons, an empty object, list, etc.. will be returned instead.



## 1. Models
the 2 models are "data classes". This kind of classe is especially used to serialize/unserialize the documents from FireStore or Real time Database. 

### 1.1 City (data class)

#### Components explanations
- constructor() : this(null, null, null, null) : the constructor with null params is needed to read the data from the firebase database. Firestore throws this exception if not implemented: "UserData does not define a no-argument constructor"  
- will be usefull to not return null but empty objects following the non-null return approach on the repositories

#### Data class content
- in the main package 
- create package "data"
- inside create package "models"
- inside create kotlin class/file > data class named "City"
``` kotlin
data class City (
    val name:String?,
    val state:String?,
    val country: String?,
    val timestamp: Long? // to retrieve docs by order (auto id not following numbers)
) {
    constructor() : this(null, null, null, null)
}
```

### 1.2 UserData (data class)

#### Data class content
- in package "models"
- create kotlin class/file > data class named "UserData"
``` kotlin
data class UserData (
    val nickname:String?,
    val age: Int?
) {
	constructor() : this(null, null)
}
```

## 2. Repositories


### 2.1 CityRepository (interface)
Contract or scheme that will be implemented by the Firestore CityRepository in our project. The interface make the code flexible and in case we want to implement another DB or a mock repos for test purposes.

#### Interface content
- in package "data"
- create package "repos"
- inside create kotlin class/file > interface named "CityRepository"
``` kotlin
interface CityRepository {
    suspend fun addCity(city: City)
    suspend fun fetchAllCities(): List<City>
    suspend fun fetchAllCitiesWithListener(): Flow<List<City>>
    suspend fun fetchAllCitiesAndIdWithListener(): Flow<List<Pair<City, String>>>
    suspend fun deleteCity(cityId: String)
}
```

### 2.2 UserDataRepository (interface)
contract again with same purpose but for the Firestore UserDataRepository

#### Interface content
``` kotlin
interface UserDataRepository {
    suspend fun addOrUpdateUserData(firebaseUser: FirebaseUser, userData: UserData)
    suspend fun fetchUserData(userId: String): UserData
    suspend fun fetchUserDataWithListener(userId: String): Flow<UserData>
}
```








# Routes creation

## NavRoutes
Store the routes


### Class content
- in the main package (the one of the MainActivity)
- create Kotlin class/file named "NavRoutes" inside with the content :
``` kotlin
sealed class NavRoutes(val route: String) {
    data object Home: NavRoutes("home")
    data object Profile: NavRoutes("profile")
    data object Cities: NavRoutes("cities")
}
```

## Nav host + navController

### MainActivity content (modified)
- modify the MainActivity class

``` kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FirebaseOCTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
}
```




