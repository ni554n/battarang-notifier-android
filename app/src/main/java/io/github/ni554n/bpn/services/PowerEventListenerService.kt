package io.github.ni554n.bpn.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import io.github.ni554n.bpn.preferences.UserPreferences
import io.github.ni554n.bpn.receivers.PowerEventReceivers
import io.github.ni554n.bpn.ui.MainActivity
import org.koin.android.ext.android.inject

class PowerEventListenerService : Service() {
  private val powerEventReceivers: PowerEventReceivers by inject()
  private val userPreferences: UserPreferences by inject()

  private var isReceiverRegistered = false

  override fun onCreate() {
    // Background services can be killed by the System at anytime.
    // Since Oreo, foreground services with a persistent notification is required for long
    // running tasks.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForeground(128, createNotification())
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (isReceiverRegistered) unregisterReceiver(powerEventReceivers)

    val powerIntentFilters: IntentFilter = IntentFilter().apply {
      userPreferences.run {
        if (isLevelReachedNotificationEnabled) {
          addAction(Intent.ACTION_POWER_CONNECTED)
          addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        if (isLowBatteryNotificationEnabled) {
          addAction(Intent.ACTION_BATTERY_LOW)
        }
      }
    }

    // Starts listening for Power Connect / Disconnect and Low Battery events
    if (powerIntentFilters.countActions() > 0) {
      registerReceiver(powerEventReceivers, powerIntentFilters)

      isReceiverRegistered = true
    }

    return super.onStartCommand(intent, flags, startId)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotification(): Notification {
    val mainActivityPendingIntent: PendingIntent = PendingIntent.getActivity(
      this,
      256,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    return Notification.Builder(this, createBatteryStateChannel())
      .setContentTitle("Listening for Battery related events") // TODO Extract these to string resource
      .setContentText("It is not doing any work. No CPU is being used.")
      .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
      .setContentIntent(mainActivityPendingIntent)
      .setTicker("Battery State Ticker")
      .build()
  }

  // Notification channel will not be created multiple times
  @RequiresApi(Build.VERSION_CODES.O)
  private fun createBatteryStateChannel(): String {
    val batteryStateId = "BATTERY_STATE"

    val batteryStateChannel: NotificationChannel = NotificationChannel(
      batteryStateId,
      "Battery Event Listener",
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = "Listens for battery states"
    }

    (this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
      .createNotificationChannel(batteryStateChannel)

    return batteryStateId
  }

  override fun onDestroy() {
    super.onDestroy()

    unregisterReceiver(powerEventReceivers)

    isReceiverRegistered = false
  }

  override fun onBind(intent: Intent): IBinder? = null
}
