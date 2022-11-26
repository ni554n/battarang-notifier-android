package com.anissan.bpn.event.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.anissan.bpn.event.receivers.handlers.BroadcastedEventHandlers
import com.anissan.bpn.storage.UserPreferences
import com.anissan.bpn.utils.logE
import com.anissan.bpn.utils.logV
import com.anissan.bpn.utils.logW
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Receives charger connection events and battery low event broadcasted by the System.
 */
class BatteryStatusReceiver : BroadcastReceiver(), KoinComponent {
  private val userPreferences: UserPreferences by inject()
  private val broadcastedEventHandlers: BroadcastedEventHandlers by inject()

  val intentFiltersBasedOnPreference: IntentFilter
    get() = IntentFilter().apply {
      userPreferences.run {
        if (isMaxLevelNotificationEnabled) {
          addAction(Intent.ACTION_POWER_CONNECTED)
          addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        if (isLowBatteryNotificationEnabled) {
          addAction(Intent.ACTION_BATTERY_LOW)
        }
      }
    }

  override fun onReceive(context: Context?, intent: Intent?) {
    val action = intent?.action ?: run {
      logW { "onReceive() received a null Intent" }
      return
    }

    logV { """Received the Intent Action: "$action"""" }

    broadcastedEventHandlers.run {
      when (action) {
        Intent.ACTION_POWER_CONNECTED -> startBatteryLevelPollingAlarm()
        Intent.ACTION_POWER_DISCONNECTED -> stopBatteryLevelPollingAlarm()

        Intent.ACTION_BATTERY_LOW -> notifyBatteryIsLow()

        else -> logE { "$action is not a supported action by this receiver" }
      }
    }
  }
}
