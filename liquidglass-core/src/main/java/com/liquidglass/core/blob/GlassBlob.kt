package com.liquidglass.core.blob

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.liquidglass.core.GlassSurface

/**
 * Varian [GlassSurface] dengan tepi organik yang "bernapas" (morphing blob),
 * alih-alih sudut membulat statis. Cocok untuk elemen dekoratif atau
 * highlight — kurang cocok untuk kartu berisi teks panjang/rapi karena
 * tepinya terus bergerak.
 *
 * Contoh:
 * ```
 * GlassBlob(modifier = Modifier.size(200.dp)) {
 *     Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
 * }
 * ```
 *
 * @param wobbleAmplitude seberapa jauh tepi menyimpang dari bentuk dasar (0f - 1f).
 *                        0.04f-0.08f terasa halus & elegan; di atas 0.12f mulai terlihat "berdenyut" kasar.
 * @param wobbleSpeed     kecepatan animasi napas, dalam radian/detik.
 * @param cornerPct       kebulatan bentuk dasar sebelum wobble diterapkan (0f = kotakish, 1f = oval penuh).
 * @param controlPoints   jumlah titik kontrol outline; naikkan untuk detail lebih halus (biaya render lebih tinggi).
 * @param respectBatterySaver jika true (default), wobble & shader berhenti otomatis saat
 *                        device dalam mode hemat baterai.
 */
@Composable
fun GlassBlob(
    modifier: Modifier = Modifier,
    blurRadius: Float = 24f,
    tint: Color = Color.White.copy(alpha = 0.18f),
    intensity: Float = 0.4f,
    wobbleAmplitude: Float = 0.06f,
    wobbleSpeed: Float = 0.8f,
    cornerPct: Float = 0.3f,
    controlPoints: Int = 24,
    animate: Boolean = true,
    respectBatterySaver: Boolean = true,
    content: @Composable Box.() -> Unit = {},
) {
    val phase = rememberBlobPhase(
        speed = wobbleSpeed,
        enabled = animate,
        respectBatterySaver = respectBatterySaver,
    )

    val shape = BlobShape(
        points = controlPoints,
        baseCornerPct = cornerPct,
        amplitude = wobbleAmplitude,
        phase = phase,
    )

    GlassSurface(
        modifier = modifier,
        blurRadius = blurRadius,
        tint = tint,
        shape = shape,
        intensity = intensity,
        animate = animate,
        respectBatterySaver = respectBatterySaver,
        content = content,
    )
}
