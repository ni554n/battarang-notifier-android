package com.anissan.battarang.background.services.receivers.handlers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.view.Display
import com.anissan.battarang.background.services.receivers.BatteryLevelPollingAlarmReceiver
import com.anissan.battarang.background.services.receivers.BatteryStatusReceiver
import com.anissan.battarang.data.LocalKvStore
import com.anissan.battarang.network.MessageType
import com.anissan.battarang.network.ReceiverApiClient
import com.anissan.battarang.utils.logV

/**
 * Collection of event handler functions used in both [BatteryLevelPollingAlarmReceiver] & [BatteryStatusReceiver]
 * in one place.
 */
class BroadcastedEventHandlers(
  private val context: Context,
  private val localKvStore: LocalKvStore,
  private val receiverApiClient: ReceiverApiClient,
) {
  private val batteryLevelPollingAlarm: AlarmManager =
    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private val alarmId: Int = 64

  private val levelCheckerIntent: Intent =
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

  private val currentBatteryLevel: Int
    get() = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
      .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

  fun startBatteryLevelPollingAlarm() {
    if (currentBatteryLevel > localKvStore.maxChargingLevelPercentage) {
      logV { "Charger Connected: But not setting the alarm because battery level is already ahead of the preferred level." }
      return
    }

    batteryLevelPollingAlarm.setRepeating(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime(),
      60 * 1_000L, // 1 minute
      PendingIntent.getBroadcast(context, alarmId, levelCheckerIntent, pendingIntentFlag),
    )

    logV { "Charger Connected: Starting an Alarm to check the battery level at a minute interval..." }
  }

  fun stopBatteryLevelPollingAlarm() {
    logV { "Requested to stop the periodic alarm" }

    batteryLevelPollingAlarm.cancel(
      PendingIntent.getBroadcast(
        context,
        alarmId,
        levelCheckerIntent,
        pendingIntentFlag,
      )
    )
    logV { "Stopped the periodic battery level checker alarm" }
  }

  fun notifyBatteryIsLow() {
    if (shouldSkipNotification()) return

    receiverApiClient.sendNotification(MessageType.LOW)
  }

  fun notifyAfterLevelReached() {
    if (shouldSkipNotification()) return

    val batteryLevel: Int = currentBatteryLevel

    if (batteryLevel < localKvStore.maxChargingLevelPercentage) return

    stopBatteryLevelPollingAlarm()
    receiverApiClient.sendNotification(MessageType.FULL, batteryLevel)
  }

  /**
   * Determines if notifications should be sent based on user preference and the current display state.
   * */
  private fun shouldSkipNotification(): Boolean {
    if (localKvStore.isSkipWhileDisplayOnEnabled) {
      // Make sure every display is OFF before notifying.
      return (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
        .displays
        .any { display: Display -> display.state == Display.STATE_ON }
    }

    return false
  }
}
