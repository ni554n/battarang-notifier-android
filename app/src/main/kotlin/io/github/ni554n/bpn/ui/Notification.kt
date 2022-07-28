package io.github.ni554n.bpn.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.github.ni554n.bpn.MainActivity

@RequiresApi(Build.VERSION_CODES.O)
fun Context.buildServiceNotification(): Notification {
  val batteryStateChannelId = createBatteryStateChannel()

  val notificationTapActionPendingIntent: PendingIntent = PendingIntent.getActivity(
    this,
    256,
    Intent(this, MainActivity::class.java),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
  )

  val minimizeButtonActionPendingIntent: PendingIntent = PendingIntent.getActivity(
    this,
    512,
    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
      putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
      putExtra(Settings.EXTRA_CHANNEL_ID, batteryStateChannelId)
    },
    PendingIntent.FLAG_IMMUTABLE,
  )

  return NotificationCompat.Builder(this, batteryStateChannelId)
    .setContentTitle("Listening for battery related events idly") // TODO Extract these to string resource
    .setContentText("You can tap the Minimize button to open the channel settings and tap turn on Minimize to this service notification in the silent section.")
    .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
    .setTicker("Battery State Ticker") // Remove it if the UX feels better
    .setPriority(NotificationCompat.PRIORITY_MIN)
    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    .setContentIntent(notificationTapActionPendingIntent)
    .addAction(
      android.R.drawable.ic_lock_idle_low_battery,
      "Minimize this Notification",
      minimizeButtonActionPendingIntent,
    ).build()
}

// Notification channel will not be created multiple times
@RequiresApi(Build.VERSION_CODES.O)
private fun Context.createBatteryStateChannel(): String {
  val channelId = "BATTERY_STATE"

  val batteryStateChannel: NotificationChannel = NotificationChannel(
    channelId,
    "Battery Event Listener",
    NotificationManager.IMPORTANCE_MIN,
  ).apply {
    description = "Listens for battery states"
    setShowBadge(false)
  }

  (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
    batteryStateChannel
  )

  return channelId
}
