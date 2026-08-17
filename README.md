# AscentKit — Fase 3

Framework Android (Jetpack Compose) untuk efek visual "liquid morph".
Fase 1: blur real-time + tint. Fase 2: distorsi refraction via shader AGSL + respons sentuh.
Fase 3: morphing shape (blob) — tepi kaca yang "bernapas" secara organik.

## Cara pakai

1. Buka folder ini di Android Studio (versi terbaru yang mendukung AGP 9.x — mis. Otter atau lebih baru).
2. Sync Gradle (Android Studio akan otomatis generate `gradlew`/wrapper jar saat sync pertama).
3. Run module `app` di emulator/device API 26+.
   - **API 33+**: blur + distorsi gelombang shader, animasi berjalan, tap untuk ripple.
   - **API 31-32**: blur real-time polos, tanpa distorsi.
   - **< API 31**: fallback tint datar (lihat `GlassCapability.kt`).

**Versi target:** `compileSdk`/`targetSdk` = 36 (Android 16), sesuai requirement Google Play
per 31 Agustus 2026. `minSdk` tetap 26 — device lama tetap didukung lewat fallback tier,
cuma nggak dapat efek liquid morph-nya.

Parameter tambahan di Fase 2:
- `intensity: Float` (0f-1f) — kekuatan distorsi shader, hanya berlaku di tier FULL.
- `animate: Boolean` — matikan kalau mau hemat baterai / gelombang statis.
- `respectBatterySaver: Boolean` (default true) — animasi otomatis berhenti saat device
  dalam mode hemat baterai.

## GlassBlob (Fase 3)

Varian dengan tepi organik yang bergerak, untuk elemen dekoratif:

```kotlin
import com.ascentkit.core.blob.GlassBlob

GlassBlob(
    modifier = Modifier.size(160.dp),
    wobbleAmplitude = 0.07f, // seberapa jauh tepi "berdenyut"
    wobbleSpeed = 0.9f,      // kecepatan napas, radian/detik
    cornerPct = 0.6f,        // 0 = kotakish, 1 = oval penuh
) {
    Text("Halo", color = Color.White)
}
```

Catatan: `GlassBlob` paling pas untuk badge, avatar, tombol aksi, atau
elemen dekoratif kecil — bukan kartu berisi teks panjang, karena tepinya
terus bergerak dan bisa mengganggu keterbacaan.

## Pakai di project lain

```kotlin
// settings.gradle.kts
include(":ascentkit-core")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":ascentkit-core"))
}
```

```kotlin
import com.ascentkit.core.GlassSurface

GlassSurface(
    modifier = Modifier.size(280.dp, 140.dp),
    blurRadius = 30f,
    cornerRadius = 28f,
) {
    Text("Halo dari balik kaca", color = Color.White)
}
```

Atau langsung sebagai Modifier ke composable apa pun:

```kotlin
import com.ascentkit.core.liquidMorph

Box(
    modifier = Modifier
        .size(200.dp, 100.dp)
        .liquidMorph(blurRadius = 24f)
)
```

## Struktur

- `ascentkit-core/` — library-nya (yang di-publish/dipakai project lain)
  - `GlassCapability.kt` — deteksi tier efek berdasarkan API level
  - `GlassModifier.kt` — `Modifier.liquidMorph()`, inti blur+tint+shader
  - `GlassSurface.kt` — composable siap pakai
  - `LiquidMorphEffectFactory.kt` — perakit `RenderEffect`, cache `RuntimeShader` per surface
  - `BatterySaver.kt` — deteksi mode hemat baterai device
  - `shader/LiquidMorphShader.kt` — source AGSL untuk distorsi/refraction
  - `blob/` — `BlobShape`, `BlobPhase`, `GlassBlob` (morphing shape organik)
- `app/` — demo app

## Optimasi performa

- `RuntimeShader` di-cache per surface (di-`remember`), tidak dikompilasi ulang tiap frame.
- Animasi (shader wave & blob wobble) otomatis berhenti saat device dalam mode hemat baterai.
- `blurRadius <= 0` melewati seluruh `RenderEffect`/`graphicsLayer` offscreen — cocok untuk
  mematikan efek tanpa mengganti composable, atau untuk daftar panjang berisi banyak surface.
- Titik kontrol `BlobShape` disimpan sebagai `FloatArray` mentah, menghindari alokasi objek
  `Offset` per titik pada jalur yang dipanggil tiap frame.

## Roadmap

- [x] Fase 1 — blur + tint statis
- [x] Fase 2 — shader AGSL untuk refraction/distorsi + animasi gelombang + tap ripple
- [x] Fase 3 — morphing shape (blob/metaball) via `BlobShape` + `rememberBlobPhase`
- [x] Optimasi performa — cache RuntimeShader, battery-saver aware, skip kerja saat blurRadius=0
- [~] Fase 4 — parallax sensor (accelerometer): **di-drop.** Butuh sensor listener aktif
      terus-menerus + komputasi shader tambahan per frame, untuk efek marginal yang bahkan
      tidak ada padanannya di implementasi Liquid Glass iOS. Tidak sepadan dengan biaya baterai/CPU.
