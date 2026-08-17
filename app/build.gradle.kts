plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ascentkit.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ascentkit.demo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Tidak perlu blok kotlinOptions: dengan built-in Kotlin (AGP 9+), jvmTarget
    // otomatis mengikuti compileOptions.targetCompatibility di atas.

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":ascentkit-core"))

    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
