package io.github.ni554n.bpn.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.github.ni554n.bpn.R
import io.github.ni554n.bpn.databinding.ActivityMainBinding
import io.github.ni554n.bpn.network.PushNotification
import io.github.ni554n.bpn.preferences.UserPreferences
import io.github.ni554n.bpn.services.ServiceManager
import logcat.LogPriority
import logcat.logcat
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
  private val userPreferences: UserPreferences by inject()
  private val serviceManager: ServiceManager by inject()
  private val push: PushNotification by inject()

  private val scanQrCode = registerForActivityResult(ScanQRCode(), ::scannedResult)

  private lateinit var mainActivityBinding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the screen
    mainActivityBinding = ActivityMainBinding.inflate(layoutInflater).apply {
      setContentView(root)

      prepareViews()
    }
  }

  /**
   * Initialize views with data and setup listeners.
   */
  private fun ActivityMainBinding.prepareViews() {
    switchNotificationService.run {
      val shouldBeEnabled = refreshNotificationServiceState(this)

      if (shouldBeEnabled) serviceManager.startService()
      else serviceManager.stopService()

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isMonitoringServiceEnabled = isChecked
      }
    }

    editTextDeviceName.run {
      setText(userPreferences.deviceName)

      doAfterTextChanged { changedText: Editable? ->
        userPreferences.deviceName = changedText.toString()
      }
    }

    checkBoxBatteryLevelReached.run {
      isChecked = userPreferences.isLevelReachedNotificationEnabled

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isLevelReachedNotificationEnabled = isChecked
      }
    }

    batteryLevelSlider.run {
      val batteryLevel: Int = userPreferences.chargingLevelPercentage

      value = batteryLevel.toFloat()

      checkBoxBatteryLevelReached.text =
        getString(R.string.battery_level_is_reached_at, batteryLevel)

      addOnChangeListener { _: Slider, value: Float, _: Boolean ->
        val levelValue: Int = value.toInt()

        userPreferences.chargingLevelPercentage = levelValue

        checkBoxBatteryLevelReached.text =
          getString(R.string.battery_level_is_reached_at, levelValue)
      }
    }

    checkBoxBatteryLevelLow.run {
      isChecked = userPreferences.isLowBatteryNotificationEnabled

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isLowBatteryNotificationEnabled = isChecked
      }
    }

    checkBoxIsScreenOn.run {
      isChecked = userPreferences.isNotificationWhileScreenOnEnabled

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isNotificationWhileScreenOnEnabled = isChecked
      }
    }

    refreshFabState(fabPair)

    fabPair.setOnClickListener {
      if (userPreferences.notifierGcmToken.isEmpty()) {
        scanQrCode.launch(null)
      } else {
        userPreferences.notifierGcmToken = ""
      }
    }
  }

  private fun scannedResult(result: QRResult) {
    when (result) {
      QRResult.QRMissingPermission -> logcat { "Missing permission" }
      QRResult.QRUserCanceled -> logcat { "User canceled" }
      is QRResult.QRError -> logcat(LogPriority.ERROR) {
        result.exception.localizedMessage ?: "Error"
      }
      is QRResult.QRSuccess -> {
        val token = result.content.rawValue

        logcat { "GCM TOKEN: $token" }

        userPreferences.notifierGcmToken = token

        // Send a test push notification
        push.notify(token, "Successfully Paired", "It is working correctly!")
      }
    }
  }

  override fun onResume() {
    super.onResume()

    userPreferences.run {
      onChange { _: SharedPreferences, key: String? ->
        if (key == null) return@onChange

        logcat { "$key has been updated." }

        when (key) {
          UserPreferences.MONITORING_SERVICE_TOGGLE -> {
            if (isMonitoringServiceEnabled) serviceManager.startService()
            else serviceManager.stopService()
          }

          // TODO: isMonitoringService is still turned off after token receiving.
          UserPreferences.NOTIFIER_GCM_TOKEN -> {
            isMonitoringServiceEnabled =
              refreshNotificationServiceState(mainActivityBinding.switchNotificationService)

            refreshFabState(mainActivityBinding.fabPair)
            updateCardState()
          }

          UserPreferences.LEVEL_REACHED_NOTIFICATION_TOGGLE,
          UserPreferences.LOW_BATTERY_NOTIFICATION_TOGGLE,
          -> isMonitoringServiceEnabled =
            refreshNotificationServiceState(mainActivityBinding.switchNotificationService)
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()

    userPreferences.stopObservingChanges()
  }

  private fun refreshFabState(fab: ExtendedFloatingActionButton) {
    fab.text = if (userPreferences.notifierGcmToken.isEmpty()) {
      getString(R.string.pair_with_device)
    } else {
      getString(R.string.unpair)
    }
  }

  private fun updateCardState() {
    if (userPreferences.notifierGcmToken.isEmpty()) {
      Snackbar.make(mainActivityBinding.root, R.string.unpaired, Snackbar.LENGTH_SHORT)
        .show()
    } else {
      Snackbar.make(mainActivityBinding.root, R.string.successful_pairing, Snackbar.LENGTH_SHORT)
        .show()
    }
  }

  // Disables notification service if token is empty
  // 1. While setting up the UI
  // 2. After successful pairing & unpairing
  // 3. Both battery reached and low battery checkbox is unchecked
  private fun refreshNotificationServiceState(switchMaterial: SwitchMaterial): Boolean {
    userPreferences.run {
      val isNotifyWhenEnabled = isLevelReachedNotificationEnabled || isLowBatteryNotificationEnabled
      val shouldBeEnabled = notifierGcmToken.isNotEmpty() && isNotifyWhenEnabled
      val shouldBeChecked = shouldBeEnabled && isMonitoringServiceEnabled

      switchMaterial.isEnabled = shouldBeEnabled
      switchMaterial.isChecked = shouldBeChecked

      return shouldBeChecked
    }
  }
}
