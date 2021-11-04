package io.github.ni554n.bpn.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.github.ni554n.bpn.receivers.BootEventReceiver

class ServiceManager(private val context: Context) {
  private val powerEventServiceIntent = Intent(context, PowerEventListenerService::class.java)

  fun startService() {
    // Most of the implicit broadcast receivers no longer work if declared from Manifest.
    // Hence, a service is needed to actively register the receivers.
    // Background services can be killed at any time, so a foreground service is the way to go.
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      // startForeground method should be called from the Service within 10 seconds.
      context.startForegroundService(powerEventServiceIntent)
    } else {
      context.startService(powerEventServiceIntent)
    }

    context.packageManager.setComponentEnabledSetting(
      ComponentName(context, BootEventReceiver::class.java),
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP,
    )
  }

  fun stopService() {
    context.stopService(powerEventServiceIntent)

    context.packageManager.setComponentEnabledSetting(
      ComponentName(context, BootEventReceiver::class.java),
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP,
    )
  }
}
