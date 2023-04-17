package com.anissan.battarang.data

import android.content.Context
import android.content.SharedPreferences
import com.anissan.battarang.network.SupportedService
import com.anissan.battarang.ui.views.defaultDeviceName
import com.anissan.battarang.utils.logV
import hu.autsoft.krate.SimpleKrate
import hu.autsoft.krate.booleanPref
import hu.autsoft.krate.default.withDefault
import hu.autsoft.krate.intPref
import hu.autsoft.krate.stringPref

/**
 * ⚠️ If not careful, renaming these can become a breaking change and stored values will be lost.
 */
enum class PrefKey {
  DEVICE_NAME,
  NOTIFICATION_SERVICE_TOGGLE,
  MAX_LEVEL_NOTIFICATION_TOGGLE,
  MAX_LEVEL_PERCENTAGE,
  LOW_BATTERY_NOTIFICATION_TOGGLE,
  SKIP_WHILE_SCREEN_ON_TOGGLE,
  PAIRED_SERVICE_TAG,
  RECEIVER_TOKEN,
  LAST_MESSAGE_ID,
}

/**
 * Local KV store for saving app states and user preferences into the default SharedPreferences.
 */
class LocalKvStore(context: Context) : SimpleKrate(context),
  SharedPreferences.OnSharedPreferenceChangeListener {

  var deviceName: String
    by stringPref(PrefKey.DEVICE_NAME.name).withDefault(context.defaultDeviceName)

  var isMonitoringServiceEnabled: Boolean
    by booleanPref(PrefKey.NOTIFICATION_SERVICE_TOGGLE.name).withDefault(true)

  var isMaxLevelNotificationEnabled: Boolean
    by booleanPref(PrefKey.MAX_LEVEL_NOTIFICATION_TOGGLE.name).withDefault(true)

  var maxChargingLevelPercentage: Int
    by intPref(PrefKey.MAX_LEVEL_PERCENTAGE.name).withDefault(85)

  var isLowBatteryNotificationEnabled: Boolean
    by booleanPref(PrefKey.LOW_BATTERY_NOTIFICATION_TOGGLE.name).withDefault(true)

  var isSkipWhileDisplayOnEnabled: Boolean
    by booleanPref(PrefKey.SKIP_WHILE_SCREEN_ON_TOGGLE.name).withDefault(true)

  /**
   * It's going to be either the FCM token generated on the receiver device
   * or the current Chat ID from the Telegram Bot.
   * */
  var receiverToken: String? by stringPref(PrefKey.RECEIVER_TOKEN.name)

  /** This ID is going to be used to delete the last sent message to keep the message history at minimum. */
  var lastTelegramMessageId: String? by stringPref(PrefKey.LAST_MESSAGE_ID.name)

  /** Must be one of the [SupportedService]. */
  var pairedServiceTag: String? by stringPref(PrefKey.PAIRED_SERVICE_TAG.name)

  val pairedServiceName: String
    get() {
      return if (receiverToken === null) "" else SupportedService.valueOf(pairedServiceTag!!).serviceName
    }

  //endregion

  private lateinit var _changeListener: (key: String) -> Unit

  fun startObservingChanges(changeListener: (key: String) -> Unit) {
    _changeListener = changeListener

    sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    logV { "Started observing sharedPreferences changes…" }
  }

  fun stopObservingChanges() {
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    logV { "Stopped observing for sharedPreferences changes." }
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    logV { "$key has been updated by user" }
    if (key == null) return

    _changeListener(key)
  }
}
