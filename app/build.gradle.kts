plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

kotlin {
    jvmToolchain(21)
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

}

dependencies {

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:${libs.versions.firebaseBom.get()}"))

    // Firebase products (NO VERSIONS HERE)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Ajout du support KTX pour Firebase Auth (simplifie l'accès à l'utilisateur)
    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")


    // Other dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment)
    implementation("androidx.viewpager2:viewpager2:${libs.versions.viewpager2.get()}")
    implementation("com.airbnb.android:lottie:${libs.versions.lottie.get()}")



    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.cardview:cardview:1.0.0")


    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


}