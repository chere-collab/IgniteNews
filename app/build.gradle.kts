import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.tryanderror"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tryanderror"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Feature 93: Secure Property Ingestion
        val properties = Properties()
        val secretsFile = project.rootProject.file("secrets.properties")
        if (secretsFile.exists()) {
            properties.load(FileInputStream(secretsFile))
        }
        val apiKey = properties.getProperty("NEWS_API_KEY", "MISSING_KEY")
        buildConfigField("String", "NEWS_API_KEY", "\"$apiKey\"")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.0")
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation(libs.core.ktx)

    // Networking - Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Glide - for loading images from URLs
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // SwipeRefreshLayout - for pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
