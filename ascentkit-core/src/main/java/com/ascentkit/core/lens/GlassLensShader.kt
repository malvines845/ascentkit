package com.ascentkit.core.lens

/**
 * Source AGSL untuk "glass lens distortion" — BERBEDA dari [com.ascentkit.core.shader.LiquidMorphShader]:
 * shader ini TIDAK melakukan blur sama sekali. Sebagai gantinya, konten di belakang kaca
 * disample dengan offset yang membesar mendekati tepi, mensimulasikan pembelokan cahaya
 * lewat lensa kaca fisik (refraksi), bukan pengaburan.
 *
 * Struktur tiga zona konsentris, dihitung dari SDF (signed distance field) rounded-rect
 * yang mengikuti bentuk & radius sudut surface yang sebenarnya:
 *
 *   1. BORDER ZONE  (paling luar, sempit)   -> rim light: garis highlight tipis semi-transparan,
 *                                              mensimulasikan pantulan cahaya di tepi kaca fisik.
 *   2. LENS ZONE    (di dalam border)       -> distorsi refraksi kuat: sample offset membesar
 *                                              tajam mendekati tepi, membuat konten di baliknya
 *                                              terlihat "membelok/membesar" seperti lensa.
 *   3. CORE ZONE    (paling dalam, terluas) -> kaca tenang: distorsi sangat kecil/nihil, ruang
 *                                              aman untuk teks/konten agar tetap terbaca jelas.
 *
 * Transisi antar zona memakai smoothstep, bukan batas tegas, supaya tidak ada garis patah.
 *
 * Uniform yang wajib di-set dari sisi Kotlin:
 *   - uSize          (float2) -> lebar & tinggi surface dalam px
 *   - uCornerRadius   (float)  -> radius sudut membulat, dalam px (mengikuti shape surface)
 *   - uBorderWidth    (float)  -> lebar zona border/rim light, dalam px
 *   - uLensWidth      (float)  -> lebar TOTAL dari tepi sampai akhir zona lensa (termasuk
 *                                 border di dalamnya), dalam px. Jadi lebar zona lensa murni
 *                                 adalah (uLensWidth - uBorderWidth).
 *   - uRefraction     (float)  -> kekuatan pembesaran/pembelokan di zona lensa. Skala
 *                                 proporsional terhadap uLensWidth (lihat catatan di
 *                                 dalam shader), rentang wajar mis. 0.15 - 0.6. Nilai
 *                                 di atas ~0.8 mulai terlihat pecah/artifact karena
 *                                 sample coordinate bisa jatuh jauh di luar konten asli.
 *   - uBorderStrength (float)  -> opasitas rim light di border (0.0 - 1.0)
 */
object GlassLensShader {

    const val SOURCE = """
        uniform shader composable;
        uniform float2 uSize;
        uniform float uCornerRadius;
        uniform float uBorderWidth;
        uniform float uLensWidth;
        uniform float uRefraction;
        uniform float uBorderStrength;

        // Signed distance field untuk rounded rectangle, berpusat di origin.
        // Hasil negatif = di dalam bentuk, 0 = tepat di tepi, positif = di luar.
        float roundedBoxSDF(float2 p, float2 halfSize, float radius) {
            float2 q = abs(p) - halfSize + radius;
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
        }

        half4 main(float2 coord) {
            float2 center = uSize * 0.5;
            float2 p = coord - center;
            float2 halfSize = uSize * 0.5;

            // Jarak (px) dari titik saat ini ke tepi terdekat bentuk (rounded rect).
            // Nilai negatif berarti di dalam; kita balik tandanya jadi "jarak ke tepi"
            // yang positif di dalam bentuk, 0 tepat di tepi.
            float distToEdge = -roundedBoxSDF(p, halfSize, uCornerRadius);

            // Arah menjauhi tepi terdekat, dipakai sebagai arah pembelokan sample.
            // Didekati lewat gradient numerik SDF (arah normal permukaan).
            float eps = 1.0;
            float dx = roundedBoxSDF(p + float2(eps, 0.0), halfSize, uCornerRadius)
                     - roundedBoxSDF(p - float2(eps, 0.0), halfSize, uCornerRadius);
            float dy = roundedBoxSDF(p + float2(0.0, eps), halfSize, uCornerRadius)
                     - roundedBoxSDF(p - float2(0.0, eps), halfSize, uCornerRadius);
            float2 inwardDir = -normalize(float2(dx, dy) + 1e-5);

            // --- Zona lensa: seberapa dalam kita berada di dalam pita distorsi ---
            // 0 di tepi luar pita, 1 di batas dalam pita (menuju core).
            float lensT = clamp(distToEdge / max(uLensWidth, 0.001), 0.0, 1.0);

            // Kurva non-linear: distorsi paling kuat persis di tepi, meluruh cepat
            // mendekati core. pow() membuat peluruhannya terasa seperti lengkungan
            // lensa fisik, bukan gradasi linear yang terasa datar.
            float lensFalloff = 1.0 - smoothstep(0.0, 1.0, lensT);
            float lensStrength = pow(lensFalloff, 1.6) * uRefraction;

            // Offset sample: menarik konten dari arah dalam ke arah tepi (magnifikasi),
            // mengikuti arah normal SDF sehingga lengkungannya mengikuti kontur border,
            // termasuk di sudut-sudut membulat.
            //
            // PENTING: skala offset diikat ke uLensWidth (lebar zona distorsi), BUKAN
            // uBorderWidth (lebar rim light) — keduanya parameter dengan tujuan berbeda.
            // Border biasanya sangat tipis (mis. 1.5dp) untuk rim light yang realistis;
            // mengikat skala distorsi ke situ membuat efek lensa nyaris tak terlihat
            // berapa pun uRefraction dinaikkan, karena hasil kali selalu kecil.
            float2 sampleOffset = inwardDir * lensStrength * uLensWidth * 0.9;
            float2 sampleCoord = coord - sampleOffset;

            half4 baseColor = composable.eval(sampleCoord);

            // --- Rim light di border zone ---
            // borderT: 0 tepat di tepi luar, 1 di batas dalam border.
            float borderT = clamp(distToEdge / max(uBorderWidth, 0.001), 0.0, 1.0);
            // Puncak highlight tepat di sekitar setengah lebar border, meluruh ke
            // dua arah (ke luar tepi & ke dalam menuju lens zone).
            float rim = (1.0 - abs(borderT - 0.35) * 2.0);
            rim = clamp(rim, 0.0, 1.0);
            rim = pow(rim, 1.4) * uBorderStrength;
            // Hanya tampak di dalam border zone; di luar border (distToEdge < 0,
            // yaitu benar-benar di luar bentuk) atau jauh di lens/core, rim = 0.
            float inBorderMask = step(0.0, distToEdge) * (1.0 - smoothstep(uBorderWidth * 0.9, uBorderWidth, distToEdge));
            rim *= inBorderMask;

            half4 rimColor = half4(1.0, 1.0, 1.0, 1.0) * rim;

            return baseColor + rimColor;
        }
    """
}
