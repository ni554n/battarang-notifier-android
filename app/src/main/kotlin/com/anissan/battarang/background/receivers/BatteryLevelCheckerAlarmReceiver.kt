package com.anissan.battarang.background.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anissan.battarang.background.receivers.handlers.BroadcastedEventHandlers
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Mainly for managing the alarm event that checks current battery level percentage.
 * There's a method to directly listens for battery level change sent by the system, but it gets
 * called too frequently. It is more efficient to check the percentage periodically as this alarm
 * starts only when device is charging.
 */
class BatteryLevelCheckerAlarmReceiver : BroadcastReceiver(), KoinComponent {
  private val broadcastedEventHandlers: BroadcastedEventHandlers by inject()

  override fun onReceive(context: Context?, intent: Intent?) {
    val action = intent?.action ?: return
    logV { """Received the Intent Action: "$action"""" }

    when (action) {
      ACTION_CHECK_BATTERY_LEVEL -> broadcastedEventHandlers.notifyIfMaxLevelReached()
      else -> logE { "$action is not a supported action by this receiver" }
    }
  }

  companion object {
    val ACTION_CHECK_BATTERY_LEVEL =
      "${BatteryLevelCheckerAlarmReceiver::class.java.name}.check_battery_level"
  }
}
