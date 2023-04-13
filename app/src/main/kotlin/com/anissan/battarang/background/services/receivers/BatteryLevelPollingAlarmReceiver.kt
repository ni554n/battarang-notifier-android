package com.anissan.battarang.background.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.anissan.battarang.background.services.receivers.handlers.BroadcastedEventHandlers
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV
import com.anissan.battarang.utils.logW
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Mainly for managing the alarm event that checks current battery level percentage.
 * There's a method to directly listens for battery level change sent by the system, but it gets
 * called too frequently. It is more efficient to check the percentage periodically as this alarm
 * starts only when device is charging.
 */
class BatteryLevelPollingAlarmReceiver : BroadcastReceiver(), KoinComponent {
  private val broadcastedEventHandlers: BroadcastedEventHandlers by inject()

  val intentFilters = IntentFilter().apply {
    addAction(ACTION_BATTERY_STATUS_CHARGING)
    addAction(ACTION_STOP_ALARM)
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    val action = intent?.action ?: run {
      logW { "onReceive() received a null Intent" }
      return
    }

    logV { """Received the Intent Action: "$action"""" }

    broadcastedEventHandlers.run {
      when (action) {
        ACTION_CHECK_BATTERY_LEVEL -> notifyAfterLevelReached()

        ACTION_BATTERY_STATUS_CHARGING -> startBatteryLevelPollingAlarm()
        ACTION_STOP_ALARM -> stopBatteryLevelPollingAlarm()

        else -> logE { "$action is not a supported action by this receiver" }
      }
    }
  }

  companion object {
    private val thisPackageName = BatteryLevelPollingAlarmReceiver::class.java.name

    val ACTION_CHECK_BATTERY_LEVEL = "$thisPackageName.check_battery_level"
    val ACTION_BATTERY_STATUS_CHARGING = "$thisPackageName.battery_status_charging"
    val ACTION_STOP_ALARM = "$thisPackageName.stop_alarm"
  }
}
