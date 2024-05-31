package com.anissan.battarang.background.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anissan.battarang.background.services.BroadcastReceiverRegistererService
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV

/**
 * Receives an event after boot and app update.
 * */
class BootEventReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null) return

    val action = intent?.action ?: return
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
}
