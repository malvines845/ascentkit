package com.ascentkit.core.lens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascentkit.core.GlassCapability
import com.ascentkit.core.GlassTier

/**
 * Modifier "glass lens distortion" — BERBEDA secara fundamental dari
 * [com.ascentkit.core.liquidMorph]: TIDAK ADA BLUR SAMA SEKALI. Sebagai gantinya, konten
 * di belakang surface disample dengan pembelokan (refraksi) yang menguat mendekati tepi,
 * mensimulasikan cahaya yang dibengkokkan lewat kaca fisik — bukan diburamkan.
 *
 * Tiga zona konsentris, dari tepi ke tengah (lihat [GlassLensShader] untuk detail matematis):
 *  1. Border zone (sempit)       -> rim light: garis highlight tipis di tepi.
 *  2. Lens zone (di dalam border, lebih lebar dari border tapi tetap sempit dibanding
 *     surface secara keseluruhan) -> distorsi lensa kuat, mengikuti lengkungan border.
 *  3. Core zone (paling luas)    -> kaca tenang nyaris tanpa distorsi — ruang aman untuk
 *     konten/teks agar tetap mudah dibaca.
 *
 * Hanya berfungsi penuh di tier FULL (API 33+, butuh RuntimeShader/AGSL). Di bawah itu,
 * modifier ini jatuh ke tampilan statis: tint tipis + border digambar langsung (tanpa
 * shader), supaya tetap terlihat seperti kaca meski tanpa distorsi real-time.
 *
 * @param tint            warna dasar kaca di zona core, alpha rendah.
 * @param cornerRadius    radius sudut membulat, dalam dp.
 * @param borderWidth     lebar zona border/rim light, dalam dp. 1dp-3dp biasanya paling
 *                        realistis (tepi kaca fisik tipis).
 * @param lensZoneWidth   lebar TOTAL dari tepi luar sampai batas dalam zona lensa (mencakup
 *                        border di dalamnya), dalam dp. HARUS lebih besar dari [borderWidth]
 *                        — selisihnya adalah lebar murni zona distorsi lensa. Core zone
 *                        mengisi sisa ruang di tengah, di luar [lensZoneWidth].
 * @param refraction      kekuatan pembelokan/pembesaran di zona lensa. Skala proporsional
 *                        terhadap [lensZoneWidth]; rentang wajar 0.15f - 0.6f. Nilai di
 *                        atas ~0.8f mulai terlihat pecah/artifact.
 * @param borderStrength  opasitas rim light (0f - 1f).
 */
fun Modifier.glassLens(
    tint: Color = Color.White.copy(alpha = 0.10f),
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.5.dp,
    lensZoneWidth: Dp = 18.dp,
    refraction: Float = 0.35f,
    borderStrength: Float = 0.8f,
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val tier = remember { GlassCapability.currentTier() }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && tier == GlassTier.FULL) {
        var sizePx by remember { mutableStateOf(Offset(1f, 1f)) }
        val effectFactory = remember { GlassLensEffectFactory() }
        val density = LocalDensity.current

        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val borderWidthPx = with(density) { borderWidth.toPx() }
        // uLensWidth adalah lebar total dari tepi (bukan hanya lebar zona lensa murni),
        // sehingga secara desain SELALU >= borderWidthPx: zona lensa mencakup border di
        // dalamnya lalu meluas lebih jauh ke arah tengah, sesuai spesifikasi "lens zone
        // lebih lebar dari border, tapi tidak sesempit border".
        val lensWidthPx = with(density) {
            maxOf(lensZoneWidth.toPx(), borderWidthPx + 1f)
        }

        this
            .clip(shape)
            .onGloballySized { w, h -> sizePx = Offset(w, h) }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                renderEffect = effectFactory.build(
                    widthPx = sizePx.x,
                    heightPx = sizePx.y,
                    cornerRadiusPx = cornerRadiusPx,
                    borderWidthPx = borderWidthPx,
                    lensWidthPx = lensWidthPx,
                    refraction = refraction,
                    borderStrength = borderStrength,
                )
            }
            .background(tint, shape)
    } else {
        // Fallback (< API 33 atau tier bukan FULL): tanpa shader/distorsi sama sekali.
        // Tint + border digambar langsung supaya tetap terbaca sebagai "kaca", meski statis.
        this
            .clip(shape)
            .background(tint, shape)
            .border(
                width = borderWidth,
                color = Color.White.copy(alpha = 0.35f * borderStrength),
                shape = shape,
            )
    }
}

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
