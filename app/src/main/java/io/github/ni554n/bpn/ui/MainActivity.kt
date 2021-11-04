package io.github.ni554n.bpn.ui

import `in`.aabhasjindal.otptextview.OTPListener
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
import io.github.ni554n.bpn.R
import io.github.ni554n.bpn.databinding.ActivityMainBinding
import io.github.ni554n.bpn.network.PushNotification
import io.github.ni554n.bpn.network.enqueue
import io.github.ni554n.bpn.network.getToken
import io.github.ni554n.bpn.preferences.UserPreferences
import io.github.ni554n.bpn.services.ServiceManager
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import okhttp3.Response
import org.koin.android.ext.android.inject
import java.io.IOException

class MainActivity : AppCompatActivity() {
  private val userPreferences: UserPreferences by inject()
  private val serviceManager: ServiceManager by inject()
  private val push: PushNotification by inject()

  private lateinit var mainActivityBinding: ActivityMainBinding
  private lateinit var fabToCard: FabToCard

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

    // Sets up the FAB.
    fabToCard = FabToCard(this@MainActivity, this)

    // Closes pairing dialog on back press.
    onBackPressedDispatcher.addCallback(this@MainActivity, fabToCard)

    refreshFabState(fabPair)

    fabPair.setOnClickListener {
      if (userPreferences.notifierGcmToken.isEmpty()) {
        fabToCard.toggleCardVisibility()
      } else {
        userPreferences.notifierGcmToken = ""
      }
    }

    pairOtp.run {
      otpListener = object : OTPListener {
        override fun onInteractionListener() {}

        override fun onOTPComplete(otp: String) {
          logcat { "OTP Input: $otp" }

          getToken(otp).enqueue(
            failure = { error: IOException -> logcat(LogPriority.ERROR) { error.asLog() } }
          ) { response: Response ->
            val token: String = response.body?.string().orEmpty()

            logcat { "GCM TOKEN: $token" }

            userPreferences.notifierGcmToken = token

            // Send a test push notification
            push.notify(token, "Successfully Paired", "It is working correctly!")
          }
        }
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
            updateCardState(fabToCard)
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

  private fun updateCardState(fabToCard: FabToCard) {
    if (userPreferences.notifierGcmToken.isEmpty()) {
      Snackbar.make(mainActivityBinding.root, R.string.unpaired, Snackbar.LENGTH_SHORT)
        .show()
    } else {
      fabToCard.toggleCardVisibility()

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
