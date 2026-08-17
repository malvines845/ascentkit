// Top-level build file
//
// CATATAN: sejak AGP 9.0, plugin 'org.jetbrains.kotlin.android' TIDAK BOLEH lagi
// diaplikasikan manual — AGP 9.x sudah menyertakan dukungan Kotlin bawaan ("built-in
// Kotlin") dan otomatis mendaftarkan extension 'kotlin' sendiri. Mengaplikasikan plugin
// lama di atasnya menyebabkan error "Cannot add extension with name 'kotlin', as there
// is an extension already registered with that name."
plugins {
    id("com.android.application") version "9.2.0" apply false
    id("com.android.library") version "9.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
