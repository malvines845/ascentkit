package com.ascentkit.core.container

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset

/**
 * Registry posisi setiap glass child (mis. [com.ascentkit.core.lens.GlassLens]) relatif
 * terhadap [GlassContainer] terdekat yang membungkusnya, di-key dengan identitas unik
 * per instance child (mis. hasil `remember { Any() }` di sisi pemanggil).
 *
 * ## Mekanisme distribusi posisi (ringkasan hasil verifikasi arsitektur)
 *
 * Setiap glass child MENULIS posisinya sendiri ke [SnapshotStateMap] ini dari dalam
 * `Modifier.onPlaced` miliknya sendiri (lihat [trackPositionInContainer]) — posisi ini
 * didapat langsung dari `LayoutCoordinates` milik child itu sendiri
 * (`it.positionInParent()`), BUKAN lewat konversi koordinat cross-hierarchy apa pun,
 * dan HANYA valid jika child tersebut adalah child LANGSUNG dari slot `content` milik
 * [GlassContainer] (lihat dokumentasi [trackPositionInContainer] untuk detail batasan
 * ini).
 *
 * Penulisan terjadi di fase PLACEMENT. Ini aman dari pola "backwards write" (menulis
 * lalu membaca ulang state yang sama di fase yang sama untuk menentukan ulang
 * ukuran/posisi diri sendiri, yang menyebabkan infinite recomposition loop) karena:
 * - Child yang menulis TIDAK PERNAH membaca kembali peta ini untuk menentukan placement
 *   dirinya sendiri — ia hanya menulis, tanpa bergantung pada isi peta sebelumnya.
 * - Consumer peta ini (mis. proses yang merakit uniform shader berdasarkan posisi
 *   sebuah glass) adalah kode yang berjalan di composable/fase LAIN, bukan di fase
 *   placement milik glass yang sama.
 *
 * [SnapshotStateMap] dipilih (bukan `MutableMap` biasa) supaya perubahan padanya tetap
 * dapat diobservasi secara reaktif oleh composable yang membaca isinya, sesuai
 * infrastruktur Snapshot System bawaan Compose.
 */
internal val LocalGlassPositionRegistry = compositionLocalOf<SnapshotStateMap<Any, Offset>> {
    error(
        "LocalGlassPositionRegistry tidak disediakan. Composable ini harus berada di " +
            "dalam GlassContainer untuk memakai fitur sampling posisi bersama."
    )
}
