package io.github.ni554n.bpn.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.view.Display
import io.github.ni554n.bpn.network.PushNotification
import io.github.ni554n.bpn.preferences.UserPreferences
import logcat.LogPriority
import logcat.logcat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Capturing Power Event and AlarmManager receivers in one class for convenience.
 *
 * ON Power Event it registers
 */
class PowerEventReceivers : BroadcastReceiver(), KoinComponent {
  private val userPreferences: UserPreferences by inject()
  private val push: PushNotification by inject()

  private val alarmsIntentActionName = "${javaClass.name}.check_battery_level"

  private lateinit var alarmsPendingIntent: PendingIntent

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null || intent == null) {
      logcat(LogPriority.ERROR) {
        "onReceive(context = ${context.toString()}, intent = ${intent.toString()})"
      }

      return
    }

    when (intent.action) {
      // Battery Event Receivers
      Intent.ACTION_BATTERY_LOW -> notifyBatteryIsLow(context)
      Intent.ACTION_POWER_CONNECTED -> startBatteryLevelCheckingAlarm(context)
      Intent.ACTION_POWER_DISCONNECTED -> stopBatteryLevelCheckingAlarm(context)

      // Alarm Event Receiver
      alarmsIntentActionName -> checkIfBatteryLevelReached(context)

      else -> logcat(LogPriority.ERROR) { "${intent.action} is not a supported Battery or Alarm action" }
    }
  }

  private fun notifyBatteryIsLow(context: Context) {
    if (shouldNotify(context)) {
      push.notify(
        userPreferences.notifierGcmToken,
        title = "🔋⚠ Low!",
        body = "🔌 Connect to a power source!",
      )
    }
  }

  private fun startBatteryLevelCheckingAlarm(context: Context) {
    logcat { "Charger Connected: Starting the Alarm to check the battery level periodically..." }

    (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.setRepeating(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime(),
      60 * 1_000L, // 1 minute
      intentForAlarmsAction(context),
    )
  }

  private fun intentForAlarmsAction(context: Context): PendingIntent {
    if (this::alarmsPendingIntent.isInitialized) return alarmsPendingIntent

    val intent: Intent = Intent(context, this::class.java).setAction(alarmsIntentActionName)

    var pendingFlag: Int = PendingIntent.FLAG_CANCEL_CURRENT

    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
      pendingFlag = pendingFlag or PendingIntent.FLAG_IMMUTABLE // Bitwise OR for setting flags
    }

    alarmsPendingIntent = PendingIntent.getBroadcast(context, 64, intent, pendingFlag)

    return alarmsPendingIntent
  }

  private fun stopBatteryLevelCheckingAlarm(context: Context) {
    logcat { "Charger Disconnected: Stopping the battery level checking alarm." }

    (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(intentForAlarmsAction(
      context))
  }

  private fun checkIfBatteryLevelReached(context: Context) {
    val batteryLevel: Int = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
      .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    logcat { "Battery level = $batteryLevel%" }

    if (batteryLevel < userPreferences.chargingLevelPercentage) return

    stopBatteryLevelCheckingAlarm(context)

    if (shouldNotify(context)) {
      logcat { "Sending notification because desired battery level has reached" }

      push.notify(
        userPreferences.notifierGcmToken,
        title = "🔋⚡ $batteryLevel%",
        body = "🔌 Disconnect.",
      )
    }
  }

  private fun shouldNotify(context: Context): Boolean {
    // Notifies regardless of the display state.
    if (userPreferences.isNotificationWhileScreenOnEnabled) return true

    // Notifies only if no display is on.
    return (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
      .displays
      .all { display: Display -> display.state != Display.STATE_ON }
  }
}
