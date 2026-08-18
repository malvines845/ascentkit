package com.ascentkit.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
    Box(
        modifier = modifier.liquidMorph(
            blurRadius = blurRadius,
            tint = tint,
            cornerRadius = cornerRadius,
            shape = shape,
            intensity = intensity,
            animate = animate,
            respectBatterySaver = respectBatterySaver,
        ),
        content = content,
    )
}
