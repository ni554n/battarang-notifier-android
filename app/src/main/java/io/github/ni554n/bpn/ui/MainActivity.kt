package io.github.ni554n.bpn.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
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

  private val scanQrCode = registerForActivityResult(ScanQRCode(), ::handleScannedResult)

  private lateinit var mainActivityBinding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Requesting to be laid out edge-to-edge.
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Initialize the screen
    mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)

    mainActivityBinding.run {
      setContentView(root)

      prepareViews()
    }
  }

  /**
   * Initialize views with data and setup listeners.
   */
  private fun ActivityMainBinding.prepareViews() {
    switchNotificationService.run {
      refreshNotificationServiceState()

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isMonitoringServiceEnabled = isChecked
      }
    }

    editTextDeviceName.run {
      setText(userPreferences.deviceName)

      doAfterTextChanged { changedText: Editable? ->
        userPreferences.deviceName = changedText.toString()
      }

      ViewCompat.setOnApplyWindowInsetsListener(root) { _: View, insets: WindowInsetsCompat ->
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

        if (imeVisible) fabPair.hide()
        else {
          if (hasFocus()) clearFocus()

          fabPair.show()
        }

        insets
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

    fabPair.run {
      refreshFabState()

      fabPair.setOnClickListener {
        if (userPreferences.notifierGcmToken.isEmpty()) {
          scanQrCode.launch(null)
        } else {
          userPreferences.notifierGcmToken = ""
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()

    userPreferences.run {
      startObservingChanges { _: SharedPreferences, key: String? ->
        if (key == null) return@startObservingChanges

        logcat { "$key has been updated." }

        when (key) {
          UserPreferences.MONITORING_SERVICE_TOGGLE,
          UserPreferences.LEVEL_REACHED_NOTIFICATION_TOGGLE,
          UserPreferences.LOW_BATTERY_NOTIFICATION_TOGGLE,
          -> refreshNotificationServiceState()

          UserPreferences.NOTIFIER_GCM_TOKEN -> refreshBecauseTokenChanged()
        }
      }
    }

    logcat { "onResume: Started observing for sharedPreferences changes" }
  }

  override fun onPause() {
    super.onPause()

    userPreferences.stopObservingChanges()

    logcat { "onPause: Stopped observing for sharedPreferences changes" }
  }

  private fun refreshBecauseTokenChanged() {
    refreshNotificationServiceState()

    refreshFabState()

    if (userPreferences.notifierGcmToken.isEmpty()) {
      Snackbar.make(mainActivityBinding.root, R.string.unpaired, Snackbar.LENGTH_SHORT)
        .show()
    } else {
      Snackbar.make(
        mainActivityBinding.root,
        R.string.successful_pairing,
        Snackbar.LENGTH_SHORT
      ).show()
    }
  }

  // Disables notification service if token is empty
  // 1. While setting up the UI
  // 2. After successful pairing & unpairing
  // 3. Both battery reached and low battery checkbox is unchecked
  private fun refreshNotificationServiceState() {
    val switchMaterial: SwitchMaterial = mainActivityBinding.switchNotificationService

    userPreferences.run {
      val isNotifyWhenEnabled = isLevelReachedNotificationEnabled || isLowBatteryNotificationEnabled
      val shouldBeEnabled = isNotifyWhenEnabled && notifierGcmToken.isNotEmpty()
      val shouldBeChecked = shouldBeEnabled && isMonitoringServiceEnabled

      switchMaterial.isEnabled = shouldBeEnabled
      switchMaterial.isChecked = shouldBeChecked

      if (shouldBeChecked) serviceManager.startService()
      else serviceManager.stopService()
    }
  }

  private fun refreshFabState() {
    mainActivityBinding.fabPair.run {
      if (userPreferences.notifierGcmToken.isEmpty()) {
        text = getString(R.string.pair_with_device)
        setIconResource(R.drawable.ic_fluent_qr_code_filled_24)
      } else {
        text = getString(R.string.unpair)
        setIconResource(R.drawable.ic_material_link_off_round_24)
      }
    }
  }

  private fun handleScannedResult(result: QRResult) {
    userPreferences.chargingLevelPercentage = 80

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
        userPreferences.isMonitoringServiceEnabled = true

        // This function is called before onResume has a chance to start observing the sharedPref changes.
        // Manual refresh is required here to update the screen state.
        refreshBecauseTokenChanged()

        // Send a test push notification
        push.notify(token, "Successfully Paired", "It is working correctly!")
      }
    }
  }
}
