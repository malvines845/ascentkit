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

## GlassLens — varian eksperimental (tanpa blur, distorsi lensa)

Berbeda secara fundamental dari `GlassSurface`: TIDAK memakai blur sama sekali. Sebagai
gantinya, konten di belakang kaca disample dengan pembelokan (refraksi) yang menguat
mendekati tepi, mensimulasikan kaca fisik yang membelokkan/membesarkan cahaya di
sekitar tepinya — bukan mengaburkannya.

```kotlin
import com.ascentkit.core.lens.GlassLens

GlassLens(
    modifier = Modifier.size(280.dp, 140.dp),
    cornerRadius = 28.dp,
    borderWidth = 1.5.dp,     // lebar rim light di tepi
    lensZoneWidth = 20.dp,    // lebar total zona distorsi (termasuk border)
    refraction = 1.0f,        // kekuatan pembelokan/pembesaran
) {
    Text("Liquid Glass asli", color = Color.White)
}
```

Tiga zona konsentris dari tepi ke tengah:
1. **Border zone** (`borderWidth`) — rim light: garis highlight tipis mensimulasikan
   pantulan cahaya di tepi kaca fisik.
2. **Lens zone** (`lensZoneWidth - borderWidth`) — distorsi refraksi kuat, mengikuti
   lengkungan `cornerRadius`, termasuk di sudut membulat.
3. **Core zone** (sisa ruang di tengah) — kaca tenang nyaris tanpa distorsi, ruang aman
   untuk teks/konten. `content` otomatis diberi padding sebesar `lensZoneWidth` agar
   jatuh di zona ini.

**Perbandingan dengan `GlassSurface`:**

| | `GlassSurface` (Liquid Morph) | `GlassLens` |
|---|---|---|
| Blur | Ya, `RenderEffect.createBlurEffect` | Tidak ada sama sekali |
| Efek utama | Blur + gelombang + ripple sentuh | Distorsi lensa di tepi + rim light |
| Biaya render | Lebih ringan | Lebih berat (SDF rounded-box per piksel) |
| Tier minimum untuk efek penuh | API 31 (blur polos), API 33 (+ shader) | API 33 (murni butuh RuntimeShader) |
| Fallback di bawah tier minimum | Tint datar | Tint + border digambar statis |

Keduanya sengaja dipertahankan terpisah (bukan salah satu menggantikan yang lain) supaya
bisa dibandingkan langsung — lihat demo di `app/`.

## GlassButton

Tombol berbahan kaca dengan feedback tekan bawaan (scale + intensitas shader naik saat ditekan):

```kotlin
import com.ascentkit.core.GlassButton

GlassButton(onClick = { /* aksi */ }) {
    Text("Tekan aku", color = Color.White)
}
```

Parameter khusus: `pressedScale` (default 0.96f), `pressedIntensityBoost` (default 0.2f),
`enabled`. Tidak bergantung pada Material/Material3 — ripple memakai `LocalIndication`
bawaan Compose Foundation, otomatis mengikuti tema ripple proyek konsumen jika ada.

## GlassBottomSheet

Modal bottom sheet berbahan kaca. Dikontrol dari luar (tidak mendukung drag/swipe —
tap area scrim atau panggil `onDismissRequest` secara programatik):

```kotlin
import com.ascentkit.core.GlassBottomSheet

var showSheet by remember { mutableStateOf(false) }

GlassBottomSheet(
    visible = showSheet,
    onDismissRequest = { showSheet = false },
) {
    Text("Isi bottom sheet", color = Color.White)
}
```

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
  - `GlassSurface.kt` — composable siap pakai (struktur 2-layer: background blur terpisah dari konten)
  - `GlassButton.kt` — tombol kaca dengan press animation
  - `GlassBottomSheet.kt` — modal bottom sheet kaca (kontrol eksternal, tanpa drag)
  - `LiquidMorphEffectFactory.kt` — perakit `RenderEffect`, cache `RuntimeShader` per surface
  - `BatterySaver.kt` — deteksi mode hemat baterai device
  - `shader/LiquidMorphShader.kt` — source AGSL untuk distorsi/refraction (blur-based)
  - `blob/` — `BlobShape`, `BlobPhase`, `GlassBlob` (morphing shape organik)
  - `lens/` — `GlassLens`, `GlassLensShader`, `GlassLensEffectFactory` (varian tanpa blur,
    distorsi lensa + rim light, lihat bagian GlassLens di atas)
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
