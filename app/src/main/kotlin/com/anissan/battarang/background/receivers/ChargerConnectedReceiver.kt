package com.anissan.battarang.background.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChargerConnectedReceiver : BroadcastReceiver(), KoinComponent {
  private val batteryLowReceiver: BatteryLowReceiver by inject()

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null) return

    val action = intent?.action ?: return
    logV { """Received the Intent Action: "$action"""" }

    when (action) {
      Intent.ACTION_POWER_CONNECTED -> {
        context.unregisterReceiver(this)

        ContextCompat.registerReceiver(
          context,
          batteryLowReceiver,
          IntentFilter(Intent.ACTION_BATTERY_LOW),
          ContextCompat.RECEIVER_NOT_EXPORTED,
        )
      }

      else -> logE { "$action is not a supported action by this receiver" }
    }
  }
}
