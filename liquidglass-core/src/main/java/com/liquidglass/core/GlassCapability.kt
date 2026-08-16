package com.liquidglass.core

import android.os.Build

/**
 * Tier kemampuan efek liquid glass, ditentukan otomatis dari API level device.
 *
 * FULL      (API 33+) -> blur real-time + AGSL refraction shader + morphing
 * BLUR_ONLY (API 31-32) -> blur real-time via RenderEffect, tanpa distorsi shader
 * STATIC    (< API 31) -> fallback: tint transparan datar, tanpa blur real-time
 */
enum class GlassTier {
    FULL,
    BLUR_ONLY,
    STATIC;

    val supportsBlur: Boolean
        get() = this == FULL || this == BLUR_ONLY

    val supportsShader: Boolean
        get() = this == FULL
}

object GlassCapability {

    /**
     * Deteksi tier berdasarkan Build.VERSION.SDK_INT saat ini.
     * Dipanggil sekali, hasilnya sebaiknya di-remember di level Composable.
     */
    fun currentTier(): GlassTier {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> GlassTier.FULL // 33
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> GlassTier.BLUR_ONLY   // 31
            else -> GlassTier.STATIC
        }
    }
}
