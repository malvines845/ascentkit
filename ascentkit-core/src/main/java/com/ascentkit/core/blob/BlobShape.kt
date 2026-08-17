package com.ascentkit.core.blob

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shape blob organik: lingkaran/rounded-rect yang tepinya "bernapas" —
 * radius tiap titik di sekeliling outline berosilasi mengikuti waktu,
 * menghasilkan efek permukaan cair yang terus bergerak pelan.
 *
 * @param points        jumlah titik kontrol di sekeliling outline. Lebih banyak = lebih halus tapi lebih berat.
 * @param baseCornerPct seberapa membulat bentuk dasarnya (0f = kotak tajam, 1f = penuh oval/lingkaran)
 * @param amplitude     seberapa jauh tepi menyimpang dari bentuk dasar (0f - 1f, relatif terhadap ukuran)
 * @param phase         waktu berjalan (detik), dipakai untuk animasi. Naikkan tiap frame dari luar.
 * @param frequency     berapa banyak "gelombang" mengelilingi bentuk (2-5 biasanya paling natural)
 *
 * PERFORMA: instance ini biasanya dibuat ulang tiap frame oleh pemanggil (mis. `GlassBlob`)
 * karena `phase` berubah terus. Untuk menghindari overhead alokasi yang tidak perlu di jalur
 * yang sudah dipanggil setiap frame ini, titik kontrol disimpan sebagai `FloatArray` mentah
 * (bukan `List<Offset>`) sehingga tidak ada boxing atau alokasi objek `Offset` per titik —
 * hanya satu alokasi array per instance.
 */
class BlobShape(
    private val points: Int = 24,
    private val baseCornerPct: Float = 0.3f,
    private val amplitude: Float = 0.06f,
    private val phase: Float = 0f,
    private val frequency: Float = 3f,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = buildBlobPath(size)
        return Outline.Generic(path)
    }

    private fun buildBlobPath(size: Size): Path {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Radius dasar mengikuti bentuk elips yang pas di dalam bounds,
        // supaya blob tetap proporsional untuk surface non-persegi.
        val baseRx = (size.width / 2f) * (0.7f + baseCornerPct * 0.3f)
        val baseRy = (size.height / 2f) * (0.7f + baseCornerPct * 0.3f)

        // xs/ys menyimpan koordinat titik kontrol sebagai FloatArray datar,
        // menghindari alokasi objek Offset per titik (lihat catatan performa di atas).
        val xs = FloatArray(points)
        val ys = FloatArray(points)
        val twoPi = (2f * Math.PI.toFloat())

        for (i in 0 until points) {
            val angle = (i.toFloat() / points) * twoPi

            // Osilasi radius: kombinasi beberapa frekuensi biar bentuknya
            // tidak terlihat seperti bunga simetris kaku, lebih organik.
            val wobble = amplitude * sin(frequency * angle + phase) +
                (amplitude * 0.5f) * sin((frequency * 1.7f) * angle - phase * 1.3f)

            val rx = baseRx * (1f + wobble)
            val ry = baseRy * (1f + wobble)

            xs[i] = cx + rx * cos(angle)
            ys[i] = cy + ry * sin(angle)
        }

        // Gambar path pakai Catmull-Rom -> kubik Bezier supaya kurvanya mulus
        // dan melewati tepat titik-titik kontrolnya (bukan cuma mendekati).
        path.moveTo(xs[0], ys[0])
        for (i in 0 until points) {
            val i0 = (i - 1 + points) % points
            val i2 = (i + 1) % points
            val i3 = (i + 2) % points

            val p1x = xs[i]; val p1y = ys[i]
            val p2x = xs[i2]; val p2y = ys[i2]

            val c1x = p1x + (p2x - xs[i0]) / 6f
            val c1y = p1y + (p2y - ys[i0]) / 6f
            val c2x = p2x - (xs[i3] - p1x) / 6f
            val c2y = p2y - (ys[i3] - p1y) / 6f

            path.cubicTo(c1x, c1y, c2x, c2y, p2x, p2y)
        }
        path.close()
        return path
    }
}
