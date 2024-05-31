package com.anissan.battarang.background.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anissan.battarang.background.receivers.handlers.BroadcastedEventHandlers
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChargerConnectionReceiver : BroadcastReceiver(), KoinComponent {
  private val broadcastedEventHandlers: BroadcastedEventHandlers by inject()

  override fun onReceive(context: Context?, intent: Intent?) {
    val action = intent?.action ?: return
    logV { """Received the Intent Action: "$action"""" }

    when (action) {
      Intent.ACTION_POWER_CONNECTED -> broadcastedEventHandlers.startBatteryLevelPollingAlarm()
      Intent.ACTION_POWER_DISCONNECTED -> broadcastedEventHandlers.stopBatteryLevelPollingAlarm()
      else -> logE { "$action is not a supported action by this receiver" }
    }
  }
}
