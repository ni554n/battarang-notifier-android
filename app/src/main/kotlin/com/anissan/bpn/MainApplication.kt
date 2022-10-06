package com.anissan.bpn

import android.app.Application
import android.os.PowerManager
import com.google.android.material.color.DynamicColors
import com.anissan.bpn.api.PushServerClient
import com.anissan.bpn.storage.UserPreferences
import com.anissan.bpn.event.receivers.PowerBroadcastReceivers
import com.anissan.bpn.event.receivers.AlarmBroadcastReceivers
import com.anissan.bpn.event.receivers.handlers.BroadcastedEventHandlers
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    // Enables app wide dynamic theme on Android 12+ using the Material library.
    DynamicColors.applyToActivitiesIfAvailable(this)

    // Setup "Logcat" for debug builds, skip in release builds.
    AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

    /* Dependency Injection */
    startKoin {
      androidLogger()
      androidContext(this@MainApplication)

      modules(module {
        single {
          UserPreferences(
            androidContext().getSharedPreferences("common", MODE_PRIVATE),
            androidContext().getSharedPreferences("user", MODE_PRIVATE),
          )
        }

        single {
          PushServerClient(
            androidContext().getSystemService(POWER_SERVICE) as PowerManager,
            OkHttpClient(),
          )
        }

        singleOf(::PowerBroadcastReceivers)
        singleOf(::AlarmBroadcastReceivers)
        singleOf(::BroadcastedEventHandlers)
      })
    }
  }
}
