package com.anissan.bpn

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.anissan.bpn.api.PushServerClient
import com.anissan.bpn.databinding.ActivityMainBinding
import com.anissan.bpn.databinding.DialogPairBinding
import com.anissan.bpn.event.BroadcastReceiverRegistererService
import com.anissan.bpn.storage.UserPreferences
import com.anissan.bpn.utils.logE
import com.anissan.bpn.utils.logV
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import dev.chrisbanes.insetter.applyInsetter
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
  private val userPreferences: UserPreferences by inject()
  private val pushServerClient: PushServerClient by inject()

  private val scanQrCode = registerForActivityResult(ScanCustomCode(), ::handleScannedResult)
  private val qrConfig = ScannerConfig.build {
    setBarcodeFormats(listOf(BarcodeFormat.FORMAT_QR_CODE))
    setOverlayDrawableRes(R.drawable.ic_qr_code_48)
    setOverlayStringRes(R.string.quickie_overlay_string)
    setHapticSuccessFeedback(true)
  }

  private lateinit var mainActivityBinding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Requesting to be laid out edge-to-edge.
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Initialize the screen
    mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)

    mainActivityBinding.run {
      setContentView(root)
      insetViewPositions()
      setupViews()
    }
  }

  /**
   * Prevents views from overlapping with the system elements such as status bar or navigation bar.
   * */
  private fun ActivityMainBinding.insetViewPositions() {
    collapsingToolbarLayout.applyInsetter {
      type(navigationBars = true) {
        margin(horizontal = true)
      }
    }

    switchNotificationServiceContainer.applyInsetter {
      type(navigationBars = true) {
        padding(horizontal = true)
      }
    }

    nestedScrollView.applyInsetter {
      type(navigationBars = true) {
        margin(horizontal = true)
      }
    }

    fabPair.applyInsetter {
      type(navigationBars = true) {
        margin(horizontal = true, vertical = true)
      }
    }
  }

  /**
   * Initialize views with user data and setup listeners along the way.
   */
  private fun ActivityMainBinding.setupViews() {
    appBarLayout.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this@MainActivity))

    /* Service Switcher */
    switchNotificationService.run {
      refreshNotificationServiceState()

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isMonitoringServiceEnabled = isChecked
      }
    }

    /* Device Name Input */
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

    /* Battery Level Reaches Toggle  */
    checkBoxBatteryLevelReached.run {
      isChecked = userPreferences.isLevelReachedNotificationEnabled

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isLevelReachedNotificationEnabled = isChecked
      }
    }

    /* Battery Level Slider  */
    batteryLevelSlider.run {
      val batteryLevel: Int = userPreferences.chargingLevelPercentage

      value = batteryLevel.toFloat()

      // Template for bolding out the percentage value
      val batteryLevelReachesStringTemplate = getString(R.string.battery_level_is_reached_at)

      checkBoxBatteryLevelReached.text =
        HtmlCompat.fromHtml(
          batteryLevelReachesStringTemplate.format(batteryLevel),
          HtmlCompat.FROM_HTML_MODE_COMPACT
        )

      addOnChangeListener { _: Slider, value: Float, _: Boolean ->
        val levelValue: Int = value.toInt()

        userPreferences.chargingLevelPercentage = levelValue

        checkBoxBatteryLevelReached.text =
          HtmlCompat.fromHtml(
            batteryLevelReachesStringTemplate.format(levelValue),
            HtmlCompat.FROM_HTML_MODE_COMPACT
          )
      }
    }

    /* Low Battery Level Toggle */
    checkBoxBatteryLevelLow.run {
      isChecked = userPreferences.isLowBatteryNotificationEnabled

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isLowBatteryNotificationEnabled = isChecked
      }
    }

    /* Skip if Display On Toggle*/
    checkBoxSkipWhileDisplayOn.run {
      isChecked = userPreferences.isSkipWhileDisplayOnEnabled

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isSkipWhileDisplayOnEnabled = isChecked
      }
    }

    licenses.setOnClickListener {
      startActivity(Intent(this@MainActivity, OssLicensesMenuActivity::class.java))
    }

    /* Device Pair-Unpair Button */
    fabPair.run {
      if (userPreferences.notifierGcmToken.isBlank()) {
        text = getString(R.string.pair_with_device)
        setIconResource(R.drawable.ic_handshake_24)
      } else {
        text = getString(R.string.unpair)
        setIconResource(R.drawable.ic_unpair_24)
      }

      setOnClickListener {
        if (userPreferences.notifierGcmToken.isBlank()) {
          val dialogContentView = DialogPairBinding.inflate(layoutInflater)

          dialogContentView.receiverLink.setOnClickListener {
            val shareIntent = Intent().apply {
              action = Intent.ACTION_SEND
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, "https://${getString(R.string.receiver_domain)}")
            }

            startActivity(Intent.createChooser(shareIntent, null))
          }

          MaterialAlertDialogBuilder(context, R.style.M3AlertDialog_Centered_FullWidthButtons)
            .setIcon(R.drawable.ic_handshake_24)
            .setTitle(getString(R.string.pair_dialog_title))
            .setView(dialogContentView.root)
            .setNeutralButton(getString(R.string.pair_dialog_paste_button)) { _, _ ->
              val clipboard = (getSystemService(Context.CLIPBOARD_SERVICE)) as? ClipboardManager
              val clipboardText: CharSequence =
                clipboard?.primaryClip?.getItemAt(0)?.text ?: ""

              saveToken(clipboardText.toString())
            }
            .setPositiveButton(getString(R.string.pair_dialog_scan_button)) { _, _ ->
              scanQrCode.launch(qrConfig)
            }
            .show()
        } else {
          MaterialAlertDialogBuilder(context)
            .setMessage(getString(R.string.unpair_dialog_content))
            .setPositiveButton(getString(R.string.unpair_dialog_button_unpair)) { _, _ ->
              userPreferences.notifierGcmToken = ""
            }
            .setNegativeButton(getString(R.string.unpair_dialog_button_send)) { _, _ ->
              pushServerClient.postNotification(
                userPreferences.notifierGcmToken,
                "Successfully Paired",
                "It is working correctly!"
              )
            }
            .show()
        }
      }
    }
  }

  // Disables notification service if token is empty
  // 1. While setting up the UI
  // 2. After successful pairing & unpairing
  // 3. Both battery reached and low battery checkbox is unchecked
  private fun refreshNotificationServiceState() {
    val switchMaterial: MaterialSwitch = mainActivityBinding.switchNotificationService

    userPreferences.run {
      val isNotifyWhenEnabled = isLevelReachedNotificationEnabled || isLowBatteryNotificationEnabled
      val shouldBeEnabled = isNotifyWhenEnabled && notifierGcmToken.isNotEmpty()
      val shouldBeChecked = shouldBeEnabled && isMonitoringServiceEnabled

      switchMaterial.isEnabled = shouldBeEnabled
      switchMaterial.isChecked = shouldBeChecked

      if (shouldBeChecked) BroadcastReceiverRegistererService.start(this@MainActivity)
      else BroadcastReceiverRegistererService.stop(this@MainActivity)
    }
  }

  override fun onResume() {
    super.onResume()

    userPreferences.run {
      startObservingChanges { _: SharedPreferences, key: String? ->
        if (key == null) return@startObservingChanges

        logV { "$key has been updated." }

        when (key) {
          UserPreferences.MONITORING_SERVICE_TOGGLE,
          UserPreferences.LEVEL_REACHED_NOTIFICATION_TOGGLE,
          UserPreferences.LOW_BATTERY_NOTIFICATION_TOGGLE,
          -> refreshNotificationServiceState()

          UserPreferences.NOTIFIER_GCM_TOKEN -> refreshBecauseTokenChanged()
        }
      }
    }

    logV { "onResume: Started observing for sharedPreferences changes" }
  }

  private fun refreshBecauseTokenChanged() {
    refreshNotificationServiceState()

    mainActivityBinding.fabPair.run {
      if (userPreferences.notifierGcmToken.isBlank()) {
        text = getString(R.string.pair_with_device)
        setIconResource(R.drawable.ic_handshake_24)
      } else {
        text = getString(R.string.unpair)
        setIconResource(R.drawable.ic_unpair_24)
      }
    }

    if (userPreferences.notifierGcmToken.isEmpty()) {
      Snackbar.make(mainActivityBinding.root, R.string.unpaired, Snackbar.LENGTH_SHORT)
        .setAnchorView(mainActivityBinding.fabPair)
        .show()
    } else {
      Snackbar.make(mainActivityBinding.root, R.string.successful_pairing, Snackbar.LENGTH_SHORT)
        .setAnchorView(mainActivityBinding.fabPair)
        .show()
    }
  }

  private fun handleScannedResult(result: QRResult) {
    when (result) {
      QRResult.QRMissingPermission -> logV { "Missing permission" }

      QRResult.QRUserCanceled -> logV { "User canceled" }

      is QRResult.QRError -> logE {
        result.exception.localizedMessage ?: "Error"
      }

      is QRResult.QRSuccess -> {
        saveToken(result.content.rawValue)
      }
    }
  }

  private fun saveToken(token: String) {
    logV { "GCM TOKEN: $token" }

    if (token.isBlank()) {
      Snackbar.make(mainActivityBinding.root, R.string.token_invalid, Snackbar.LENGTH_LONG)
        .setAnchorView(mainActivityBinding.fabPair)
        .show()

      return
    }

    userPreferences.notifierGcmToken = token
    userPreferences.isMonitoringServiceEnabled = true

    // This function is called before onResume has a chance to start observing the sharedPref changes.
    // Manual refresh is required here to update the screen state.
    refreshBecauseTokenChanged()

    // Send a test push notification
    pushServerClient.postNotification(token, "Successfully Paired", "It is working correctly!")
  }

  override fun onPause() {
    super.onPause()

    userPreferences.stopObservingChanges()

    logV { "onPause: Stopped observing for sharedPreferences changes" }
  }
}