package io.github.ni554n.bpn.preferences

import android.content.SharedPreferences
import android.os.Build
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
    const val NOTIFICATION_WHILE_SCREEN_ON = "notification_while_screen_on_toggle"
    const val NOTIFIER_GCM_TOKEN = "notifier_gcm_token"
    const val USER_DEVICE_NAME = "user_device_name"
  }

  private val commonPreferences: Map<String, *> = commonSharedPreferences.all
  private val userPreferences: Map<String, *> = userSharedPreferences.all

  private lateinit var opf: SharedPreferences.OnSharedPreferenceChangeListener

  // Rename to Notification Service
  var isMonitoringServiceEnabled: Boolean =
    commonPreferences[MONITORING_SERVICE_TOGGLE] as? Boolean ?: false
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

  // Replace Enabled with Checked
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

  var isNotificationWhileScreenOnEnabled: Boolean =
    commonPreferences[NOTIFICATION_WHILE_SCREEN_ON] as? Boolean ?: false
    set(value) {
      field = value

      commonSharedPreferences.edit { putBoolean(NOTIFICATION_WHILE_SCREEN_ON, value) }
    }

  var notifierGcmToken: String = commonPreferences[NOTIFIER_GCM_TOKEN] as? String ?: ""
    set(value) {
      field = value

      commonSharedPreferences.edit { putString(NOTIFIER_GCM_TOKEN, value) }
    }

  fun onChange(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
    opf = listener

    commonSharedPreferences.registerOnSharedPreferenceChangeListener(opf)
    userSharedPreferences.registerOnSharedPreferenceChangeListener(opf)
  }

  fun stopObservingChanges() {
    commonSharedPreferences.unregisterOnSharedPreferenceChangeListener(opf)
    userSharedPreferences.unregisterOnSharedPreferenceChangeListener(opf)
  }
}
