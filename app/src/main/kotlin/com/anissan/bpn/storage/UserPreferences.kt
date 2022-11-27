package com.anissan.bpn.storage

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.core.content.edit

/**
 * "Common" prefs are going to be backed up to the cloud and restored upon re-installation.
 * "User" prefs won't be synced.
 */
class UserPreferences(
  private val commonSharedPreferences: SharedPreferences,
  private val userSharedPreferences: SharedPreferences,
) {
  private val commonKVs: Map<String, *> = commonSharedPreferences.all
  private val userKVs: Map<String, *> = userSharedPreferences.all

  companion object {
    const val MONITORING_SERVICE_TOGGLE_KEY = "monitoring_service_toggle"
    const val MAX_LEVEL_NOTIFICATION_TOGGLE_KEY = "max_charging_level_notification_toggle"
    const val CHARGING_LEVEL_PERCENTAGE_KEY = "charging_level_percentage"
    const val LOW_BATTERY_NOTIFICATION_TOGGLE_KEY = "low_battery_notification_toggle"
    const val SKIP_WHILE_SCREEN_ON_TOGGLE_KEY = "skip_while_screen_on_toggle"
    const val NOTIFIER_GCM_TOKEN_KEY = "notifier_gcm_token"
    const val USER_DEVICE_NAME_KEY = "user_device_name"

    val DEFAULT_DEVICE_NAME = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Unknown" }
  }

  var isMonitoringServiceEnabled: Boolean =
    userKVs[MONITORING_SERVICE_TOGGLE_KEY] as? Boolean ?: true
    set(value) {
      field = value

      userSharedPreferences.edit { putBoolean(MONITORING_SERVICE_TOGGLE_KEY, value) }
    }

  var deviceName: String = userKVs[USER_DEVICE_NAME_KEY] as? String ?: DEFAULT_DEVICE_NAME
    set(value) {
      field = value

      userSharedPreferences.edit { putString(USER_DEVICE_NAME_KEY, value) }
    }

  var isMaxLevelNotificationEnabled: Boolean =
    commonKVs[MAX_LEVEL_NOTIFICATION_TOGGLE_KEY] as? Boolean ?: true
    set(value) {
      field = value

      commonSharedPreferences.edit { putBoolean(MAX_LEVEL_NOTIFICATION_TOGGLE_KEY, value) }
    }

  var maxChargingLevelPercentage: Int = commonKVs[CHARGING_LEVEL_PERCENTAGE_KEY] as? Int ?: 85
    set(value) {
      field = value

      commonSharedPreferences.edit { putInt(CHARGING_LEVEL_PERCENTAGE_KEY, value) }
    }

  var isLowBatteryNotificationEnabled: Boolean =
    commonKVs[LOW_BATTERY_NOTIFICATION_TOGGLE_KEY] as? Boolean ?: true
    set(value) {
      field = value

      commonSharedPreferences.edit { putBoolean(LOW_BATTERY_NOTIFICATION_TOGGLE_KEY, value) }
    }

  var isSkipWhileDisplayOnEnabled: Boolean =
    commonKVs[SKIP_WHILE_SCREEN_ON_TOGGLE_KEY] as? Boolean ?: true
    set(value) {
      field = value

      commonSharedPreferences.edit { putBoolean(SKIP_WHILE_SCREEN_ON_TOGGLE_KEY, value) }
    }

  var notifierGcmToken: String = commonKVs[NOTIFIER_GCM_TOKEN_KEY] as? String ?: ""
    set(value) {
      field = value

      commonSharedPreferences.edit { putString(NOTIFIER_GCM_TOKEN_KEY, value) }
    }

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

    commonSharedPreferences.registerOnSharedPreferenceChangeListener(_changeListener)
    userSharedPreferences.registerOnSharedPreferenceChangeListener(_changeListener)
  }

  fun stopObservingChanges() {
    commonSharedPreferences.unregisterOnSharedPreferenceChangeListener(_changeListener)
    userSharedPreferences.unregisterOnSharedPreferenceChangeListener(_changeListener)
  }
}
