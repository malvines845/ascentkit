package com.ascentkit.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet modal berbahan kaca, muncul/hilang lewat kontrol eksternal (bukan
 * drag/swipe — untuk gestur drag, gunakan komponen bottom sheet dari Material/Material3
 * dan bungkus kontennya dengan [GlassSurface]).
 *
 * Ditutup dengan tap di area scrim (luar sheet) atau memanggil [onDismissRequest]
 * secara programatik (mis. dari tombol close di dalam konten).
 *
 * Contoh:
 * ```
 * var showSheet by remember { mutableStateOf(false) }
 *
 * GlassBottomSheet(
 *     visible = showSheet,
 *     onDismissRequest = { showSheet = false },
 * ) {
 *     Text("Isi bottom sheet", color = Color.White)
 * }
 * ```
 *
 * @param visible          kontrol tampil/sembunyi dari luar (state hoisting).
 * @param onDismissRequest dipanggil saat area scrim (luar sheet) di-tap. Sheet TIDAK
 *                         otomatis tersembunyi — pemanggil bertanggung jawab meng-update
 *                         [visible] sebagai respons terhadap callback ini.
 * @param scrimColor       warna overlay gelap di belakang sheet.
 * @param showHandle       jika true, tampilkan handle bar kecil di atas sheet (indikator
 *                         visual saja, bukan draggable — lihat catatan di atas).
 */
@Composable
fun GlassBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    blurRadius: Float = 32f,
    tint: Color = Color.White.copy(alpha = 0.16f),
    cornerRadius: Float = 28f,
    shape: Shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
    intensity: Float = 0.35f,
    scrimColor: Color = Color.Black.copy(alpha = 0.45f),
    showHandle: Boolean = true,
    animate: Boolean = true,
    respectBatterySaver: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        // Scrim: menutupi seluruh layar, tap di mana pun di sini memicu dismiss.
        // interactionSource tanpa indication supaya tap di scrim tidak menampilkan
        // ripple yang aneh menutupi seluruh layar.
        val scrimInteractionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickable(
                    interactionSource = scrimInteractionSource,
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = tween(250),
                    initialOffsetY = { fullHeight -> fullHeight },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { fullHeight -> fullHeight },
                ),
            ) {
                // Bungkus sheet dengan clickable no-op (indication = null) supaya tap di
                // dalam sheet dikonsumsi di sini dan TIDAK diteruskan ke scrim di
                // belakangnya (yang akan salah memicu dismiss). pointerInput kosong tidak
                // cukup untuk mengonsumsi klik — clickable dengan onClick no-op diperlukan
                // agar event benar-benar berhenti di sini.
                val sheetInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = sheetInteractionSource,
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    GlassSurface(
                        modifier = modifier
                            .fillMaxWidth()
                            .clip(shape),
                        blurRadius = blurRadius,
                        tint = tint,
                        shape = shape,
                        intensity = intensity,
                        animate = animate,
                        respectBatterySaver = respectBatterySaver,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (showHandle) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 12.dp)
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.4f)),
                                )
                            }
                            content()
                        }
                    }
                }
            }
        }
    }
}
