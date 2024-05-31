package com.anissan.battarang.background.receivers.handlers

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.view.Display
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anissan.battarang.R
import com.anissan.battarang.background.receivers.BatteryLevelCheckerAlarmReceiver
import com.anissan.battarang.data.LocalKvStore
import com.anissan.battarang.network.MessageType
import com.anissan.battarang.network.ReceiverApiClient
import com.anissan.battarang.ui.MainActivity
import com.anissan.battarang.utils.logV

class BroadcastedEventHandlers(
  private val context: Context,
  private val localKvStore: LocalKvStore,
  private val receiverApiClient: ReceiverApiClient,
) {
  private val batteryLevelPollingAlarm: AlarmManager =
    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private val alarmId: Int = 64

  private val levelCheckerIntent: Intent =
    Intent(context, BatteryLevelCheckerAlarmReceiver::class.java)
      .setAction(BatteryLevelCheckerAlarmReceiver.ACTION_CHECK_BATTERY_LEVEL)

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
      PendingIntent.getBroadcast(context, alarmId, levelCheckerIntent, pendingIntentFlag)
    )

    logV { "Stopped the periodic battery level checker alarm" }
  }

  fun notifyBatteryIsLow() {
    if (shouldSkipNotification()) {
      logV { "Skipping sending battery low notification as the display is on" }
      return
    }

    receiverApiClient.sendNotification(MessageType.LOW, null, ::notifyUpdates)
  }

  fun notifyIfMaxLevelReached() {
    val batteryLevel: Int = currentBatteryLevel
    logV { "Battery Level: $batteryLevel" }

    if (batteryLevel < localKvStore.maxChargingLevelPercentage) return

    if (shouldSkipNotification()) {
      logV { "Skipping sending battery max reached notification as the display is on" }
      return
    }

    stopBatteryLevelPollingAlarm()

    receiverApiClient.sendNotification(MessageType.FULL, batteryLevel, ::notifyUpdates)
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

  companion object {
    const val NOTIFICATION_ID = 404
  }

  private fun notifyUpdates(responseBody: String?) {
    if (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      logV { "Skipping notifying updates as user has explicitly disabled notifications from app settings" }
      return
    }

    // A server response delimited with `||` means it should be posted as notification.
    val message = responseBody?.split("||")
    if (message?.size != 3) return

    val (action, title, description) = message.map { it.trim() }

    // A new activity will be created on every notification click regardless of an existing activity.
    val notificationTapActionPendingIntent: PendingIntent = PendingIntent.getActivity(
      context,
      128,
      Intent(context, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    val notificationActionPendingIntent: PendingIntent = PendingIntent.getActivity(
      context,
      129,
      Intent(context, MainActivity::class.java).setAction(action),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    val notification = NotificationCompat.Builder(context, context.createServiceUpdateChannel())
      .setAutoCancel(true)
      .setContentTitle(title)
      .setStyle(NotificationCompat.BigTextStyle().bigText(description))
      .setSmallIcon(R.drawable.ic_notification_service)
      .setContentIntent(notificationTapActionPendingIntent)
      .addAction(0, action, notificationActionPendingIntent)
      .build()

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
  }

  /**
   * This function is safe to call multiple times as the Channels get created only once.
   */
  private fun Context.createServiceUpdateChannel(): String {
    val channelId = "SERVICE_UPDATE"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
        NotificationChannel(
          channelId,
          getString(R.string.service_update_channel),
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = getString(R.string.service_update_channel_description)
          setShowBadge(true)
        }
      )
    }

    return channelId
  }
}
