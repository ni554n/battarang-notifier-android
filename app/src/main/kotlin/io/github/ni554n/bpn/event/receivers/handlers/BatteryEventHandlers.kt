package io.github.ni554n.bpn.event.receivers.handlers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import io.github.ni554n.bpn.network.PushNotification
import io.github.ni554n.bpn.preferences.UserPreferences
import io.github.ni554n.bpn.event.receivers.BatteryEventReceivers
import logcat.logcat

class BatteryEventHandlers(
  private val context: Context,
  private val userPreferences: UserPreferences,
  private val pushNotification: PushNotification,
) {
  private val alarmManager: AlarmManager =
    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private lateinit var batteryLevelCheckerAlarmPendingIntent: PendingIntent

  fun startBatteryLevelCheckerAlarm() {
    batteryLevelCheckerAlarmPendingIntent = run {
      val uniqueId = 64

      val alarmEventIntent: Intent = Intent(context, BatteryEventReceivers::class.java)
        .setAction(ACTION_ALARM_BATTERY_LEVEL_CHECK)

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
    if (::batteryLevelCheckerAlarmPendingIntent.isInitialized.not()) return

    alarmManager.cancel(batteryLevelCheckerAlarmPendingIntent)
    logcat { "Stopped the periodic alarm" }
  }

  fun notifyBatteryIsLow() {
    if (userPreferences.shouldNotify(context)) {
      pushNotification.notifyAsync(
        userPreferences.notifierGcmToken,
        title = "🔋⚠ Low!",
        body = "🔌 Connect to a power source!",
      )

      logcat { "Battery low event has been notified successfully" }
    } else {
      logcat { "Skipped battery low push notification due to user preference" }
    }
  }

  fun checkIfBatteryLevelReached() {
    logcat { "Triggered alarm event for battery level check" }

    if (userPreferences.shouldNotify(context).not()) {
      logcat { "Skipped alarm event due to user preference" }
      return
    }

    val currentLevel: Int = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
      .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    logcat { "Battery level = $currentLevel%" }

    if (currentLevel < userPreferences.chargingLevelPercentage) return

    pushNotification.notifyAsync(
      userPreferences.notifierGcmToken,
      title = "🔋⚡ $currentLevel%",
      body = "🔌 Disconnect.",
    ) {
      stopBatteryLevelCheckerAlarm()
      logcat { "Notification has been sent successfully. Alarm is now stopped." }
    }
  }

  companion object {
    val ACTION_ALARM_BATTERY_LEVEL_CHECK = "${BatteryEventHandlers::class.java.name}.check_battery_level"
  }
}
