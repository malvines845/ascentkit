package com.ascentkit.core.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onPlaced

/**
 * Menghasilkan [Modifier] yang melacak posisi composable saat ini relatif terhadap
 * parent langsungnya, dan menyediakan offset itu lewat [onPosition].
 *
 * PERSYARATAN WAJIB (harus dipenuhi pemanggil): composable yang memakai modifier ini
 * HARUS menjadi child LANGSUNG dari slot `content` milik [GlassContainer] — tanpa Box,
 * Column, atau layout pembungkus lain di antaranya. `it.positionInParent()` di dalam
 * [androidx.compose.ui.layout.onPlaced] mengembalikan posisi relatif terhadap PARENT
 * LANGSUNG composable ini secara native (bukan lewat konversi koordinat cross-hierarchy
 * apa pun) — jadi jika ada layout pembungkus tambahan di antara [GlassContainer] dan
 * composable ini, offset yang dihasilkan hanya relatif terhadap pembungkus tersebut,
 * BUKAN terhadap [GlassContainer].
 *
 * PENTING (kinerja): sengaja memakai [androidx.compose.ui.layout.onPlaced], BUKAN
 * `onGloballyPositioned`. `onGloballyPositioned` dirancang untuk memberi koordinat GLOBAL
 * (root/window), yang mengharuskan traversal seluruh UI tree beserta perkalian matriks
 * untuk setiap parent di antaranya. `onPlaced` memberi `LayoutCoordinates` milik
 * composable itu sendiri tepat setelah placement, dari situ `positionInParent()`
 * dipanggil sebagai method pada `LayoutCoordinates` tersebut — bukan top-level function
 * yang perlu di-import terpisah, dan bukan API yang butuh menaiki hierarchy ke root.
 *
 * PENTING (lifecycle/keamanan state): callback [onPosition] dipanggil dari fase
 * PLACEMENT, bukan fase composition. Pemanggil bertanggung jawab menyimpan/menyalurkan
 * nilai ini lewat cara yang tidak membaca-ulang nilai yang sama pada fase yang sama untuk
 * menentukan ulang posisi/ukuran composable ini sendiri (pola "backwards write" yang
 * harus dihindari) — di [GlassContainer], nilai ini hanya ditulis ke peta posisi dan
 * dibaca oleh composable LAIN (proses sampling shader), bukan dibaca-ulang oleh
 * composable yang sama untuk menentukan placement-nya sendiri.
 */
internal fun Modifier.trackPositionInContainer(
    onPosition: (Offset) -> Unit,
): Modifier = this.onPlaced { coordinates ->
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
