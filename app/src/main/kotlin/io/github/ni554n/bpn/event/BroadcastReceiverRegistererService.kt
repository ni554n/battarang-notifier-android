package io.github.ni554n.bpn.event

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.ni554n.bpn.BootEventReceiver.Companion.resumeAfterBoot
import io.github.ni554n.bpn.MainActivity
import io.github.ni554n.bpn.event.receivers.AlarmBroadcastReceivers
import io.github.ni554n.bpn.event.receivers.PowerBroadcastReceivers
import logcat.logcat
import org.koin.android.ext.android.inject

/**
 * Most of the implicit broadcast receivers no longer work if declared from Manifest.
 * This Service is required to register the Receivers dynamically and to listen
 * for the broadcasted events as long as the notification service is enabled.
 *
 * This service can be started and stopped easily with the helper functions in companion object.
 */
class BroadcastReceiverRegistererService : Service() {
  private val systemBroadcastReceivers: PowerBroadcastReceivers by inject()
  private val localBroadcastReceivers: AlarmBroadcastReceivers by inject()

  private val currentBatteryStatus: Int?
    get() = registerReceiver(
      null,
      IntentFilter(Intent.ACTION_BATTERY_CHANGED),
    )?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

  override fun onCreate() {
    // Background services can be killed by the System at anytime.
    // Since Oreo, foreground services with a persistent notification is required for long
    // running tasks.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForeground(128, buildServiceNotification())
    }

    /* Register the implicit Broadcast Receivers */
    registerReceiver(systemBroadcastReceivers,
      systemBroadcastReceivers.intentFiltersBasedOnPreference)

    LocalBroadcastManager.getInstance(this)
      .registerReceiver(localBroadcastReceivers, localBroadcastReceivers.intentFilters)

    logcat { "Registered the implicit Broadcast Receivers." }

    // Battery level monitoring alarm is triggered by the power connection event.
    // If the charger is already connected before starting this service, then manually
    // trigger the monitoring alarm.
    if (currentBatteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
      LocalBroadcastManager.getInstance(this)
        .sendBroadcast(Intent(AlarmBroadcastReceivers.ACTION_BATTERY_STATUS_CHARGING))
    }

    resumeAfterBoot(true)
  }

  override fun onDestroy() {
    unregisterReceiver(systemBroadcastReceivers)
    LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadcastReceivers)

    resumeAfterBoot(false)

    super.onDestroy()
  }

  override fun onBind(intent: Intent): IBinder? = null

  companion object {
    private lateinit var thisServiceIntent: Intent

    fun start(context: Context) {
      thisServiceIntent = Intent(context, BroadcastReceiverRegistererService::class.java)

      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        context.startForegroundService(thisServiceIntent)
      } else {
        context.startService(thisServiceIntent)
      }

      logcat { "Started this foreground service successfully" }
    }

    fun stop(context: Context) {
      if (::thisServiceIntent.isInitialized.not()) return

      // If an alarm is already in progress, only stopping this service won't stop the alarm.
      // It needs to be stopped explicitly.
      LocalBroadcastManager.getInstance(context)
        .sendBroadcast(Intent(AlarmBroadcastReceivers.ACTION_STOP_ALARM))

      context.stopService(thisServiceIntent)
      logcat { "Stopped the foreground service" }
    }
  }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Context.buildServiceNotification(): Notification {
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
    .setContentText("You can tap the Minimize button to open the channel settings and tap turn on Minimize to this service notification in the silent section. May need to toggle the Notification Service for it to minimize.")
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
