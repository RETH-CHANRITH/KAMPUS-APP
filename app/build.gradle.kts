plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.kampus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kampus"
        minSdk = 26
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

    @Suppress("DEPRECATION")
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    // Supabase (for image storage)
    implementation("io.github.jan-tennert.supabase:supabase-kt:2.2.2")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.2.2")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.2.2")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.2.2")
    implementation("io.ktor:ktor-client-okhttp:2.3.0")

    // Android APIs used by current screens
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("io.github.webrtc-sdk:android:125.6422.07")

    // Google Sign-In (for Firebase Auth with Google)
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    
    // Location services
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.compose.foundation.layout)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation(libs.androidx.activity.compose)
    
    // Security (for E2EE encrypted preferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Coroutines Play Services (for Task<T>.await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.7.0")

    // Image loading (for gallery attachments)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // CameraX (story camera capture)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Video playback (Media3 / ExoPlayer)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // WorkManager for background uploads and retries
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // WebRTC (native) - Maven Central hosted fork that keeps org.webrtc packages
    implementation("io.github.webrtc-sdk:android:125.6422.07")

    // WebSocket client for signaling
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // Image crop editor
    implementation("com.github.yalantis:ucrop:2.2.10")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")


    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(libs.androidx.compose.runtime)
    implementation(libs.play.services.location)
    implementation(libs.play.services.contextmanager)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}