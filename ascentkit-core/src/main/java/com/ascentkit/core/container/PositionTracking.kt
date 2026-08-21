package com.ascentkit.core.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Menghasilkan [Modifier] yang melacak posisi composable saat ini relatif terhadap
 * parent langsungnya (diasumsikan slot `content` milik [GlassContainer]), dan
 * menyediakan offset itu lewat [onPosition].
 *
 * PENTING (kinerja): sengaja memakai `positionInParent()` dari `LayoutCoordinates`,
 * BUKAN `positionInRoot()` atau `localToWindow()`. Menghitung posisi relatif terhadap
 * root/window mengharuskan traversal seluruh UI tree beserta perkalian matriks untuk
 * setiap parent di antaranya — mahal, terutama bila dipanggil berulang kali (mis. saat
 * scroll). Karena kita hanya butuh posisi relatif terhadap SATU parent langsung (asumsi:
 * glass surface adalah child langsung dari `content` milik [GlassContainer]), memakai
 * `positionInParent()` membatasi biaya traversal ke satu level saja.
 *
 * Batasan: jika glass surface diletakkan lebih dalam dari satu level di bawah slot
 * `content` milik [GlassContainer] (mis. dibungkus Column/Row tambahan), offset yang
 * dihasilkan HANYA relatif terhadap parent langsungnya, bukan terhadap GlassContainer.
 * Untuk kasus ini, pemanggil perlu mengakumulasi offset tambahan secara manual, atau
 * meletakkan glass surface langsung sebagai child dari `content`.
 */
internal fun Modifier.trackPositionInContainer(
    onPosition: (Offset) -> Unit,
): Modifier = this.onGloballyPositioned { coordinates ->
    onPosition(coordinates.positionInParent())
}

/**
 * Versi composable dari [trackPositionInContainer]: mengembalikan [Offset] yang
 * ter-remember dan otomatis update saat posisi berubah.
 */
@Composable
internal fun rememberPositionInContainer(): Pair<Offset, Modifier> {
    var position by remember { mutableStateOf(Offset.Zero) }
    val modifier = Modifier.trackPositionInContainer { position = it }
    return position to modifier
}
