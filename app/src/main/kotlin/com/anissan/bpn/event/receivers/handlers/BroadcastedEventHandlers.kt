package com.anissan.bpn.event.receivers.handlers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import com.anissan.bpn.api.PushServerClient
import com.anissan.bpn.event.receivers.AlarmBroadcastReceivers
import com.anissan.bpn.storage.UserPreferences
import com.anissan.bpn.utils.logV

class BroadcastedEventHandlers(
  private val context: Context,
  private val userPreferences: UserPreferences,
  private val pushServerClient: PushServerClient,
) {
  private val alarmManager: AlarmManager =
    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private val currentBatteryLevel: Int
    get() {
      val currentLevel: Int = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

      logV { "Battery level = $currentLevel%" }
      return currentLevel
    }

  private lateinit var batteryLevelCheckerAlarmPendingIntent: PendingIntent

  fun startBatteryLevelCheckerAlarm() {
    // Maybe the user wants to fully charge the battery this time.
    if (currentBatteryLevel > userPreferences.chargingLevelPercentage) {
      logV { "Charger Connected: But not setting the alarm because battery level is already ahead of the preferred level." }
      return
    }

    batteryLevelCheckerAlarmPendingIntent = run {
      val uniqueId = 64

      val alarmEventIntent: Intent = Intent(context, AlarmBroadcastReceivers::class.java)
        .setAction(AlarmBroadcastReceivers.ACTION_CHECK_BATTERY_LEVEL)

      // From Android 12+, it is mandatory to add a mutability flag on pending intents.
      // FLAG_IMMUTABLE added in API 23.
      val pendingFlag: Int = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
      } else {
        PendingIntent.FLAG_CANCEL_CURRENT
      }

      PendingIntent.getBroadcast(context, uniqueId, alarmEventIntent, pendingFlag)
    }

    alarmManager.setRepeating(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime(),
      60 * 1_000L, // 1 minute
      batteryLevelCheckerAlarmPendingIntent,
    )

    logV { "Charger Connected: Starting an Alarm to check the battery level at a minute interval..." }
  }

  fun stopBatteryLevelCheckerAlarm() {
    logV { "Requested to stop the periodic alarm" }

    if (::batteryLevelCheckerAlarmPendingIntent.isInitialized.not()) return

    alarmManager.cancel(batteryLevelCheckerAlarmPendingIntent)
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

    if (currentBatteryLevel < userPreferences.chargingLevelPercentage) return

    pushServerClient.postNotification(
      token = userPreferences.notifierGcmToken,
      title = "🔋 $currentBatteryLevel%",
      body = "⚡ Disconnect.",
    ) {
      logV { "Notification has been sent successfully." }

      stopBatteryLevelCheckerAlarm()
    }
  }
}
