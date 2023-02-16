package com.anissan.bpn.storage

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import hu.autsoft.krate.SimpleKrate
import hu.autsoft.krate.booleanPref
import hu.autsoft.krate.default.withDefault
import hu.autsoft.krate.intPref
import hu.autsoft.krate.stringPref

// Renaming any of these can become a breaking change and stored value will be lost
// as they are used as Preference key.
enum class PrefKey {
  THIS_DEVICE_NAME,
  NOTIFICATION_SERVICE_TOGGLE,
  MAX_LEVEL_NOTIFICATION_TOGGLE,
  MAX_CHARGING_LEVEL_PERCENTAGE,
  LOW_BATTERY_NOTIFICATION_TOGGLE,
  SKIP_WHILE_SCREEN_ON_TOGGLE,
  RECEIVER_TOKEN,
}

class UserPreferences(context: Context) : SimpleKrate(context) {
  companion object {
    val DEFAULT_DEVICE_NAME = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Unknown" }
  }

  var deviceName: String
    by stringPref(PrefKey.THIS_DEVICE_NAME.name).withDefault(DEFAULT_DEVICE_NAME)

  var isMonitoringServiceEnabled: Boolean
    by booleanPref(PrefKey.NOTIFICATION_SERVICE_TOGGLE.name).withDefault(true)

  var isMaxLevelNotificationEnabled: Boolean
    by booleanPref(PrefKey.MAX_LEVEL_NOTIFICATION_TOGGLE.name).withDefault(true)

  var maxChargingLevelPercentage: Int
    by intPref(PrefKey.MAX_CHARGING_LEVEL_PERCENTAGE.name).withDefault(85)

  var isLowBatteryNotificationEnabled: Boolean
    by booleanPref(PrefKey.LOW_BATTERY_NOTIFICATION_TOGGLE.name).withDefault(true)

  var isSkipWhileDisplayOnEnabled: Boolean
    by booleanPref(PrefKey.SKIP_WHILE_SCREEN_ON_TOGGLE.name).withDefault(true)

  var receiverToken: String? by stringPref(PrefKey.RECEIVER_TOKEN.name)

  /**
   * Determines if notifications should be sent based on user preference and current display state.
   * */
  fun shouldNotify(context: Context): Boolean {
    if (isSkipWhileDisplayOnEnabled) {
      // Make sure every display is OFF before notifying.
      return (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
        .displays
        .none { display: Display -> display.state == Display.STATE_ON }
    }

    return true
  }

  private lateinit var _changeListener: SharedPreferences.OnSharedPreferenceChangeListener

  fun startObservingChanges(changeListener: SharedPreferences.OnSharedPreferenceChangeListener) {
    _changeListener = changeListener

    sharedPreferences.registerOnSharedPreferenceChangeListener(_changeListener)
  }

  fun stopObservingChanges() {
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(_changeListener)
  }
}
