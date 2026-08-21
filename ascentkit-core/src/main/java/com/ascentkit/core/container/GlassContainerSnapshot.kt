package com.ascentkit.core.container

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Snapshot konten [GlassContainer], dibagikan ke semua glass surface (GlassLens,
 * GlassSurface) di dalamnya lewat [LocalGlassContainerSnapshot].
 *
 * @param bitmap          potret HANYA slot `background` dari [GlassContainer] (bukan
 *                        seluruh container termasuk glass children) pada capture terakhir
 *                        yang berhasil. Null selagi capture pertama belum selesai (mis.
 *                        pada frame-frame paling awal composition).
 * @param containerSizePx ukuran container dalam px, dipakai untuk validasi/clamping
 *                        koordinat sampling.
 *
 * PENTING (arsitektur — cegah self-sampling): bitmap ini HANYA berisi hasil render slot
 * `background` milik [GlassContainer], TIDAK PERNAH menyertakan glass children (GlassLens/
 * GlassSurface) di dalamnya. Ini disengaja: GraphicsLayer di Compose membentuk sebuah DAG
 * (directed acyclic graph) — kalau sebuah glass ikut tercapture dalam bitmap yang lalu
 * disample balik oleh glass itu sendiri, hasilnya adalah siklus semantik (bukan cuma bug
 * kecil) yang membuat hasil visual tidak konsisten dan berpotensi memburuk dari waktu ke
 * waktu. Karena itu `GlassContainer` mewajibkan slot `background` dan `content` terpisah,
 * dan HANYA `background` yang pernah masuk ke capture ini.
 *
 * PENTING (frame sync & keterlambatan): capture dilakukan lewat loop yang menunggu
 * `withFrameNanos` (selaras dengan Choreographer/frame clock Compose, BUKAN `delay()`
 * yang lepas dari render loop), namun di-throttle dengan melewati sejumlah frame di
 * antara tiap capture (lihat `GlassContainer.framesPerCapture`) karena `toImageBitmap()`
 * adalah operasi suspend yang tidak murah untuk dijalankan tiap frame. Akibatnya snapshot
 * ini best-effort dan bisa tertinggal beberapa frame di belakang konten yang benar-benar
 * tampil saat ini — untuk `background` yang jarang beranimasi cepat, keterlambatan ini
 * praktis tidak terlihat.
 */
internal data class GlassContainerSnapshot(
    val bitmap: ImageBitmap?,
    val containerSizePx: Offset,
)

/**
 * CompositionLocal yang menyediakan [GlassContainerSnapshot] dari [GlassContainer]
 * terdekat yang membungkus composable saat ini. Defaultnya snapshot kosong (bitmap null),
 * yang berarti glass surface yang tidak dibungkus [GlassContainer] akan otomatis jatuh ke
 * perilaku lama (sampling dalam bounds sendiri saja, lihat GlassLensModifier) — tidak ada
 * error atau crash, hanya kembali ke mode sebelumnya.
 */
internal val LocalGlassContainerSnapshot = compositionLocalOf {
    GlassContainerSnapshot(bitmap = null, containerSizePx = Offset.Zero)
}
