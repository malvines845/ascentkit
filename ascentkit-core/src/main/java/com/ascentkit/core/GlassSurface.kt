package com.ascentkit.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Composable siap-pakai untuk permukaan kaca cair.
 * Bungkus konten apa pun (teks, ikon, tombol) di dalamnya.
 *
 * Contoh:
 * ```
 * GlassSurface(modifier = Modifier.size(200.dp, 100.dp)) {
 *     Text("Halo dari balik kaca", color = Color.White)
 * }
 * ```
 *
 * PENTING (arsitektur): blur/shader hanya dipasang pada layer background terpisah
 * (`Modifier.matchParentSize().liquidMorph(...)`), BUKAN pada Box terluar yang juga
 * menjadi induk dari [content]. `RenderEffect` mem-blur seluruh isi layer tempat ia
 * dipasang — kalau dipasang di layer yang sama dengan teks/ikon, teks itu ikut ke-blur
 * dan jadi tidak terbaca. Dengan struktur dua-layer ini, blur hanya mengenai background,
 * sementara [content] digambar di layer terpisah di atasnya, tetap tajam.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    blurRadius: Float = 24f,
    tint: Color = Color.White.copy(alpha = 0.18f),
    cornerRadius: Float = 24f,
    shape: Shape = RoundedCornerShape(cornerRadius),
    intensity: Float = 0.4f,
    animate: Boolean = true,
    respectBatterySaver: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        // Layer 1: background kaca. Blur/shader HANYA diterapkan di sini.
        Box(
            modifier = Modifier
                .matchParentSize()
                .liquidMorph(
                    blurRadius = blurRadius,
                    tint = tint,
                    cornerRadius = cornerRadius,
                    shape = shape,
                    intensity = intensity,
                    animate = animate,
                    respectBatterySaver = respectBatterySaver,
                )
        )

        // Layer 2: konten (teks, ikon, dll). Digambar di layer terpisah tanpa
        // RenderEffect apapun, sehingga tetap tajam meski background di baliknya blur.
        content()
    }
}
