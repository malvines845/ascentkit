package com.ascentkit.core

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.ascentkit.core.shader.LiquidMorphShader

/**
 * Merakit RenderEffect final yang dipasang ke graphicsLayer, sesuai tier device.
 *
 * FULL      -> blur lalu dilewatkan ke RuntimeShader (AGSL) untuk distorsi/refraction
 * BLUR_ONLY -> blur polos, tanpa shader
 * STATIC    -> null (tidak ada RenderEffect; ditangani via tint di GlassModifier)
 *
 * PENTING (optimasi): instance ini stateful dan menyimpan [RuntimeShader] yang di-cache.
 * Kompilasi RuntimeShader (parsing AGSL -> program GPU) mahal; membuatnya ulang tiap frame
 * (mis. di dalam lambda graphicsLayer yang re-run tiap kali `timeSeconds` berubah) akan
 * membebani CPU/GC tanpa perlu, karena source shader tidak pernah berubah — hanya uniform-nya.
 *
 * Satu instance factory harus dipakai per surface (di-remember di composable pemanggil),
 * BUKAN dibuat ulang tiap frame maupun dipakai sebagai singleton lintas-surface (ukuran
 * surface berbeda-beda perlu instance RuntimeShader terpisah).
 */
internal class LiquidMorphEffectFactory {

    // Lazy: shader baru dikompilasi saat pertama kali benar-benar dibutuhkan (tier FULL,
    // API 33+), bukan langsung saat factory dibuat. Device di tier lain tidak pernah
    // membayar biaya kompilasi ini sama sekali.
    private var cachedShader: RuntimeShader? = null

    @RequiresApi(Build.VERSION_CODES.S)
    fun build(
        tier: GlassTier,
        blurRadius: Float,
        widthPx: Float,
        heightPx: Float,
        timeSeconds: Float,
        touchX: Float,
        touchY: Float,
        intensity: Float,
    ): androidx.compose.ui.graphics.RenderEffect? {
        // Skip semua pekerjaan RenderEffect kalau blur dimatikan eksplisit (blurRadius <= 0),
        // supaya dev bisa menonaktifkan efek tanpa harus mengganti seluruh composable.
        if (blurRadius <= 0f) return null

        val blur = RenderEffect.createBlurEffect(
            blurRadius, blurRadius, Shader.TileMode.CLAMP
        )

        if (tier != GlassTier.FULL || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // BLUR_ONLY, atau FULL tapi device ternyata < 33 (safety net) -> blur polos
            return blur.asComposeRenderEffect()
        }

        return buildFullEffect(blur, widthPx, heightPx, timeSeconds, touchX, touchY, intensity)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun buildFullEffect(
        blur: RenderEffect,
        widthPx: Float,
        heightPx: Float,
        timeSeconds: Float,
        touchX: Float,
        touchY: Float,
        intensity: Float,
    ): androidx.compose.ui.graphics.RenderEffect {
        // Kompilasi hanya sekali per instance factory; frame berikutnya hanya update uniform,
        // yang jauh lebih murah (tidak ada parsing/compile ulang di GPU driver).
        val shader = cachedShader ?: RuntimeShader(LiquidMorphShader.SOURCE).also {
            cachedShader = it
        }

        shader.setFloatUniform("uSize", widthPx, heightPx)
        shader.setFloatUniform("uTime", timeSeconds)
        shader.setFloatUniform("uTouch", touchX, touchY)
        shader.setFloatUniform("uIntensity", intensity)
        shader.setInputShader("composable", blur)

        val combined = RenderEffect.createRuntimeShaderEffect(shader, "composable")
        return combined.asComposeRenderEffect()
    }
}
