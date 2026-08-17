package com.ascentkit.core.blob

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import com.ascentkit.core.rememberBatterySaverActive

/**
 * Menghasilkan nilai phase yang berjalan terus (dalam radian per detik `speed`),
 * dipakai sebagai parameter `phase` di [BlobShape] agar tepinya "bernapas".
 *
 * @param speed kecepatan animasi dalam radian/detik. ~0.6f - 1.2f terasa paling natural
 *              (lambat & cair, bukan gemetar cepat).
 * @param enabled jika false, phase berhenti di nilai terakhir (hemat baterai / state statis).
 * @param respectBatterySaver jika true (default), animasi otomatis berhenti saat device
 *              dalam mode hemat baterai, terlepas dari nilai [enabled].
 */
@Composable
fun rememberBlobPhase(
    speed: Float = 0.8f,
    enabled: Boolean = true,
    respectBatterySaver: Boolean = true,
): Float {
    var phase by remember { mutableFloatStateOf(0f) }

    val batterySaverActive = if (respectBatterySaver) rememberBatterySaverActive() else false
    val inPreview = LocalInspectionMode.current
    val shouldAnimate = enabled && !batterySaverActive && !inPreview

    if (shouldAnimate) {
        LaunchedEffect(speed) {
            val startMillis = withInfiniteAnimationFrameMillis { it }
            while (true) {
                withInfiniteAnimationFrameMillis { nowMillis ->
                    val elapsedSeconds = (nowMillis - startMillis) / 1000f
                    phase = elapsedSeconds * speed
                }
            }
        }
    }

    return phase
}
