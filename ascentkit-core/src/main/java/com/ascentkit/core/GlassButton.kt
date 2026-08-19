package com.ascentkit.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Tombol berbahan kaca dengan feedback tekan (press animation): mengecil sedikit
 * (scale) dan intensitas distorsi shader naik sesaat saat ditekan, mensimulasikan
 * kaca yang "ditekan" secara fisik.
 *
 * Sengaja TIDAK menggunakan ripple dari androidx.compose.material/material3 — library
 * ini dirancang independen dari sistem desain manapun (lihat catatan di README), jadi
 * feedback visual utamanya adalah animasi scale + intensity di atas. `clickable` di
 * bawah tetap memakai indication default dari LocalIndication (biasanya ripple platform
 * bawaan Compose Foundation), tanpa perlu depend ke artifact Material tambahan. Jika
 * proyek konsumen sudah memakai Material/Material3, ripple tema mereka otomatis berlaku
 * di sini karena LocalIndication diwariskan dari atas.
 *
 * Contoh:
 * ```
 * GlassButton(onClick = { /* aksi */ }) {
 *     Text("Tekan aku", color = Color.White)
 * }
 * ```
 *
 * @param pressedScale        skala saat ditekan (1f = tanpa perubahan). 0.94f-0.97f terasa
 *                             halus tanpa berlebihan.
 * @param pressedIntensityBoost tambahan intensitas distorsi shader saat ditekan (0f - 1f),
 *                             ditambahkan ke atas [intensity]. Hanya berlaku di tier FULL.
 * @param enabled              jika false, tombol tidak merespons tap maupun menampilkan
 *                             animasi tekan.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    blurRadius: Float = 22f,
    tint: Color = Color.White.copy(alpha = 0.22f),
    cornerRadius: Float = 20f,
    shape: Shape = RoundedCornerShape(cornerRadius),
    intensity: Float = 0.4f,
    pressedScale: Float = 0.96f,
    pressedIntensityBoost: Float = 0.2f,
    animate: Boolean = true,
    respectBatterySaver: Boolean = true,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // spring() dipilih daripada tween(): memberi sedikit "pantulan" natural yang
    // terasa seperti bahan elastis (kaca cair), bukan gerakan linear kaku.
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "glassButtonScale",
    )
    val currentIntensity by animateFloatAsState(
        targetValue = if (isPressed && enabled) intensity + pressedIntensityBoost else intensity,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "glassButtonIntensity",
    )

    GlassSurface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            ),
        blurRadius = blurRadius,
        tint = tint,
        shape = shape,
        intensity = currentIntensity,
        animate = animate,
        respectBatterySaver = respectBatterySaver,
        content = content,
    )
}
