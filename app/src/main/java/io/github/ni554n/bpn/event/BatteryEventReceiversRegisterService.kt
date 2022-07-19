package io.github.ni554n.bpn.event

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.ni554n.bpn.BootEventReceiver.Companion.toggleBootReceiverComponent
import io.github.ni554n.bpn.MainActivity
import io.github.ni554n.bpn.event.receivers.BatteryEventReceivers
import logcat.logcat
import org.koin.android.ext.android.inject

/**
 * Most of the implicit broadcast receivers no longer work if declared in Manifest.
 * Hence, a service is needed to dynamically register the receivers.
 */
class BatteryEventReceiversRegisterService : Service() {
  private val batteryEventReceivers: BatteryEventReceivers by inject()

  override fun onCreate() {
    // Background services can be killed by the System at anytime.
    // Since Oreo, foreground services with a persistent notification is required for long
    // running tasks.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForeground(NOTIFICATION_ID, createForegroundNotification())
    }

    val batteryEventIntentFilters: IntentFilter =
      batteryEventReceivers.getBatteryEventIntentFilters()

    if (batteryEventIntentFilters.countActions() > 0) {
      registerReceiver(batteryEventReceivers, batteryEventIntentFilters)

      // If charger is already connected before starting the service,
      // broadcast it to the receiver to sync the state
      val batteryStatus: Int? = registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
      )?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

      if (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(Intent.ACTION_POWER_CONNECTED))
      }

      logcat { "Registered PowerEventReceivers to listen for power connect/disconnect and low battery events" }
    }

    toggleBootReceiverComponent(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  fun createForegroundNotification(): Notification {
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
  private fun createBatteryStateChannel(): String {
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

  override fun onDestroy() {
    unregisterReceiver(batteryEventReceivers)
    toggleBootReceiverComponent(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)

    super.onDestroy()
  }

  override fun onBind(intent: Intent): IBinder? = null

  companion object {
    private const val NOTIFICATION_ID = 128

    private lateinit var powerEventServiceIntent: Intent

    // Background services can be killed at any time, so a foreground service is required.
    fun startForeground(context: Context) {
      powerEventServiceIntent = Intent(context, BatteryEventReceiversRegisterService::class.java)

      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        context.startForegroundService(powerEventServiceIntent)
      } else {
        context.startService(powerEventServiceIntent)
      }

      logcat { "Started this foreground service successfully" }
    }

    fun stopForeground(context: Context) {
      if (::powerEventServiceIntent.isInitialized.not()) return

      context.stopService(powerEventServiceIntent)
      logcat { "Stopped the foreground service" }
    }
  }
}
