# Liquid Glass — Fase 3

Framework Android (Jetpack Compose) untuk efek "liquid glass".
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
cuma nggak dapat efek liquid-nya.

Parameter tambahan di Fase 2:
- `intensity: Float` (0f-1f) — kekuatan distorsi shader, hanya berlaku di tier FULL.
- `animate: Boolean` — matikan kalau mau hemat baterai / gelombang statis.

## GlassBlob (Fase 3)

Varian dengan tepi organik yang bergerak, untuk elemen dekoratif:

```kotlin
import com.liquidglass.core.blob.GlassBlob

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
dependencies {
    implementation(project(":liquidglass-core"))
}
```

```kotlin
import com.liquidglass.core.GlassSurface

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
import com.liquidglass.core.liquidGlass

Box(
    modifier = Modifier
        .size(200.dp, 100.dp)
        .liquidGlass(blurRadius = 24f)
)
```

## Struktur

- `liquidglass-core/` — library-nya (yang di-publish/dipakai project lain)
  - `GlassCapability.kt` — deteksi tier efek berdasarkan API level
  - `GlassModifier.kt` — `Modifier.liquidGlass()`, inti blur+tint
  - `GlassSurface.kt` — composable siap pakai
  - `shader/LiquidGlassShader.kt` — source AGSL, disiapkan untuk Fase 2 (belum aktif dipakai)
- `app/` — demo app

## Roadmap

- [x] Fase 1 — blur + tint statis
- [x] Fase 2 — shader AGSL untuk refraction/distorsi + animasi gelombang + tap ripple
- [x] Fase 3 — morphing shape (blob/metaball) via `BlobShape` + `rememberBlobPhase`
- [x] Optimasi performa — cache RuntimeShader, battery-saver aware, skip kerja saat blurRadius=0
- [~] Fase 4 — parallax sensor (accelerometer): **di-drop.** Butuh sensor listener aktif
      terus-menerus + komputasi shader tambahan per frame, untuk efek marginal yang bahkan
      tidak ada padanannya di implementasi Liquid Glass iOS. Tidak sepadan dengan biaya baterai/CPU.
