package com.anissan.battarang.background.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.anissan.battarang.background.receivers.handlers.BroadcastedEventHandlers
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BatteryLowReceiver : BroadcastReceiver(), KoinComponent {
  private val broadcastedEventHandlers: BroadcastedEventHandlers by inject()
  private val chargerConnectedReceiver: ChargerConnectedReceiver by inject()

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null) return

    val action = intent?.action ?: return
    logV { """Received the Intent Action: "$action"""" }

    when (action) {
      Intent.ACTION_BATTERY_LOW -> {
        // Instead of triggering this event only once, some OEMs spam this constantly.
        // So I'm unregistering this until the next charger connection.
        context.unregisterReceiver(this)

        ContextCompat.registerReceiver(
          context,
          chargerConnectedReceiver,
          IntentFilter(Intent.ACTION_POWER_CONNECTED),
          ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        broadcastedEventHandlers.notifyBatteryIsLow()
      }

      else -> logE { "$action is not a supported action by this receiver" }
    }
  }
}
