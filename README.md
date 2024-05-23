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
- start in test mode

## Link app to project
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

## AuthUI
in "app level" build.gradle.kts 
``` kotlin
dependencies {dependencies {
	...
	implementation ("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation("com.google.android.gms:play-services-auth:20.7.0") // ! v21 not compatible with AuthUi
}
```
