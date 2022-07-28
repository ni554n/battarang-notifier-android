package io.github.ni554n.bpn.event

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.ni554n.bpn.BootEventReceiver.Companion.resumeAfterBoot
import io.github.ni554n.bpn.event.receivers.AlarmBroadcastReceivers
import io.github.ni554n.bpn.event.receivers.PowerBroadcastReceivers
import io.github.ni554n.bpn.ui.buildServiceNotification
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
