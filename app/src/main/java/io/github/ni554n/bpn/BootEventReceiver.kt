package io.github.ni554n.bpn

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.github.ni554n.bpn.event.BatteryEventReceiversRegisterService
import logcat.LogPriority
import logcat.logcat

class BootEventReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null) {
      logcat(LogPriority.WARN) { "onReceive() received a null Context" }
      return
    }

    if (intent == null) {
      logcat(LogPriority.WARN) { "onReceive() received a null Intent" }
      return
    }

    logcat { """onReceive() received "${intent.action}" action""" }

    when (intent.action) {
      "android.intent.action.BOOT_COMPLETED",
      "android.intent.action.QUICKBOOT_POWERON",
      "com.htc.intent.action.QUICKBOOT_POWERON",
      "android.intent.action.MY_PACKAGE_REPLACED",
      -> BatteryEventReceiversRegisterService.startForeground(context)
    }
  }

  companion object {
    /**
     * Instead of managing a separate state for syncing the service status with the receiver,
     * we are going to enable or disable the boot receiver component itself.
     * */
    fun Context.toggleBootReceiverComponent(state: Int) {
      packageManager.setComponentEnabledSetting(
        ComponentName(this, BootEventReceiver::class.java),
        state,
        PackageManager.DONT_KILL_APP,
      )

      val stateStatus: String =
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) "enabled" else "disabled"

      logcat { "BootReceiverComponent is now $stateStatus" }
    }
  }
}
