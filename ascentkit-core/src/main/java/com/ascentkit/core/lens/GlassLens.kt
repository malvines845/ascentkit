package com.ascentkit.core.lens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composable siap-pakai untuk efek "glass lens distortion" — varian eksperimental yang
 * jauh lebih kompleks dari [com.ascentkit.core.GlassSurface]: TANPA BLUR sama sekali,
 * memakai distorsi refraksi bergaya lensa fisik yang menguat di tepi dan mereda menuju
 * tengah, plus rim light (garis highlight) tipis mengikuti kontur border.
 *
 * PENTING (ruang konten): karena zona lensa (yang penuh distorsi) ada di sekeliling tepi,
 * [content] otomatis diberi padding sebesar [lensZoneWidth] agar teks/elemen di dalamnya
 * jatuh di zona core yang tenang, bukan di zona lensa yang terdistorsi. Ini membuat teks
 * tetap terbaca meski dekat dengan tepi kaca yang "melengkung" secara visual.
 *
 * Sengaja dibuat sebagai composable TERPISAH dari [com.ascentkit.core.GlassSurface] (bukan
 * menggantikannya) supaya keduanya bisa dibandingkan langsung — [com.ascentkit.core.GlassSurface]
 * lebih murah secara performa (masih memakai blur asli) dan cocok untuk kebanyakan kasus;
 * [GlassLens] jauh lebih dekat ke tampilan "liquid glass" fisik/asli tapi shader-nya lebih
 * berat karena menghitung SDF rounded-box per piksel.
 *
 * Hanya tier FULL (API 33+) yang mendapat distorsi & rim light penuh. Di bawah itu, tampilan
 * jatuh ke tint + border statis (lihat [glassLens]).
 *
 * Contoh:
 * ```
 * GlassLens(modifier = Modifier.size(280.dp, 140.dp)) {
 *     Text("Liquid Glass asli", color = Color.White)
 * }
 * ```
 */
@Composable
fun GlassLens(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.10f),
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.5.dp,
    lensZoneWidth: Dp = 18.dp,
    refraction: Float = 0.9f,
    borderStrength: Float = 0.8f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        // Layer 1: efek lensa. Sama seperti GlassSurface, dipasang di layer terpisah
        // (matchParentSize) supaya konten di layer 2 TIDAK ikut terdistorsi/terpotong.
        Box(
            modifier = Modifier
                .matchParentSize()
                .glassLens(
                    tint = tint,
                    cornerRadius = cornerRadius,
                    borderWidth = borderWidth,
                    lensZoneWidth = lensZoneWidth,
                    refraction = refraction,
                    borderStrength = borderStrength,
                )
        )

        // Layer 2: konten, diberi padding sebesar zona lensa supaya jatuh di zona core
        // yang tenang (lihat catatan ruang konten di dokumentasi kelas).
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(lensZoneWidth),
        ) {
            content()
        }
    }
}
