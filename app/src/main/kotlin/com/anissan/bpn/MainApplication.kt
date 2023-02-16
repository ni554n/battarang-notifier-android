package com.anissan.bpn

import android.app.Application
import android.os.PowerManager
import com.anissan.bpn.background.receivers.BatteryLevelPollingAlarmReceiver
import com.anissan.bpn.background.receivers.BatteryStatusReceiver
import com.anissan.bpn.background.receivers.handlers.BroadcastedEventHandlers
import com.anissan.bpn.network.PushServerClient
import com.anissan.bpn.storage.UserPreferences
import com.anissan.bpn.utils.SystemLogBackend
import com.anissan.bpn.utils.Ulog
import com.google.android.material.color.DynamicColors
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

    // Setup "ulog" for debug builds, skip in release builds.
    Ulog.installBackend(SystemLogBackend())

    /* Dependency Injection */
    startKoin {
      androidLogger()
      androidContext(this@MainApplication)

      modules(module {
        single { UserPreferences(androidContext()) }

        single {
          PushServerClient(
            androidContext().getSystemService(POWER_SERVICE) as PowerManager,
            OkHttpClient(),
          )
        }

        singleOf(::BatteryStatusReceiver)
        singleOf(::BatteryLevelPollingAlarmReceiver)
        singleOf(::BroadcastedEventHandlers)
      })
    }
  }
}
