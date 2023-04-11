package com.anissan.bpn.background.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import com.anissan.bpn.R
import com.anissan.bpn.background.BootEventReceiver.Companion.resumeAfterBoot
import com.anissan.bpn.background.services.receivers.BatteryLevelPollingAlarmReceiver
import com.anissan.bpn.background.services.receivers.BatteryStatusReceiver
import com.anissan.bpn.ui.MainActivity
import com.anissan.bpn.utils.logV
import org.koin.android.ext.android.inject

/**
 * Most implicit broadcast receivers can not be declared in the manifest anymore.
 * This Service is responsible for registering the Receivers and listening
 * for the broadcasted events as long as the notification service is enabled.
 *
 * This service can be started and stopped easily with the helper functions in companion object.
 */
class BroadcastReceiverRegistererService : Service() {
  private val batteryStatusReceiver: BatteryStatusReceiver by inject()
  private val batteryLevelPollingAlarmReceiver: BatteryLevelPollingAlarmReceiver by inject()

  override fun onCreate() {
    // Background services can be killed by the System at anytime.
    // Since Oreo, foreground services with a persistent notification is required for long
    // running tasks. It's all in here:
    // https://developer.android.com/guide/components/foreground-services
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForeground(128, buildServiceNotification())
    }

    registerReceiver(
      batteryStatusReceiver,
      batteryStatusReceiver.intentFiltersBasedOnPreference,
    )

    LocalBroadcastManager.getInstance(this)
      .registerReceiver(
        batteryLevelPollingAlarmReceiver,
        batteryLevelPollingAlarmReceiver.intentFilters,
      )

    logV { "Registered the implicit Broadcast Receivers." }

    // Corner case: if the charger is already connected before starting this service, then manually
    // start the battery level polling alarm.

    val currentBatteryStatus: Int? =
      registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(
        BatteryManager.EXTRA_STATUS,
        -1,
      )

    if (currentBatteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
      LocalBroadcastManager.getInstance(this)
        .sendBroadcast(Intent(BatteryLevelPollingAlarmReceiver.ACTION_BATTERY_STATUS_CHARGING))
    }

    resumeAfterBoot(true)
  }

  override fun onDestroy() {
    unregisterReceiver(batteryStatusReceiver)
    LocalBroadcastManager.getInstance(this).unregisterReceiver(batteryLevelPollingAlarmReceiver)

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

      logV { "Started this foreground service successfully" }
    }

    fun stop(context: Context) {
      if (Companion::thisServiceIntent.isInitialized.not()) return

      // If an alarm is already in progress, only stopping this service won't stop the alarm.
      // It needs to be stopped explicitly.
      LocalBroadcastManager.getInstance(context)
        .sendBroadcast(Intent(BatteryLevelPollingAlarmReceiver.ACTION_STOP_ALARM))

      context.stopService(thisServiceIntent)
      logV { "Stopped the foreground service." }
    }
  }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Context.buildServiceNotification(): Notification {
  val batteryStateChannelId = createNotificationServiceChannel()

  // A new activity will be created on every notification click regardless of an existing activity.
  val notificationTapActionPendingIntent: PendingIntent = PendingIntent.getActivity(
    this,
    256,
    Intent(this, MainActivity::class.java),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
  )

  val channelSettingsIntent: PendingIntent = PendingIntent.getActivity(
    this,
    512,
    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
      putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
      putExtra(Settings.EXTRA_CHANNEL_ID, batteryStateChannelId)
    },
    PendingIntent.FLAG_IMMUTABLE,
  )

  return NotificationCompat.Builder(this, batteryStateChannelId)
    .setContentTitle(getString(R.string.service_notification_content_title))
    .setContentText(getString(R.string.service_notification_content_text))
    .setSmallIcon(R.drawable.ic_notification_service)
    .setTicker(getString(R.string.service_notification_ticker))
    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    .setContentIntent(notificationTapActionPendingIntent)
    .addAction(
      R.drawable.ic_visibility_off,
      getString(R.string.service_notification_channel_settings),
      channelSettingsIntent,
    ).build()
}

/**
 * This function is safe to call multiple times as the Channels get created only once.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun Context.createNotificationServiceChannel(): String {
  val channelId = "PUSH_NOTIFIER_SERVICE"

  val notifierServiceChannel: NotificationChannel = NotificationChannel(
    channelId,
    getString(R.string.notifier_service_channel),
    NotificationManager.IMPORTANCE_LOW, // Lowest level we are allowed to go
  ).apply {
    description = getString(R.string.notifier_service_channel_description)
    setShowBadge(false)
  }

  (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
    notifierServiceChannel
  )

  return channelId
}
