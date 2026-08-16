package com.liquidglass.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.core.GlassSurface
import com.liquidglass.core.blob.GlassBlob
import com.liquidglass.core.rememberGlassTier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LiquidGlassDemoScreen()
                }
            }
        }
    }
}

@Composable
fun LiquidGlassDemoScreen() {
    val tier = rememberGlassTier()

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
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
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
                        text = "Liquid Glass",
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
    }
}
