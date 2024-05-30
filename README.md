# AuthuiFirestoreDemo (in progress... )
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

## 1 Authui SignIn screen (composable)
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
## 2 AuthManager (object)
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


## 3 MainViewModel (class)
Viewmodel linked to "MainScreen"

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

## 4 MainScreen & MyColumn (composables)

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

## 5 MainActivity (composable)
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



## 1 Models
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

## 2 Repositories


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
- in package "repos"
- create kotlin class/file > interface named "UserDataRepository"
``` kotlin
interface UserDataRepository {
    suspend fun addOrUpdateUserData(firebaseUser: FirebaseUser, userData: UserData)
    suspend fun fetchUserData(userId: String): UserData
    suspend fun fetchUserDataWithListener(userId: String): Flow<UserData>
}
```

### 2.3 FirestoreDB (object)
Singleton that content the instance of the Firestore DB

#### Components explanations
By lazy is used here but not mandatory as the object instanciations are implicitely lazy. If "lazy is not explicitely used" like that "val instance = Firebase.firestore", it works but Android Studio displays A warning about potential memory leaks du to static fields.

#### Object content
- in package "repos"
- create package "firestore"
- inside create kotlin class/file > object named "FirestoreDB"
``` kotlin
object FirestoreDB {
    val instance: FirebaseFirestore by lazy {
        Firebase.firestore
    }
}
```

### 2.4 FirestoreCityRepository (class)
implementation of the CityRepository interface by using FirestoreDB as dependecy injection to make the different calls to the Firestore DB in the "cities" collection

#### Class content
- in package "firestore"
- create kotlin class/file > class named "FirestoreCityRepository "
``` kotlin
class FirestoreCityRepository(private val db: FirebaseFirestore): CityRepository {
    override suspend fun addCity(city: City) {
        db
            .collection("cities")

            // for custom ID
            //.document("${city.name}")
            //.set(city)

            // for auto ID
            .add(city)
    }

    override suspend fun fetchAllCities(): List<City> {
        val cities = mutableListOf<City>()
        val querySnapshot = db
            .collection("cities")
            .get()
            .await()

        for (document in querySnapshot.documents) {
            val city = document.toObject(City::class.java)
            city?.let {
                cities.add(city)
            }
        }
        return cities
    }

    override suspend fun fetchAllCitiesWithListener(): Flow<List<City>> = callbackFlow {
        val citiesCollection = db.collection("cities")

        val subscription = citiesCollection.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            // cities must be in the snapshotListener in order it gets update from DB
            val cities = mutableListOf<City>()

            querySnapshot?.documents?.forEach { document ->
                val city = document.toObject(City::class.java)
                city?.let {
                    cities.add(city)
                }
            }
            trySend(cities).isSuccess
        }

        awaitClose { subscription.remove() }
    }

    override suspend fun fetchAllCitiesAndIdWithListener(): Flow<List<Pair<City, String>>> = callbackFlow{
        val citiesCollection = db.collection("cities")

        val subscription = citiesCollection.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            // citiesAndId must be in the snapshotListener in order it gets update from DB
            val citiesAndId = mutableListOf<Pair<City, String>>()

            querySnapshot?.documents?.forEach { document ->
                val city = document.toObject(City::class.java)
                val id = document.id
                city?.let {
                    citiesAndId.add(Pair(city, id))
                }
            }
            trySend(citiesAndId).isSuccess

        }

        awaitClose { subscription.remove() }
    }

    override suspend fun deleteCity(cityId: String) {
        // do not delete the collections inside the document !
        db.collection("cities").document(cityId)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully deleted!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }
    }
}
```

### 2.5 FirestoreUserDataRepository (class)
implementation of the UserDataRepository interface by using FirestoreDB as dependecy injection to make the different calls to the Firestore DB in the "userdata" collection

#### Class content
- in package "firestore"
- create kotlin class/file > class named "FirestoreUserDataRepository"
``` kotlin
class FirestoreUserDataRepository {
    class FirestoreUserDataRepository(
        private val db: FirebaseFirestore
    ) : UserDataRepository {
        override suspend fun addOrUpdateUserData(
            firebaseUser: FirebaseUser,
            userData: UserData
        ) {
            // the .set function create or update a document if it already exists
            db.collection("userdata")
                .document(firebaseUser.uid)
                .set(userData)
        }

        override suspend fun fetchUserData(userId: String): UserData {
            return try {
                val querySnapShot = db.collection("userdata")
                    .document(userId)
                    .get()
                    .await()

                // if obect is null > return of an empty UserData object
                querySnapShot.toObject(UserData::class.java) ?: UserData()
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("FetchUserData", "Unauthorized access: ${e.message}")
                    UserData("Unauthorized access", 0)
                } else {
                    Log.e("FetchUserData", "Firestore error: ${e.message}")
                    UserData("Firestore error", 0)
                }
            }
        }

        override suspend fun fetchUserDataWithListener(userId: String): Flow<UserData> =
            callbackFlow {

                val userDataRef = db.collection("userdata").document(userId)

                val subscription = userDataRef.addSnapshotListener { documentSnapshot, exception ->
                    if (exception != null) {
                        close(exception)
                        return@addSnapshotListener
                    }

                    val userData = documentSnapshot?.toObject(UserData::class.java) ?: UserData()
                    trySend(userData).isSuccess

                }

                awaitClose { subscription.remove() }
            }
    }
}
```


## 3 ViewModels

### 3.1 CitiesViewModel (class)
ViewModel linked to "CitiesScreen"

#### Class content
- in package "screens"
- create kotlin class/file > class named "CitiesViewModel"
``` kotlin
class CitiesViewModel(
    private val cityRepository: CityRepository
) : ViewModel() {
    companion object {
        fun provideFactory(
            cityRepository: CityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
            ): T {
                if (modelClass.isAssignableFrom(CitiesViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return CitiesViewModel(cityRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    // FireStore properties purpose
    private val _citiesList = mutableStateListOf<City>()
    val citiesList: List<City> = _citiesList

    private val _citiesFlowWithListener = MutableStateFlow(emptyList<City>())
    val citiesFlowWithListener: StateFlow<List<City>> = _citiesFlowWithListener.asStateFlow()

    private val _citiesAndIdFlowWithListener = MutableStateFlow(emptyList<Pair<City, String>>())
    val citiesAndIdFlowWithListener: StateFlow<List<Pair<City, String>>> = _citiesAndIdFlowWithListener.asStateFlow()

    init {
        // get all cities
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // cites without listener affectation
                cityRepository.fetchAllCities().forEach {
                    _citiesList.add(it)
                }
            }
        }
        // get all cities with listener
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // flow collection of cities with listener
                cityRepository.fetchAllCitiesWithListener().collect {
                    _citiesFlowWithListener.value = it
                }
            }
        }
        // get all cities with listener and ID
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // flow collection of cities and ID with listener
                cityRepository.fetchAllCitiesAndIdWithListener().collect {
                    _citiesAndIdFlowWithListener.value = it
                }
            }
        }
    }
    // FireStore methods purpose
    fun createCityToDatabase(name: String, state: String, country: String) {
        val city = City(name, state, country, System.currentTimeMillis())
        // use of launch > no return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cityRepository.addCity(city)
            }
        }
    }
    fun deleteCity(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cityRepository.deleteCity(id)
            }
        }
    }
}
```

### 3.2 ProfileViewModel (class)
ViewModel linked to "ProfileScreen"

#### Class content
- in package "screens"
- create kotlin class/file > class named "ProfileViewModel"
``` kotlin
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
    private val _signedInUser = MutableStateFlow<FirebaseUser?>(null)
    val signedInUser = _signedInUser.asStateFlow()

    private val _userData = MutableStateFlow<UserData>(UserData())
    val userData = _userData.asStateFlow()

    private val _targetedUserData = mutableStateOf(UserData())
    val targetedUserData: State<UserData> = _targetedUserData


    init {
        _signedInUser.value = FirebaseAuth.getInstance().currentUser

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                    _signedInUser.value?.let { firebaseUser ->
                        userDataRepository.fetchUserDataWithListener(firebaseUser.uid).collect {
                            _userData.value = it
                        }
                    }
            }
        }
    }
    fun createUserDataToDatabase(nickName: String, age: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _signedInUser.value?.let {
                    userDataRepository.addOrUpdateUserData(_signedInUser.value!!, UserData(nickName, age))
                }
            }
        }
    }
    // purpose : check rules in firestore
    fun getUserDataFromDb(userId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _targetedUserData.value = userDataRepository.fetchUserData(userId)
            }
        }
    }
}
```

## 4 Views

### 4.4 CitiesScreen (composable)

#### Composable content
- in package "screens"
- create Kotlin class/file named "CitiesScreen"
``` kotlin
@Composable
fun CitiesScreen(cityRepository: CityRepository) {

    val viewModel: CitiesViewModel = viewModel(
        factory = CitiesViewModel.provideFactory(cityRepository)
    )

    var cityName by remember { mutableStateOf("") }
    val modifyCityName = { name: String -> cityName = name }
    val citiesWithoutListener = viewModel.citiesList
    val citiesWithListener by viewModel.citiesFlowWithListener.collectAsState()
    val citiesAndIdWithListener by viewModel.citiesAndIdFlowWithListener.collectAsState()

    MyColumn(
        viewModel = viewModel,
        cityName = cityName,
        modifyCityName = modifyCityName,
        citiesWithoutListener = citiesWithoutListener,
        citiesWithListener = citiesWithListener,
        citiesAndIdWithListener = citiesAndIdWithListener
    )
}

@Composable
fun MyColumn(
    viewModel: CitiesViewModel,
    cityName: String,
    modifyCityName: (String) -> Unit,
    citiesWithoutListener: List<City>,
    citiesWithListener: List<City>,
    citiesAndIdWithListener: List<Pair<City, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        // Create City
        // --------------
        Text(text = "create city Name")
        TextField(value = cityName, onValueChange = modifyCityName)
        Button(onClick = {
            viewModel.createCityToDatabase(name = cityName, state = "default", country = "default")
        }) {
            Text("Add city to DB")
        }
        Spacer(Modifier.padding(2.dp))

        // Get cities without listener
        // ----------------------------
        Text(text = "Show all cities from the DB without listener")
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(citiesWithoutListener) { city ->
                CityListItem(city = city)
            }
        }

        // Get cities with listener
        // ----------------------------
        Text(text = "Show all cities from the DB with listener")
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(citiesWithListener) { city ->
                CityListItem(city = city)
            }
        }

        // Get cities and ID with listener
        // ----------------------------------
        Text(text = "Show all cities with listener and deletable")
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(citiesAndIdWithListener) { pair ->
                CityListItemDeletable(city = pair.first, id = pair.second, viewModel = viewModel)
            }
        }
    }
}
// City list item
@Composable
fun CityListItem(city: City) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { },
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = city.name ?: "blank")
            Text(text = city.country ?: "blank")
            Text(text = city.state ?: "blank")
        }
    }
}

// City list item deletable
@Composable
fun CityListItemDeletable(city: City, id: String, viewModel: CitiesViewModel) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = city.name ?: "blank")
                Text(text = city.country ?: "blank")
                Text(text = city.state ?: "blank")
            }
            IconButton(
                onClick = { viewModel.deleteCity(id) },
                content = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            )
        }
    }
}
```
test




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




