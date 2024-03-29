package com.anissan.battarang.background

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.anissan.battarang.background.services.BroadcastReceiverRegistererService
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV
import com.anissan.battarang.utils.logW

/**
 * Receives the boot completed event as well as the event after an app update.
 *
 * This Broadcast Receiver component can be enabled or disabled by the
 * function in companion object.
 * */
class BootEventReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null) {
      logW { "onReceive() received a null Context" }
      return
    }

    val action = intent?.action ?: run {
      logW { "onReceive() received a null Intent" }
      return
    }

    logV { """Received the Intent Action: "$action"""" }

    when (action) {
      "android.intent.action.BOOT_COMPLETED",
      "android.intent.action.QUICKBOOT_POWERON",
      "com.htc.intent.action.QUICKBOOT_POWERON",
      "android.intent.action.MY_PACKAGE_REPLACED",
      -> BroadcastReceiverRegistererService.start(context)

      else -> logE { "$action is not a supported action by this receiver" }
    }
  }

  companion object {
    /**
     * Instead of managing a separate state for syncing the service status preference with
     * the receiver, we are going to enable or disable the boot receiver component itself.
     * So, if the boot receiver component is enabled, we can enable the notification service
     * straight away.
     * */
    fun Context.resumeAfterBoot(toggle: Boolean) {
      val state =
        if (toggle) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

      packageManager.setComponentEnabledSetting(
        ComponentName(this, BootEventReceiver::class.java),
        state,
        PackageManager.DONT_KILL_APP,
      )

      val stateStatus: String = if (toggle) "enabled" else "disabled"
      logV { "Boot event Receiver Component is now $stateStatus." }
    }
  }
}
