package com.ascentkit.core.container

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Container yang menyediakan satu snapshot bitmap BERSAMA dari slot [background]-nya untuk
 * semua [com.ascentkit.core.lens.GlassLens] (dan komponen glass lain yang mendukungnya) di
 * [content] — termasuk untuk area yang berada di LUAR bounds masing-masing glass individual.
 *
 * Ini yang memungkinkan efek "sampling area lebih besar dari glass itu sendiri", sesuai
 * prinsip Liquid Glass Apple: glass tidak hanya melihat apa yang persis ada di
 * belakangnya, tapi juga sedikit area di sekitarnya, sehingga distorsi lensa terasa
 * "menarik" konten dari sekitar, bukan cuma dari tepat di baliknya.
 *
 * ## Kenapa `background` dan `content` adalah slot TERPISAH (bukan satu lambda tunggal)
 *
 * GraphicsLayer di Compose membentuk sebuah DAG (directed acyclic graph) untuk hubungan
 * antar-layer. Jika snapshot yang disample oleh sebuah glass ikut menyertakan glass itu
 * sendiri (atau glass lain yang juga sample dari snapshot yang sama), hasilnya adalah
 * siklus semantik: distorsi lensa akan "melihat" dirinya sendiri dari frame sebelumnya,
 * menghasilkan artefak visual yang tidak konsisten dan berpotensi memburuk seiring waktu
 * (bukan sekadar delay ringan yang bisa diabaikan).
 *
 * Karena itu, HANYA konten di slot [background] yang pernah masuk ke capture bitmap.
 * Slot [content] (tempat glass children diletakkan) digambar secara normal di atasnya
 * setelah capture, dan tidak pernah menjadi bagian dari snapshot.
 *
 * ## Kenapa capture memakai `withFrameNanos`, bukan `delay()`
 *
 * `delay()` berjalan independen dari Choreographer/frame clock Compose — timing-nya bisa
 * jatuh di titik mana pun relatif terhadap siklus render, membuat capture tidak selaras
 * dengan kapan frame benar-benar selesai digambar. `withFrameNanos` menyuspensi tepat
 * hingga frame berikutnya di-dispatch oleh Choreographer, sehingga setiap capture terjadi
 * pada batas frame yang jelas.
 *
 * ## Kenapa tetap di-throttle (skip N frame), bukan capture tiap frame
 *
 * `GraphicsLayer.toImageBitmap()` adalah operasi suspend yang melibatkan alokasi/transfer
 * bitmap — menjalankannya di setiap frame (berpotensi 60-120x per detik) akan membebani
 * CPU/GPU dan memori secara signifikan untuk manfaat kesegaran visual yang biasanya tidak
 * terlihat pengguna. [framesPerCapture] mengontrol berapa frame dilewati di antara tiap
 * capture; nilai default melewati 6 frame (~setiap 100ms pada layar 60Hz).
 *
 * @param framesPerCapture jumlah frame yang dilewati Choreographer di antara tiap capture
 *                         snapshot. Nilai lebih kecil = snapshot lebih segar tapi lebih
 *                         mahal; lebih besar = lebih hemat tapi background yang terlihat
 *                         lewat glass terasa lebih "tertinggal" saat beranimasi cepat.
 * @param background       konten yang ingin "dilihat" lewat kaca (mis. gambar, gradient,
 *                          konten scrollable). HANYA slot ini yang di-capture ke bitmap.
 * @param content           slot untuk glass children ([com.ascentkit.core.lens.GlassLens],
 *                          [com.ascentkit.core.GlassSurface], dll) dan elemen UI lain yang
 *                          TIDAK boleh ikut ter-capture. Digambar di atas [background]
 *                          secara normal.
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    framesPerCapture: Int = 6,
    background: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val backgroundLayer = rememberGraphicsLayer()
    val inPreview = LocalInspectionMode.current

    var snapshot by remember {
        mutableStateOf(GlassContainerSnapshot(bitmap = null, containerSizePx = Offset.Zero))
    }
    var containerSizePx by remember { mutableStateOf(Offset.Zero) }

    if (!inPreview) {
        // Loop capture: menunggu tepat `framesPerCapture` frame Choreographer (bukan
        // delay() yang lepas dari render clock), lalu capture SATU KALI. Loop otomatis
        // berhenti saat GlassContainer keluar dari komposisi karena LaunchedEffect
        // dibatalkan mengikuti lifecycle composable induknya.
        LaunchedEffect(backgroundLayer, framesPerCapture) {
            while (true) {
                repeat(framesPerCapture) {
                    withFrameNanos { /* menunggu frame berikutnya, tidak melakukan apa pun */ }
                }
                val bitmap = backgroundLayer.toImageBitmap()
                snapshot = GlassContainerSnapshot(
                    bitmap = bitmap,
                    containerSizePx = containerSizePx,
                )
            }
        }
    }

    CompositionLocalProvider(LocalGlassContainerSnapshot provides snapshot) {
        Box(
            modifier = modifier
                .onSizeChanged { containerSizePx = Offset(it.width.toFloat(), it.height.toFloat()) },
        ) {
            // Slot background: direkam ke backgroundLayer (untuk snapshot di atas) DAN
            // digambar normal ke layar. Hanya elemen di slot ini yang pernah masuk capture.
            Box(
                modifier = Modifier.drawWithContent {
                    backgroundLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(backgroundLayer)
                },
            ) {
                background()
            }

            // Slot content: digambar normal di atas background, TIDAK PERNAH ikut masuk
            // ke backgroundLayer manapun. Glass children di sini mengambil snapshot
            // background lewat LocalGlassContainerSnapshot, bukan dengan mensampling
            // lapisan tempat mereka sendiri berada.
            content()
        }
    }
}
