package com.anissan.battarang.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.anissan.battarang.R
import com.anissan.battarang.background.services.BroadcastReceiverRegistererService
import com.anissan.battarang.data.LocalKvStore
import com.anissan.battarang.data.PrefKey
import com.anissan.battarang.databinding.ActivityMainBinding
import com.anissan.battarang.network.MessageType
import com.anissan.battarang.network.ReceiverApiClient
import com.anissan.battarang.ui.views.optimization.restoreOptimizationRequestDialogState
import com.anissan.battarang.ui.views.optimization.saveOptimizationRequestDialogState
import com.anissan.battarang.ui.views.optimization.showOptimizationRequestDialog
import com.anissan.battarang.ui.views.pairing.registerQrScanner
import com.anissan.battarang.ui.views.pairing.restorePairingDialogState
import com.anissan.battarang.ui.views.pairing.savePairingDialogState
import com.anissan.battarang.ui.views.saveEditedText
import com.anissan.battarang.ui.views.setupAppBar
import com.anissan.battarang.ui.views.setupButtonBar
import com.anissan.battarang.ui.views.setupDeviceNameInput
import com.anissan.battarang.ui.views.setupLowBatteryLevelCheckbox
import com.anissan.battarang.ui.views.setupMaxBatteryLevelCheckbox
import com.anissan.battarang.ui.views.setupNotifierServiceToggle
import com.anissan.battarang.ui.views.setupPairReceiverFab
import com.anissan.battarang.ui.views.setupSkipIfDisplayOnToggleCheckbox
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

    setupNotifierServiceToggle()

    setupDeviceNameInput()

    setupMaxBatteryLevelCheckbox()
    setupLowBatteryLevelCheckbox()
    setupSkipIfDisplayOnToggleCheckbox()

    setupButtonBar()

    setupPairReceiverFab()
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
        serviceNameText.text = localKvStore.pairedServiceName

        pairReceiverFab.hide()

        receiverApiClient.sendNotification(MessageType.PAIRED) { response: String? ->
          if (response == null) {
            Toast.makeText(this@MainActivity, R.string.network_error, Toast.LENGTH_LONG).show()
          }
        }

        showOptimizationRequestDialog()
      } else {
        localKvStore.isMonitoringServiceEnabled = false
        localKvStore.pairedServiceTag = null

        unpairButton.visibility = View.GONE
        testButton.visibility = View.GONE
        serviceNameText.text = ""

        pairReceiverFab.show()
      }
    }
  }

  fun refreshServiceState() {
    BroadcastReceiverRegistererService.stop(this@MainActivity)

    val isAnyNotifyOptionChecked =
      localKvStore.isMaxLevelNotificationEnabled || localKvStore.isLowBatteryNotificationEnabled

    val shouldServiceBeEnabled = isAnyNotifyOptionChecked && paired
    val shouldServiceStart = shouldServiceBeEnabled && localKvStore.isMonitoringServiceEnabled

    binding.notifierServiceCard.apply {
      isEnabled = isAnyNotifyOptionChecked
      alpha = if (shouldServiceBeEnabled) 1f else 0.6f
    }

    binding.notifierServiceSwitch.apply {
      isEnabled = shouldServiceBeEnabled
      isChecked = shouldServiceStart
    }

    if (shouldServiceStart) BroadcastReceiverRegistererService.start(this@MainActivity)
  }

  override fun onStop() {
    super.onStop()

    localKvStore.stopObservingChanges()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    binding.deviceNameInput.run {
      if (hasFocus()) saveEditedText(localKvStore)
    }

    savePairingDialogState(outState)
    saveOptimizationRequestDialogState(outState)

    super.onSaveInstanceState(outState)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)

    restorePairingDialogState(savedInstanceState)
    restoreOptimizationRequestDialogState(savedInstanceState)
  }

  fun showSnackbar(stringResId: Int, length: Int = Snackbar.LENGTH_LONG) {
    showSnackbar(getString(stringResId), length)
  }

  fun showSnackbar(text: String, length: Int = Snackbar.LENGTH_LONG) {
    binding.run {
      Snackbar.make(root, text, length).apply {
        anchorView =
          if (pairReceiverFab.visibility == View.VISIBLE) pairReceiverFab else buttonBarCard
      }.show()
    }
  }
}
