package com.anissan.bpn.event.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.anissan.bpn.event.receivers.handlers.BroadcastedEventHandlers
import logcat.LogPriority
import logcat.logcat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Receives the periodic event to check for battery level sent by the Alarm and a
 * couple of Local broadcast to start and stop the alarm.
 */
class AlarmBroadcastReceivers : BroadcastReceiver(), KoinComponent {
  private val broadcastedEventHandlers: BroadcastedEventHandlers by inject()

  val intentFilters = IntentFilter().apply {
    addAction(ACTION_BATTERY_STATUS_CHARGING)
    addAction(ACTION_STOP_ALARM)
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    val action = intent?.action ?: run {
      logcat(LogPriority.WARN) { "onReceive() received a null Intent" }
      return
    }

    logcat { """Received the Intent Action: "$action"""" }

    broadcastedEventHandlers.run {
      when (action) {
        ACTION_CHECK_BATTERY_LEVEL -> notifyAfterLevelReached()

        ACTION_BATTERY_STATUS_CHARGING -> startBatteryLevelCheckerAlarm()
        ACTION_STOP_ALARM -> stopBatteryLevelCheckerAlarm()

        else -> logcat(LogPriority.ERROR) { "$action is not a supported action by this receiver" }
      }
    }
  }

  companion object {
    private val thisPackageName = AlarmBroadcastReceivers::class.java.name

    val ACTION_CHECK_BATTERY_LEVEL = "$thisPackageName.check_battery_level"
    val ACTION_BATTERY_STATUS_CHARGING = "$thisPackageName.battery_status_charging"
    val ACTION_STOP_ALARM = "$thisPackageName.stop_alarm"
  }
}
