package com.anissan.bpn.background.receivers.handlers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import com.anissan.bpn.background.receivers.BatteryLevelPollingAlarmReceiver
import com.anissan.bpn.background.receivers.BatteryStatusReceiver
import com.anissan.bpn.network.PushServerClient
import com.anissan.bpn.storage.UserPreferences
import com.anissan.bpn.utils.logV

/**
 * Collection of event handler functions used in both [BatteryLevelPollingAlarmReceiver] & [BatteryStatusReceiver]
 * in one place.
 */
class BroadcastedEventHandlers(
  private val context: Context,
  private val userPreferences: UserPreferences,
  private val pushServerClient: PushServerClient,
) {
  private val alarmManager: AlarmManager =
    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private val alarmId: Int = 64

  private val pollingAlarmIntent: Intent =
    Intent(context, BatteryLevelPollingAlarmReceiver::class.java)
      .setAction(BatteryLevelPollingAlarmReceiver.ACTION_CHECK_BATTERY_LEVEL)

  // From Android 12+, it is mandatory to add a mutability flag on pending intents.
  // FLAG_IMMUTABLE added in API 23.
  private val pendingIntentFlag: Int =
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
      PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_CANCEL_CURRENT
    }

  fun startBatteryLevelPollingAlarm() {
    // Maybe the user wants to fully charge the battery this time.
    if (checkCurrentBatteryLevel() > userPreferences.maxChargingLevelPercentage) {
      logV { "Charger Connected: But not setting the alarm because battery level is already ahead of the preferred level." }
      return
    }

    alarmManager.setRepeating(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime(),
      60 * 1_000L, // 1 minute
      PendingIntent.getBroadcast(context, alarmId, pollingAlarmIntent, pendingIntentFlag),
    )

    logV { "Charger Connected: Starting an Alarm to check the battery level at a minute interval..." }
  }

  fun stopBatteryLevelPollingAlarm() {
    logV { "Requested to stop the periodic alarm" }

    alarmManager.cancel(
      PendingIntent.getBroadcast(
        context,
        alarmId,
        pollingAlarmIntent,
        pendingIntentFlag
      )
    )
    logV { "Stopped the periodic battery level checker alarm" }
  }

  fun notifyBatteryIsLow() {
    if (userPreferences.shouldNotify(context)) {
      pushServerClient.postNotification(
        userPreferences.notifierGcmToken,
        title = "🔋⚠ Low!",
        body = "🔌 Connect to a power source!",
      )

      logV { "Battery low event has been notified successfully" }
    } else {
      logV { "Skipped battery low push notification due to user preference" }
    }
  }

  fun notifyAfterLevelReached() {
    logV { "Triggered alarm event for battery level check" }

    if (userPreferences.shouldNotify(context).not()) {
      logV { "Skipped alarm event due to user preference" }
      return
    }

    val currentBatteryLevel: Int = checkCurrentBatteryLevel()

    if (currentBatteryLevel < userPreferences.maxChargingLevelPercentage) return

    pushServerClient.postNotification(
      token = userPreferences.notifierGcmToken,
      title = "🔋 $currentBatteryLevel%",
      body = "⚡ Disconnect.",
    ) {
      logV { "Notification has been sent successfully." }

      stopBatteryLevelPollingAlarm()
    }
  }

  private fun checkCurrentBatteryLevel(): Int {
    val currentLevel: Int = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
      .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    logV { "Battery level = $currentLevel%" }
    return currentLevel
  }
}
