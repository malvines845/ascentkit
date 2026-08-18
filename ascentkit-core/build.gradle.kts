plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ascentkit.core"
    compileSdk = 37

    defaultConfig {
        // API 26 = minimum absolut (RenderEffect butuh 31, AGSL butuh 33).
        // Di bawah 31 otomatis jatuh ke fallback statis (lihat GlassCapability.kt)
        minSdk = 26
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
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.annotation:annotation:1.9.1")
}
