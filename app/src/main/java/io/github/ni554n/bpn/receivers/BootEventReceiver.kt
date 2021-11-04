package io.github.ni554n.bpn.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ni554n.bpn.services.ServiceManager
import logcat.LogPriority
import logcat.logcat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootEventReceiver : BroadcastReceiver(), KoinComponent {
  private val serviceManager: ServiceManager by inject()

  private val bootActions: HashSet<String> = hashSetOf(
    "android.intent.action.BOOT_COMPLETED",
    "android.intent.action.QUICKBOOT_POWERON",
    "com.htc.intent.action.QUICKBOOT_POWERON",
    "android.intent.action.MY_PACKAGE_REPLACED",
  )

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null || intent == null) {
      logcat(LogPriority.ERROR) {
        "onReceive(context = ${context.toString()}, intent = ${intent.toString()})"
      }

      return
    }

    if (bootActions.contains(intent.action)) serviceManager.startService()
  }
}
