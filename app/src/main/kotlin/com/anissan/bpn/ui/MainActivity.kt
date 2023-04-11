package com.anissan.bpn.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.anissan.bpn.R
import com.anissan.bpn.background.services.BroadcastReceiverRegistererService
import com.anissan.bpn.data.LocalKvStore
import com.anissan.bpn.data.PrefKey
import com.anissan.bpn.databinding.ActivityMainBinding
import com.anissan.bpn.network.MessageType
import com.anissan.bpn.network.ReceiverApiClient
import com.anissan.bpn.ui.views.optimization.showOptimizationRequestDialog
import com.anissan.bpn.ui.views.pairing.registerQrScanner
import com.anissan.bpn.ui.views.setupAppBar
import com.anissan.bpn.ui.views.setupButtonBar
import com.anissan.bpn.ui.views.setupDeviceNameInput
import com.anissan.bpn.ui.views.setupLowBatteryLevelCheckbox
import com.anissan.bpn.ui.views.setupMaxBatteryLevelCheckbox
import com.anissan.bpn.ui.views.setupPairingFab
import com.anissan.bpn.ui.views.setupServiceToggle
import com.anissan.bpn.ui.views.setupSkipIfDisplayOnToggleCheckbox
import com.google.android.material.snackbar.Snackbar

import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
  val localKvStore: LocalKvStore by inject()
  val receiverApiClient: ReceiverApiClient by inject()

  lateinit var binding: ActivityMainBinding

  val paired: Boolean
    get() = localKvStore.receiverToken.isNullOrBlank().not()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Requesting this layout to be laid out edge-to-edge.
    WindowCompat.setDecorFitsSystemWindows(window, false)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    registerQrScanner()

    /* Each `setup*` function has three responsibilities:
       1. Dynamically adjust the View paddings and margins,
       2. Initialize the View from its saved states, and
       3. Registers the event listeners.
       */

    setupAppBar()

    setupServiceToggle()

    setupDeviceNameInput()

    setupMaxBatteryLevelCheckbox()
    setupLowBatteryLevelCheckbox()
    setupSkipIfDisplayOnToggleCheckbox()

    setupButtonBar()

    setupPairingFab()
  }

  override fun onStart() {
    super.onStart()

    localKvStore.startObservingChanges { key: String ->
      when (key) {
        PrefKey.RECEIVER_TOKEN.name -> refreshAfterTokenUpdate()

        PrefKey.NOTIFICATION_SERVICE_TOGGLE.name,
        PrefKey.MAX_LEVEL_NOTIFICATION_TOGGLE.name,
        PrefKey.LOW_BATTERY_NOTIFICATION_TOGGLE.name,
        -> refreshServiceState()
      }
    }
  }

  private fun refreshAfterTokenUpdate() {
    binding.run {
      if (paired) {
        localKvStore.isMonitoringServiceEnabled = true

        unpairButton.visibility = View.VISIBLE
        testButton.visibility = View.VISIBLE
        serviceNameTextView.text = localKvStore.pairedServiceName

        fabPair.hide()

        receiverApiClient.sendNotification(MessageType.PAIRED) { response: String? ->
          if (response == null) showSnackbar(R.string.network_error)
        }

        showOptimizationRequestDialog()
      } else {
        localKvStore.isMonitoringServiceEnabled = false
        localKvStore.pairedServiceTag = null

        unpairButton.visibility = View.GONE
        testButton.visibility = View.GONE
        serviceNameTextView.text = ""

        fabPair.show()
      }
    }
  }

  fun refreshServiceState() {
    binding.run {
      // At least one of the "NOTIFY WHEN" option needs to be checked for the switch to be enabled.
      val isNotifyWhenEnabled =
        localKvStore.isMaxLevelNotificationEnabled || localKvStore.isLowBatteryNotificationEnabled

      val shouldServiceBeEnabled = isNotifyWhenEnabled && paired

      cardNotificationService.apply {
        alpha = if (shouldServiceBeEnabled) 1f else 0.6f
        isEnabled = isNotifyWhenEnabled
      }

      switchNotificationService.isEnabled = shouldServiceBeEnabled
      switchNotificationService.isChecked =
        shouldServiceBeEnabled && localKvStore.isMonitoringServiceEnabled

      if (switchNotificationService.isChecked) BroadcastReceiverRegistererService.start(this@MainActivity)
      else BroadcastReceiverRegistererService.stop(this@MainActivity)
    }
  }

  override fun onStop() {
    super.onStop()

    localKvStore.stopObservingChanges()
  }

  fun showSnackbar(stringResId: Int, length: Int = Snackbar.LENGTH_LONG) {
    showSnackbar(getString(stringResId), length)
  }

  fun showSnackbar(text: String, length: Int = Snackbar.LENGTH_LONG) {
    binding.run {
      Snackbar.make(root, text, length).apply {
        anchorView = if (fabPair.visibility == View.VISIBLE) fabPair else cardButtonBar
      }.show()
    }
  }
}
