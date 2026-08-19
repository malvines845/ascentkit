package com.ascentkit.core.lens

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Merakit RenderEffect untuk efek glass lens distortion — TIDAK memakai blur sama sekali,
 * berbeda dari [com.ascentkit.core.LiquidMorphEffectFactory]. Hanya tersedia di tier FULL
 * (API 33+, butuh RuntimeShader/AGSL); di bawah itu, [GlassLens] jatuh ke tampilan statis
 * tanpa distorsi (lihat penanganan tier di GlassLens.kt).
 *
 * Seperti factory lain di library ini, RuntimeShader di-cache satu kali per instance
 * (satu instance per surface, di-remember oleh pemanggil) untuk menghindari kompilasi
 * shader ulang tiap frame.
 */
internal class GlassLensEffectFactory {

    private var cachedShader: RuntimeShader? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun build(
        widthPx: Float,
        heightPx: Float,
        cornerRadiusPx: Float,
        borderWidthPx: Float,
        lensWidthPx: Float,
        refraction: Float,
        borderStrength: Float,
    ): androidx.compose.ui.graphics.RenderEffect {
        val shader = cachedShader ?: RuntimeShader(GlassLensShader.SOURCE).also {
            cachedShader = it
        }

        shader.setFloatUniform("uSize", widthPx, heightPx)
        shader.setFloatUniform("uCornerRadius", cornerRadiusPx)
        shader.setFloatUniform("uBorderWidth", borderWidthPx)
        // uLensWidth adalah lebar TOTAL dari tepi (termasuk border di dalamnya), lihat
        // dokumentasi di GlassLensShader — memastikan zona lensa tidak lebih sempit dari
        // border itu sendiri, sesuai spesifikasi: lens zone harus lebih lebar dari border.
        shader.setFloatUniform("uLensWidth", lensWidthPx)
        shader.setFloatUniform("uRefraction", refraction)
        shader.setFloatUniform("uBorderStrength", borderStrength)

        val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
        return shaderEffect.asComposeRenderEffect()
    }
}
