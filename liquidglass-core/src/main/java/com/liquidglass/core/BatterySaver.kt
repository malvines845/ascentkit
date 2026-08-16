package com.liquidglass.core

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Mengembalikan true jika device sedang dalam mode hemat baterai (Battery Saver / Power Save Mode).
 *
 * Dipakai secara internal untuk otomatis meredam animasi liquid glass yang berat
 * (shader wave, blob wobble) ketika pengguna sudah minta perangkat berhemat daya —
 * menghormati preferensi pengguna alih-alih terus memaksakan efek visual mahal.
 *
 * State diperbarui secara reaktif via BroadcastReceiver terhadap ACTION_POWER_SAVE_MODE_CHANGED,
 * jadi jika pengguna mengubah pengaturan saat komponen sedang ditampilkan, efek langsung
 * menyesuaikan tanpa perlu restart activity.
 */
@Composable
fun rememberBatterySaverActive(): Boolean {
    val context = LocalContext.current
    var isActive by remember {
        mutableStateOf(readPowerSaveMode(context))
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                isActive = readPowerSaveMode(context)
            }
        }
        val filter = android.content.IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return isActive
}

private fun readPowerSaveMode(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isPowerSaveMode ?: false
}
