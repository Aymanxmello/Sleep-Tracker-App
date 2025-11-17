plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.sleeptrackerapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sleeptrackerapp"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // Utilisation des versions du catalogue TOML
    implementation("androidx.viewpager2:viewpager2:${libs.versions.viewpager2.get()}") // Utilise la version 1.1.0
    implementation("com.airbnb.android:lottie:${libs.versions.lottie.get()}")       // Utilise la version 6.7.1

    // Déclaration de la BOM en utilisant le catalogue
    implementation(platform("com.google.firebase:firebase-bom:${libs.versions.firebaseBom.get()}"))

    // Retour à la syntaxe simple pour que la BOM gère les versions
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}