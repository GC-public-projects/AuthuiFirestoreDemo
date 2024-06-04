# AuthuiFirestoreDemo 
### (Project Workable, Documentation in progress... )
mini project that mix Authui and Firestore by using Jetpack Compose as toolkit and Kotlin for the language

## Presentation
The goal of this demo is to show how the Firestore DB can be called and how the data can be handled after fetching or before sending it to the DB. 
As it is better to use the less code we can in order to gain in understanding, we will use "Authui", a library that will create for us the screens with the fields needed (activities) to authenticate.
Then we will create 2 collections of documents in the Firestore DB and some "CRUD" functions to interact with the data in real time or not.   
Some rules will be created via The Firebase Console (site) in order to make the data handled with read and/or write rights in funtion of our needs. In short, one collection (cities) will be available for non and authenticated users and the documents of the other collection (userData) will be available only for the users they belong.  
As we use Jetpack Compose, the patern MVVM (model, view viewModel) will be used to sort the data, the features and the views.
Except an exception of Firestore "permission" necessary for understanding, to obtain the shortest possible code, the other exceptions will not be managed.

## Warning
- The version of the dependencies used here are workable for the project. I figured out, some new versions are not compatible for the moment with some components of our project. I reported the ones that make issues but in the future, it is possible some other new versions of some other libraries wont be ddcompatible too.

- With Authui, "Email Enumeration protection" doesn't work, so the use of this library is not a good way to make the app secure. In addition, i had some problems to use the Google authentication way with it, that's why only the mail authentication way will be used here.



# Init
- create FireBase project in https://console.firebase.google.com/ and enable Google Analytics if needed.
- create Jetpack Compose project in Android Studio
- chose for the 2 projects the same name : "AuthuiFirestoreDemo"


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
dependencies {
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
This composable takes an "onSignInresult" function as param that will be executed later by "rememberLauncherForActivityResult". We will pass the content of the function during the call of the composable later.  
  
### Purpose 
- setup the authentication providers (Mail, google, Github, etc...) (Just "mail" is setup)
- create the Intent of the sign-in/Sign-up and launch the AuthUI Activity
- thanks to the function as param, the result of the tasks done in the activity will be handled later.

### function content
- create package "screens" in the main package (the one of the MainActivity)
- create Kotlin class/file inside named "SignInScreen"
- import the dependencies (to do all the time for each content and wont be specified anymore !)
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
Singleton (object) that implements "FirebaseAuth.AuthStateListener"

### Purpose
The object contents as flow the signed in user (FirebasUser) from FirebaseAuth.getInstance() and the signiedInStatus crated by us.  
By implementing the AuthStateListener we ensure we have the status and the data in real time of the Firebase user. The singleton will let us get the Auth data in many places without needing to use the dependency injection as it will be needed in the most of the composables.

### Components explanations
- FirebaseAuth.AuthStateListener : this interface can be implemented by an activity or a viewModel. In our case as the main of the composables will need the FireBaseAuth, it is better to create a singleton and call it when needed.
- _firebaseAuth.addAuthStateListener(this) : The AuthStateListener oberves the changes in real time of the auth, thanks to it, we don't need to test the auth each time we need it.
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
- create Kotlin class/file named "MainViewModel" inside
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
- showSignIn boolean flag obligatory because the Onclick from a button cannot contain a composable. So the composable SIgnInScreen is displayed after the flag becomes true

### functions content
- in package "screens"
- create Kotlin class/file named "MainScreen"
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

The repositories will follow the approach of non-null returned value as long as possible. So if the requests to the repos don't return any values for some reasons, an empty object, list, etc.. will be returned instead.


## 1 Models
the 2 models are "data classes". This kind of classes is especially used to serialize/unserialize the documents from FireStore or Real time Database. 

### 1.1 City (data class)

#### Components explanations
- constructor() : this(null, null, null, null) : the constructor with null params is needed to read the data from the firebase database. Firestore throws this exception if not implemented: "UserData does not define a no-argument constructor"  
- will be usefull to not return null but empty objects following the non-null return approach of the repositories
- the timestamp is usefull to sort the documents by arrival order as they are no numbers assigned to them in function of the cration date

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

#### Components explanations
- addCity(city: City) : .add is used instead .set because we want to auto generate a document name in the DB for the city. the name is a String ID.

- fetchAllCities() : the function return a simple list of cities

- fetchAllCitiesWithListener() : the function returns as flow a list of cities from the callbackFlow { ... }, in this one we will add a snapshotListener to the "cities" collection. In the snapshotListener, the objects created from the query are rebuilt and returned again when the cities collection is modified. 
Flows are a modern approach to handle the data from the listeners but it is possible to use Livedata too.  


- fetchAllCitiesAndIdWithListener() : same than fetchAllCitiesWithListener() but returns as flow a list of pairs in which each city will be attached to its document ID. This ID will be usefull when we will need to delete a city from the DB. Indeed, contrary to the records of the tables from the relational DBs, the content of the documents (record) don't content any attribute (like primary key) to recognise them in order to delete them. So we need to return the document ID.
It is possible to add the document ID to the aatributes of the documents but this is not a good way as we will create redundant info.

- deleteCity(cityId: String) : thanks to the document ID of each city, the city will be deleted. Lets add .addOnSuccessListener { ... } and addOnFailureListener { ... } in order to handel the result of the delition (in occurrence some logs)



#### Class content
- in package "firestore"
- create kotlin class/file > class named "FirestoreCityRepository"
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

#### Purpose
When an user is created with Firebase Auth, an auto ID that belongs to it is created too. Then for each user, we will have the possibility to create a document that belong to him. This document will have as ID the user ID. We will store these docuemnts in the UserData collection. Later we will create some rules to ensure only the authenticated user be the only one to access its data.

#### Components explanations
- addOrUpdateUserData(firebaseUser: FirebaseUser, userData: UserData) : By using ".set" we need to specify the document ID.Contrary with .add, it is possible to use many times the same id with different data to call the function. In this case, if the ID already exists, Firestore taht cannot crate 2 documents with the same id in the collection will replace the content of the former document by the new one. This operation consists of an update. Of course if the ID doesn't exist, the document will be created.

- fetchUserData(userId: String) : this function returns an UserData object. Later in the project, we will prove??????????????????????....................


#### Class content
- in package "firestore"
- create kotlin class/file > class named "FirestoreUserDataRepository"
``` kotlin
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

### 4.1 CitiesScreen (composable)

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

### 4.2 ProfileScreen (composable)

#### Composable content
- in package "screens"
- create Kotlin class/file named "ProfileScreen"
``` kotlin
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
    viewModel.getUserDataFromDb("INSERT ONE USER ID HERE !!")

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
```





# navigation creation

## 1 NavRoutes (sealed class)
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

## 2 MainActivity modification (class)
### Nav host + navController + some dependecy injections


### Class content 
``` kotlin
class MainActivity : ComponentActivity() {
    private val db = FirestoreDB.instance
    private lateinit var cityRepository: CityRepository
    private lateinit var userDataRepository: UserDataRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cityRepository = FirestoreCityRepository(db)
        userDataRepository = FirestoreUserDataRepository(db)
        
        enableEdgeToEdge()
        setContent {
            AuthuiFirestoreDemoTheme {
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
                MainScreen()
            }
        }
    }
}
```

## 3 MainScreen modification (composable)
navigation implmentation


### functions content
- modify MainScreen
``` kotlin
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
```

# Firestore rules

in Firebase Site > our project > Firestore Database > Rules
``` fsl
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /cities/{cityId} {
      allow read: if true;
      allow write: if request.auth != null;
   }    
    match /userdata/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
  	}
  }
}
```
