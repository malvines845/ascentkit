package com.liquidglass.core

import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Modifier liquid glass, tier-aware:
 * - FULL      (API 33+) -> blur + distorsi shader AGSL, animasi berjalan, respons sentuh
 * - BLUR_ONLY (API 31-32) -> blur real-time polos, tanpa distorsi
 * - STATIC    (< API 31) -> fallback tint datar
 *
 * @param blurRadius   radius blur, wajar-nya 16f - 48f. Nilai <= 0 menonaktifkan seluruh
 *                     RenderEffect (blur & shader) tanpa perlu mengganti composable.
 * @param tint         warna kaca; alpha rendah (mis. 0.15f - 0.3f) yang paling natural
 * @param cornerRadius radius sudut membulat kaca
 * @param intensity    kekuatan distorsi shader (0f - 1f). Hanya berlaku di tier FULL.
 * @param animate      jika true, gelombang shader terus bergerak. Set false untuk hemat baterai.
 * @param respectBatterySaver jika true (default), animasi otomatis berhenti saat device dalam
 *                     mode hemat baterai, terlepas dari nilai [animate]. Set false untuk selalu
 *                     memaksa animasi berjalan (jarang diperlukan; pertimbangkan dampak baterai).
 */
fun Modifier.liquidGlass(
    blurRadius: Float = 24f,
    tint: Color = Color.White.copy(alpha = 0.18f),
    cornerRadius: Float = 24f,
    shape: Shape = RoundedCornerShape(cornerRadius),
    intensity: Float = 0.4f,
    animate: Boolean = true,
    respectBatterySaver: Boolean = true,
): Modifier = this
    .clip(shape)
    .then(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.composed {
                val tier = rememberGlassTier()
                var sizePx by remember { mutableStateOf(Offset(1f, 1f)) }
                var touchPx by remember { mutableStateOf(Offset.Zero) }
                var timeSeconds by remember { mutableFloatStateOf(0f) }

                // Cache satu instance factory per surface (bukan per frame). Instance ini
                // menyimpan RuntimeShader yang sudah dikompilasi, jadi frame berikutnya hanya
                // meng-update uniform, bukan mengompilasi ulang shader dari nol.
                val effectFactory = remember { LiquidGlassEffectFactory() }

                val batterySaverActive = if (respectBatterySaver) {
                    rememberBatterySaverActive()
                } else {
                    false
                }

                // Preview Compose (Android Studio) tidak menjalankan frame clock sungguhan;
                // hindari LaunchedEffect infinite loop di sana.
                val inPreview = LocalInspectionMode.current
                val shouldAnimate = animate && tier.supportsShader &&
                    !batterySaverActive && !inPreview

                if (shouldAnimate) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        val start = withInfiniteAnimationFrameMillis { it }
                        while (true) {
                            withInfiniteAnimationFrameMillis { now ->
                                timeSeconds = (now - start) / 1000f
                            }
                        }
                    }
                }

                this
                    .onGloballySized { w, h -> sizePx = Offset(w, h) }
                    .then(
                        if (tier.supportsShader && blurRadius > 0f) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        touchPx = offset
                                    }
                                )
                            }
                        } else Modifier
                    )
                    .then(
                        // graphicsLayer offscreen hanya dibutuhkan kalau ada RenderEffect untuk
                        // dipasang. Kalau blur dimatikan (blurRadius <= 0), lewati sepenuhnya —
                        // menghindari compositing layer offscreen yang sia-sia (mahal di GPU
                        // terutama saat banyak GlassSurface tampil sekaligus, mis. dalam list).
                        if (blurRadius > 0f) {
                            Modifier.graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                renderEffect = effectFactory.build(
                                    tier = tier,
                                    blurRadius = blurRadius,
                                    widthPx = sizePx.x,
                                    heightPx = sizePx.y,
                                    timeSeconds = timeSeconds,
                                    touchX = touchPx.x,
                                    touchY = touchPx.y,
                                    intensity = intensity,
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .background(tint, shape)
            }
        } else {
            // Fallback API < 31: nggak ada blur real-time yang murah,
            // jadi kompensasi dengan tint sedikit lebih pekat biar tetap kebaca sebagai "kaca"
            Modifier.background(tint.copy(alpha = (tint.alpha + 0.12f).coerceAtMost(1f)), shape)
        }
    )

/**
 * Helper: tier efek yang aktif untuk device saat ini, di-remember supaya
 * nggak recompute tiap recomposition.
 */
@androidx.compose.runtime.Composable
fun rememberGlassTier(): GlassTier = remember { GlassCapability.currentTier() }

/**
 * Modifier kecil buat nangkep ukuran layout dalam px, dipakai buat uniform uSize di shader.
 */
private fun Modifier.onGloballySized(onSized: (width: Float, height: Float) -> Unit): Modifier =
    this.then(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            onSized(placeable.width.toFloat(), placeable.height.toFloat())
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    )
