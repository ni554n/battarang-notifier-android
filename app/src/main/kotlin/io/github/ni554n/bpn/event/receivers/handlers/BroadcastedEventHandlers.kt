package io.github.ni554n.bpn.event.receivers.handlers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import io.github.ni554n.bpn.event.receivers.AlarmBroadcastReceivers
import io.github.ni554n.bpn.api.PushServerClient
import io.github.ni554n.bpn.storage.UserPreferences
import logcat.logcat

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

      logcat { "Battery level = $currentLevel%" }
      return currentLevel
    }

  private lateinit var batteryLevelCheckerAlarmPendingIntent: PendingIntent

  fun startBatteryLevelCheckerAlarm() {
    // Maybe the user wants to fully charge the battery this time.
    if (currentBatteryLevel > userPreferences.chargingLevelPercentage) {
      logcat { "Charger Connected: But not setting the alarm because battery level is already ahead of the preferred level." }
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

    logcat { "Charger Connected: Starting an Alarm to check the battery level at a minute interval..." }
  }

  fun stopBatteryLevelCheckerAlarm() {
    logcat { "Requested to stop the periodic alarm" }

    if (::batteryLevelCheckerAlarmPendingIntent.isInitialized.not()) return

    alarmManager.cancel(batteryLevelCheckerAlarmPendingIntent)
    logcat { "Stopped the periodic battery level checker alarm" }
  }

  fun notifyBatteryIsLow() {
    if (userPreferences.shouldNotify(context)) {
      pushServerClient.postNotification(
        userPreferences.notifierGcmToken,
        title = "🔋⚠ Low!",
        body = "🔌 Connect to a power source!",
      )

      logcat { "Battery low event has been notified successfully" }
    } else {
      logcat { "Skipped battery low push notification due to user preference" }
    }
  }

  fun notifyAfterLevelReached() {
    logcat { "Triggered alarm event for battery level check" }

    if (userPreferences.shouldNotify(context).not()) {
      logcat { "Skipped alarm event due to user preference" }
      return
    }

    if (currentBatteryLevel < userPreferences.chargingLevelPercentage) return

    pushServerClient.postNotification(
      token = userPreferences.notifierGcmToken,
      title = "🔋⚡ $currentBatteryLevel%",
      body = "🔌 Disconnect.",
    ) {
      logcat { "Notification has been sent successfully." }

      stopBatteryLevelCheckerAlarm()
    }
  }
}
