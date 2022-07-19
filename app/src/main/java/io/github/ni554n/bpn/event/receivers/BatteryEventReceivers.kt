package io.github.ni554n.bpn.event.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.github.ni554n.bpn.preferences.UserPreferences
import io.github.ni554n.bpn.event.receivers.handlers.BatteryEventHandlers
import logcat.LogPriority
import logcat.logcat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Receives Power, Battery and Alarm events in one class for convenience.
 */
class BatteryEventReceivers : BroadcastReceiver(), KoinComponent {
  private val userPreferences: UserPreferences by inject()
  private val batteryEventHandlers: BatteryEventHandlers by inject()

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null) {
      logcat(LogPriority.WARN) { "onReceive() received a null Context" }
      return
    }

    if (intent == null) {
      logcat(LogPriority.WARN) { "onReceive() received a null Intent" }
      return
    }

    logcat { """onReceive() received "${intent.action}" action""" }

    batteryEventHandlers.run {
      when (intent.action) {
        Intent.ACTION_POWER_CONNECTED -> startBatteryLevelCheckerAlarm()
        Intent.ACTION_POWER_DISCONNECTED -> stopBatteryLevelCheckerAlarm()

        Intent.ACTION_BATTERY_LOW -> notifyBatteryIsLow()

        BatteryEventHandlers.ACTION_ALARM_BATTERY_LEVEL_CHECK -> checkIfBatteryLevelReached()

        else -> logcat(LogPriority.ERROR) { "${intent.action} is not a supported action" }
      }
    }
  }

  fun getBatteryEventIntentFilters(): IntentFilter = IntentFilter().apply {
    userPreferences.run {
      if (isLevelReachedNotificationEnabled) {
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
      }

      if (isLowBatteryNotificationEnabled) {
        addAction(Intent.ACTION_BATTERY_LOW)
      }
    }
  }
}
