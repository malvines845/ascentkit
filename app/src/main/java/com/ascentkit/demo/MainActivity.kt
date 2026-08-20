package com.ascentkit.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascentkit.core.GlassBottomSheet
import com.ascentkit.core.GlassButton
import com.ascentkit.core.GlassSurface
import com.ascentkit.core.blob.GlassBlob
import com.ascentkit.core.lens.GlassLens
import com.ascentkit.core.rememberGlassTier
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LiquidMorphDemoScreen()
                }
            }
        }
    }
}

/**
 * Latar belakang untuk menguji blur secara visual: pola garis + kotak-kotak kontras
 * tinggi (bukan gradient polos). Kalau blur beneran mengaburkan konten di baliknya,
 * garis-garis ini akan tampak kabur/melebur di balik kartu kaca. Kalau efeknya cuma
 * tint/warna solid tanpa blur sungguhan, garis-garis ini akan tetap tampak tajam
 * (cuma warnanya jadi agak pudar), yang menandakan efeknya BUKAN blur asli.
 */
@Composable
private fun TestPatternBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawTestPattern()
    }
}

private fun DrawScope.drawTestPattern() {
    val cellSize = 40f
    val cols = (size.width / cellSize).roundToInt() + 1
    val rows = (size.height / cellSize).roundToInt() + 1

    // Kotak catur hitam-putih kontras tinggi, paling jelas menunjukkan blur.
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val isDark = (row + col) % 2 == 0
            drawRect(
                color = if (isDark) Color(0xFF1A1A2E) else Color(0xFFF5F5F5),
                topLeft = Offset(col * cellSize, row * cellSize),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
            )
        }
    }

    // Garis diagonal warna terang di atasnya, supaya distorsi shader (kalau ada)
    // juga terlihat sebagai lekukan pada garis lurus, bukan cuma blur kotak.
    val lineColor = Color(0xFFFF4081)
    var x = -size.height
    while (x < size.width) {
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end = Offset(x + size.height, size.height),
            strokeWidth = 6f,
        )
        x += 80f
    }
}

@Composable
fun LiquidMorphDemoScreen() {
    val tier = rememberGlassTier()
    var useTestPattern by remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Layer background: bisa ditukar antara pola uji (default, untuk verifikasi
        // blur) atau gradient halus (tampilan "asli" yang lebih representatif untuk
        // produk jadi).
        if (useTestPattern) {
            TestPatternBackground(modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6EC7),
                                Color(0xFF6E7BFF),
                                Color(0xFF00E5C7),
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f),
                        )
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // GlassButton: contoh tombol dengan press animation bawaan (scale + intensity).
            GlassButton(
                onClick = { useTestPattern = !useTestPattern },
                modifier = Modifier
                    .width(280.dp)
                    .height(56.dp),
                tint = Color.White.copy(alpha = 0.3f),
                cornerRadius = 50f,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (useTestPattern) "Tap: pola uji \u2192 gradient" else "Tap: gradient \u2192 pola uji",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
            }

            GlassButton(
                onClick = { showSheet = true },
                modifier = Modifier
                    .width(280.dp)
                    .height(56.dp),
                tint = Color.White.copy(alpha = 0.3f),
                cornerRadius = 50f,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Buka bottom sheet",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
            }

            GlassSurface(
                modifier = Modifier
                    .width(280.dp)
                    .height(140.dp),
                blurRadius = 30f,
                cornerRadius = 28f,
                intensity = 0.5f,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Liquid Morph",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tier aktif: ${tier.name}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                    )
                }
            }

            // GlassLens: varian eksperimental TANPA BLUR, memakai distorsi refraksi
            // bergaya lensa fisik + rim light di border. Dibandingkan langsung dengan
            // kartu GlassSurface (Liquid Morph) di atas: perhatikan pola uji di
            // belakangnya "membelok/membesar" di tepi kartu ini, alih-alih kabur seperti
            // di atas.
            GlassLens(
                modifier = Modifier
                    .width(280.dp)
                    .height(140.dp),
                cornerRadius = 28.dp,
                borderWidth = 2.dp,
                lensZoneWidth = 36.dp,
                refraction = 0.55f,
                borderStrength = 1f,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Glass Lens",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tanpa blur \u2014 distorsi lensa",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                    )
                }
            }

            GlassSurface(
                modifier = Modifier
                    .width(280.dp)
                    .height(70.dp),
                blurRadius = 20f,
                tint = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Fase 1: Blur + Tint",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            GlassBlob(
                modifier = Modifier.width(160.dp).height(160.dp),
                blurRadius = 26f,
                wobbleAmplitude = 0.07f,
                wobbleSpeed = 0.9f,
                cornerPct = 0.6f,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Fase 3\nBlob",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        // GlassBottomSheet: contoh modal berbahan kaca, ditutup dengan tap area luar
        // (scrim) atau tombol close di dalam konten.
        GlassBottomSheet(
            visible = showSheet,
            onDismissRequest = { showSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Bottom Sheet Kaca",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Tap di luar area ini atau tombol di bawah untuk menutup.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                )
                GlassButton(
                    onClick = { showSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    tint = Color.White.copy(alpha = 0.25f),
                    cornerRadius = 14f,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Tutup", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
