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
import io.github.ni554n.bpn.network.MyInfo
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

  private val alarmIntentAction = "${javaClass.name}.check_battery_level"

  private lateinit var pendingIntent: PendingIntent

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
      Intent.ACTION_POWER_CONNECTED -> startPowerLevelCheckingAlarm(context)
      Intent.ACTION_POWER_DISCONNECTED -> stopPowerLevelCheckingAlarm(context)

      // Alarm Event Receiver
      alarmIntentAction -> checkIfBatteryLevelReached(context)

      else -> logcat(LogPriority.ERROR) { "Invalid intent action: ${intent.action} provided." }
    }
  }

  private fun notifyBatteryIsLow(context: Context) {
    if (shouldNotify(context)) {
      push.notify(
        MyInfo.token,
        title = "🔋⚠ Low!",
        body = "🔌 Connect to a power source!"
      )
    }
  }

  private fun startPowerLevelCheckingAlarm(context: Context) {
    (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.setRepeating(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime(),
      60 * 1_000L, // 1 minute
      getPendingIntent(context),
    )
  }

  private fun stopPowerLevelCheckingAlarm(context: Context) {
    (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(getPendingIntent(
      context))
  }

  private fun getPendingIntent(context: Context): PendingIntent {
    if (this::pendingIntent.isInitialized) return pendingIntent

    val intent: Intent = Intent(context, this::class.java).setAction(alarmIntentAction)

    val pendingFlag: Int = PendingIntent.FLAG_CANCEL_CURRENT.also {
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
        PendingIntent.FLAG_IMMUTABLE or it // Bitwise OR for setting flags
      }
    }

    pendingIntent = PendingIntent.getBroadcast(context, 64, intent, pendingFlag)

    return pendingIntent
  }

  private fun checkIfBatteryLevelReached(context: Context) {
    val batteryLevel: Int = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
      .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    if (batteryLevel < userPreferences.chargingLevelPercentage) return

    stopPowerLevelCheckingAlarm(context)

    if (shouldNotify(context)) {
      logcat { "Notifying battery level reached at $batteryLevel%" }

      push.notify(
        MyInfo.token,
        title = "🔋⚡ $batteryLevel%",
        body = "🔌 Disconnect."
      )
    }
  }

  private fun shouldNotify(context: Context): Boolean {
    // Preference is set to notify regardless of the display state.
    if (userPreferences.isNotificationWhileScreenOnEnabled) return true

    // Notifies only if no display is on.
    return (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
      .displays
      .all { display: Display -> display.state != Display.STATE_ON }
  }
}
