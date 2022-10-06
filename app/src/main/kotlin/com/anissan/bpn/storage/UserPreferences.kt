package com.anissan.bpn.storage

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.core.content.edit

class UserPreferences(
  private val commonSharedPreferences: SharedPreferences,
  private val userSharedPreferences: SharedPreferences,
) {
  companion object {
    const val MONITORING_SERVICE_TOGGLE = "monitoring_service_toggle"
    const val LEVEL_REACHED_NOTIFICATION_TOGGLE = "charging_level_reached_notification_toggle"
    const val CHARGING_LEVEL_PERCENTAGE = "charging_level_percentage"
    const val LOW_BATTERY_NOTIFICATION_TOGGLE = "low_battery_notification_toggle"
    const val SKIP_WHILE_SCREEN_ON_TOGGLE = "skip_while_screen_on_toggle"
    const val NOTIFIER_GCM_TOKEN = "notifier_gcm_token"
    const val USER_DEVICE_NAME = "user_device_name"
  }

  private val commonPreferences: Map<String, *> = commonSharedPreferences.all
  private val userPreferences: Map<String, *> = userSharedPreferences.all

  private lateinit var opf: SharedPreferences.OnSharedPreferenceChangeListener

  // Rename to Notification Service
  var isMonitoringServiceEnabled: Boolean =
    commonPreferences[MONITORING_SERVICE_TOGGLE] as? Boolean ?: true
    set(value) {
      field = value

      commonSharedPreferences.edit { putBoolean(MONITORING_SERVICE_TOGGLE, value) }
    }

  var deviceName: String =
    userPreferences[USER_DEVICE_NAME] as? String ?: "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    set(value) {
      field = value

      userSharedPreferences.edit { putString(USER_DEVICE_NAME, value) }
    }

  var isLevelReachedNotificationEnabled: Boolean =
    commonPreferences[LEVEL_REACHED_NOTIFICATION_TOGGLE] as? Boolean ?: true
    set(value) {
      field = value

      commonSharedPreferences.edit { putBoolean(LEVEL_REACHED_NOTIFICATION_TOGGLE, value) }
    }

  var chargingLevelPercentage: Int = commonPreferences[CHARGING_LEVEL_PERCENTAGE] as? Int ?: 80
    set(value) {
      field = value

      commonSharedPreferences.edit { putInt(CHARGING_LEVEL_PERCENTAGE, value) }
    }

  var isLowBatteryNotificationEnabled: Boolean =
    commonPreferences[LOW_BATTERY_NOTIFICATION_TOGGLE] as? Boolean ?: true
    set(value) {
      field = value

      commonSharedPreferences.edit { putBoolean(LOW_BATTERY_NOTIFICATION_TOGGLE, value) }
    }

  var isSkipWhileDisplayOnEnabled: Boolean =
    commonPreferences[SKIP_WHILE_SCREEN_ON_TOGGLE] as? Boolean ?: true
    set(value) {
      field = value

      commonSharedPreferences.edit { putBoolean(SKIP_WHILE_SCREEN_ON_TOGGLE, value) }
    }

  var notifierGcmToken: String = commonPreferences[NOTIFIER_GCM_TOKEN] as? String ?: ""
    set(value) {
      field = value

      commonSharedPreferences.edit { putString(NOTIFIER_GCM_TOKEN, value) }
    }

  /**
   * Determines if notifications should be sent based on the preference and current display state.
   * */
  fun shouldNotify(context: Context): Boolean {
    // If this option is Selected, make sure every display is OFF before notifying.
    if (isSkipWhileDisplayOnEnabled) {
      return (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
        .displays
        .none { display: Display -> display.state == Display.STATE_ON }
    }

    return true
  }

  fun startObservingChanges(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
    opf = listener

    commonSharedPreferences.registerOnSharedPreferenceChangeListener(opf)
    userSharedPreferences.registerOnSharedPreferenceChangeListener(opf)
  }

  fun stopObservingChanges() {
    commonSharedPreferences.unregisterOnSharedPreferenceChangeListener(opf)
    userSharedPreferences.unregisterOnSharedPreferenceChangeListener(opf)
  }
}
