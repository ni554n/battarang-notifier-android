package io.github.ni554n.bpn

import android.app.Application
import android.content.Context
import android.os.PowerManager
import com.google.android.material.color.DynamicColors
import io.github.ni554n.bpn.network.PushNotification
import io.github.ni554n.bpn.preferences.UserPreferences
import io.github.ni554n.bpn.receivers.PowerEventReceivers
import io.github.ni554n.bpn.services.ServiceManager
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

// This class is falsely reported as unused because of an Android Studio bug.
// https://issuetracker.google.com/issues/74514347
@Suppress("unused")
class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    // Enables Android 12 app wide dynamic theme using the Material library.
    DynamicColors.applyToActivitiesIfAvailable(this)

    // Log all priorities in debug builds, no-op in release builds.
    AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

    // Dependency Injection stuff
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

        single { ServiceManager(androidContext()) }

        single {
          PushNotification(
            androidContext().getSystemService(Context.POWER_SERVICE) as PowerManager,
          )
        }

        single { PowerEventReceivers() }
      })
    }
  }
}
