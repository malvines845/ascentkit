package com.ascentkit.core.shader

/**
 * Source AGSL untuk efek "liquid morph": membelokkan sample warna di sekitar
 * tepi surface, mensimulasikan refraksi kaca cair.
 *
 * Hanya kompatibel dengan RuntimeShader (API 33+).
 * Uniform yang wajib di-set dari sisi Kotlin sebelum dipakai:
 *   - uSize      (float2)  -> lebar & tinggi surface dalam px
 *   - uTime      (float)   -> waktu berjalan, untuk animasi cair
 *   - uTouch     (float2)  -> posisi sentuhan terakhir (px), untuk ripple
 *   - uIntensity (float)   -> kekuatan distorsi, 0.0 - 1.0
 */
object LiquidMorphShader {

    const val SOURCE = """
        uniform shader composable;
        uniform float2 uSize;
        uniform float uTime;
        uniform float2 uTouch;
        uniform float uIntensity;

        half4 main(float2 coord) {
            float2 uv = coord / uSize;

            // Distorsi dasar: gelombang sinus halus mensimulasikan permukaan cair
            float wave = sin(uv.x * 12.0 + uTime * 1.5) * cos(uv.y * 10.0 + uTime * 1.2);
            float2 offset = float2(wave, wave) * (2.0 * uIntensity);

            // Tambahan riak dari titik sentuh (ripple menjauh dari uTouch)
            float2 touchUv = uTouch / uSize;
            float dist = distance(uv, touchUv);
            float ripple = sin(dist * 40.0 - uTime * 6.0) * exp(-dist * 6.0);
            offset += float2(ripple, ripple) * uIntensity;

            float2 sampleCoord = coord + offset;
            return composable.eval(sampleCoord);
        }
    """
}
