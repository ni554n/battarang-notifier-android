package com.anissan.battarang

import android.app.Application
import android.os.PowerManager
import androidx.appcompat.app.AppCompatDelegate
import com.anissan.battarang.background.receivers.BatteryLevelCheckerAlarmReceiver
import com.anissan.battarang.background.receivers.BatteryLowReceiver
import com.anissan.battarang.background.receivers.ChargerConnectedReceiver
import com.anissan.battarang.background.receivers.ChargerConnectionReceiver
import com.anissan.battarang.background.receivers.handlers.BroadcastedEventHandlers
import com.anissan.battarang.data.LocalKvStore
import com.anissan.battarang.network.ReceiverApiClient
import com.anissan.battarang.utils.SystemLogBackend
import com.anissan.battarang.utils.Ulog
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

    if (DynamicColors.isDynamicColorAvailable().not()) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    // Enables app wide dynamic theme on Android 12+ using the Material library.
    DynamicColors.applyToActivitiesIfAvailable(this)

    // Setup "ulog" for debug builds, skip in release builds.
    Ulog.installBackend(SystemLogBackend())

    /* Dependency Injection */
    startKoin {
      androidLogger()
      androidContext(this@MainApplication)

      modules(module {
        single { LocalKvStore(androidContext()) }

        single {
          ReceiverApiClient(
            powerManager = androidContext().getSystemService(POWER_SERVICE) as PowerManager,
            okHttpClient = OkHttpClient(),
            localKvStore = get(),
          )
        }

        singleOf(::BatteryLowReceiver)
        singleOf(::ChargerConnectedReceiver)
        singleOf(::ChargerConnectionReceiver)
        singleOf(::BatteryLevelCheckerAlarmReceiver)
        singleOf(::BroadcastedEventHandlers)
      })
    }
  }
}
