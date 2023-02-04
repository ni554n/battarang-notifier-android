package com.anissan.bpn

import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.anissan.bpn.databinding.ActivityMainBinding
import com.anissan.bpn.databinding.DialogPairBinding
import com.anissan.bpn.event.BroadcastReceiverRegistererService
import com.anissan.bpn.network.PushServerClient
import com.anissan.bpn.storage.UserPreferences
import com.anissan.bpn.ui.about.AboutSheet
import com.anissan.bpn.ui.optimizationremover.OptimizationRemoverSheet
import com.anissan.bpn.utils.logE
import com.anissan.bpn.utils.logV
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
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

  private lateinit var mainActivityBinding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Requesting this layout to be laid out edge-to-edge.
    WindowCompat.setDecorFitsSystemWindows(window, false)

    mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)

    mainActivityBinding.run {
      setContentView(root)

      insetViewPositions()

      setupAppBar()
      setupNotificationServiceToggleSwitch()

      setupDeviceNameInputField()

      setupMaxBatteryLevelToggleCheckbox()
      setupMaxBatteryLevelSlider()

      setupLowBatteryToggleCheckbox()
      setupSkipIfDisplayOnToggleCheckbox()

      setupAbout()

      setupDevicePairingFab()
    }
  }

  override fun onResume() {
    super.onResume()

    userPreferences.startObservingChanges { _: SharedPreferences, key: String? ->
      if (key == null) return@startObservingChanges

      logV { "$key has been updated by user" }

      when (key) {
        UserPreferences.MONITORING_SERVICE_TOGGLE_KEY,
        UserPreferences.MAX_LEVEL_NOTIFICATION_TOGGLE_KEY,
        UserPreferences.LOW_BATTERY_NOTIFICATION_TOGGLE_KEY,
        -> refreshMonitoringServiceState()

        UserPreferences.NOTIFIER_GCM_TOKEN_KEY -> refreshAfterPairingUnpairing()
      }
    }

    logV { "onResume: Started listening for sharedPreferences changes ..." }
  }

  override fun onPause() {
    super.onPause()

    userPreferences.stopObservingChanges()
    logV { "onPause: Stopped observing for sharedPreferences changes." }
  }

  /**
   * Provide proper margin and padding to prevent views from overlapping with the system elements
   * such as status bar or navigation bar.
   * */
  private fun ActivityMainBinding.insetViewPositions() {
    collapsingToolbarLayout.applyInsetter {
      type(navigationBars = true) {
        margin(horizontal = true)
      }
    }

    cardNotificationService.applyInsetter {
      type(navigationBars = true) {
        padding(horizontal = true)
      }
    }

    nestedScrollView.applyInsetter {
      type(navigationBars = true) {
        margin(horizontal = true)
      }
    }

    contentLayout.applyInsetter {
      type(navigationBars = true) {
        margin(vertical = true)
      }
    }

    fabPair.applyInsetter {
      type(navigationBars = true) {
        margin(horizontal = true, vertical = true)
      }
    }
  }

  private fun ActivityMainBinding.setupAppBar() {
    appBarLayout.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this@MainActivity))
  }

  private fun ActivityMainBinding.setupNotificationServiceToggleSwitch() {
    refreshMonitoringServiceState()

    cardNotificationService.setOnClickListener {
      if (paired) {
        switchNotificationService.performClick()
      } else {
        // Giving an option to open the Pairing Dialog even though this card looks disabled in this state.
        // Not going to enable the Switch itself because it turns on instantly upon clicked.
        buildPairingDialog().show()
      }
    }

    switchNotificationService.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
      userPreferences.isMonitoringServiceEnabled = isChecked
    }
  }

  private fun ActivityMainBinding.setupBatteryExemptionGuide() {
    cardBatteryExemption.setOnClickListener {
      OptimizationRemoverSheet.show(supportFragmentManager)

      it.visibility = View.GONE
    }
  }

  private fun ActivityMainBinding.setupDeviceNameInputField() {
    textInputLayoutDeviceName.run {
      // Hide the Save icon initially. It'll be shown on EditText focus later.
      isEndIconVisible = false

      setEndIconOnClickListener { endIconView: View ->
        userPreferences.deviceName =
          editTextDeviceName.text.toString().ifBlank { UserPreferences.DEFAULT_DEVICE_NAME }

        // Let the ripple animation finish before hiding the save icon.
        handler.postDelayed({ isEndIconVisible = false }, 450)

        // Clear the EditText focus, otherwise blinking cursor won't hide.
        editTextDeviceName.clearFocus()

        // Hide the keyboard.
        WindowCompat.getInsetsController(window, endIconView).hide(WindowInsetsCompat.Type.ime())
      }
    }

    editTextDeviceName.run {
      setText(userPreferences.deviceName)

      setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
        if (hasFocus) textInputLayoutDeviceName.isEndIconVisible = true
        else if (text.toString().isBlank()) setText(userPreferences.deviceName)
      }
    }
  }

  private fun ActivityMainBinding.setupMaxBatteryLevelToggleCheckbox() {
    checkBoxMaxBatteryLevel.run {
      isChecked = userPreferences.isMaxLevelNotificationEnabled

      bindClicksFrom(cardBatteryLevelReached)

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isMaxLevelNotificationEnabled = isChecked
      }
    }
  }

  /**
   * Checkbox have a ripple animation set only on its checkmark icon, not the text beside it.
   * Setting a custom ripple on the whole checkbox doesn't work if margin is required. The ripple will fall short.
   * The only workaround I found is to use a full bleed wrapper card (which already has a nice ripple effect built-in)
   * and disabling the checkbox so that the card behind gets the click. But then I have to programmatically
   * pass the card clicks to the checkbox.
   */
  private fun MaterialCheckBox.bindClicksFrom(card: MaterialCardView) {
    card.setOnClickListener { performClick() }
  }

  private fun ActivityMainBinding.setupMaxBatteryLevelSlider() {
    batteryLevelSlider.run {
      val batteryLevel: Int = userPreferences.maxChargingLevelPercentage
      value = batteryLevel.toFloat()

      // String template for formatting battery percentage ("~85%") portion bold.
      val batteryLevelReachesStringTemplate = getString(R.string.battery_level_reaches)

      checkBoxMaxBatteryLevel.text =
        HtmlCompat.fromHtml(
          batteryLevelReachesStringTemplate.format(batteryLevel),
          HtmlCompat.FROM_HTML_MODE_COMPACT,
        )

      addOnChangeListener { _: Slider, value: Float, _: Boolean ->
        val levelValue: Int = value.toInt()

        userPreferences.maxChargingLevelPercentage = levelValue

        checkBoxMaxBatteryLevel.text =
          HtmlCompat.fromHtml(
            batteryLevelReachesStringTemplate.format(levelValue),
            HtmlCompat.FROM_HTML_MODE_COMPACT,
          )
      }
    }
  }

  private fun ActivityMainBinding.setupLowBatteryToggleCheckbox() {
    checkBoxBatteryLevelLow.run {
      isChecked = userPreferences.isLowBatteryNotificationEnabled

      bindClicksFrom(cardBatteryLevelLow)

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isLowBatteryNotificationEnabled = isChecked
      }
    }
  }

  private fun ActivityMainBinding.setupSkipIfDisplayOnToggleCheckbox() {
    checkBoxSkipWhileDisplayOn.run {
      isChecked = userPreferences.isSkipWhileDisplayOnEnabled

      bindClicksFrom(cardSkipWhileDisplayOn)

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isSkipWhileDisplayOnEnabled = isChecked
      }
    }
  }

  /**
   * Checkbox have a ripple animation set only on its checkmark icon, not the text beside it.
   * Setting a custom ripple on the whole checkbox doesn't work if margin is applied and the ripple won't reach the edges.
   * The only workaround I found is to use a full bleed wrapper card (which already has a nice ripple effect built-in)
   * and disabling the checkbox so that the card behind gets the click. But then I have to programmatically
   * pass the card clicks to the checkbox.
   */
  private fun MaterialCheckBox.bindClicksFrom(card: MaterialCardView) {
    card.setOnClickListener { performClick() }
  }

  private fun ActivityMainBinding.setupRemoveRestrictionsButton() {
    removeRestrictionsButton.setOnClickListener {
      OptimizationRemoverSheet.show(supportFragmentManager)
    }
  }

  private fun ActivityMainBinding.setupAbout() {
    licenses.setOnClickListener {
      startActivity(Intent(this@MainActivity, OssLicensesMenuActivity::class.java))
    }
  }

  private fun ActivityMainBinding.setupDevicePairingFab() {
    refreshFabUi()

    fabPair.setOnClickListener {
      if (userPreferences.notifierGcmToken.isBlank()) {
        buildPairingDialog().show()
      } else {
        buildUnpairingDialog().show()
      }
    }

    // Hide the FAB when keyboard shows up, otherwise it can overlap with the input field on smaller device.
    ViewCompat.setOnApplyWindowInsetsListener(root) { _: View, insets: WindowInsetsCompat ->
      val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
      if (isKeyboardVisible) fabPair.hide() else fabPair.show()

      insets
    }
  }

  private fun refreshFabUi() {
    mainActivityBinding.fabPair.run {
      if (userPreferences.notifierGcmToken.isBlank()) {
        text = getString(R.string.pair_with_device)
        setIconResource(R.drawable.ic_handshake_24)
      } else {
        text = getString(R.string.unpair)
        setIconResource(R.drawable.ic_unpair_24)
      }
    }
  }

  private val qrCodeScanner: ActivityResultLauncher<ScannerConfig> =
    registerForActivityResult(ScanCustomCode()) { scanResult: QRResult ->
      when (scanResult) {
        QRResult.QRMissingPermission -> logV { "Missing permission" }

        QRResult.QRUserCanceled -> logV { "User canceled" }

        is QRResult.QRError -> logE { scanResult.exception.localizedMessage ?: "Error" }

        is QRResult.QRSuccess -> saveToken(scanResult.content.rawValue)
      }
    }

  private val qrScannerConfig = ScannerConfig.build {
    setBarcodeFormats(listOf(BarcodeFormat.FORMAT_QR_CODE))
    setOverlayDrawableRes(R.drawable.ic_qr_code_48)
    setOverlayStringRes(R.string.quickie_overlay_string)
    setHapticSuccessFeedback(true)
  }

  private fun buildPairingDialog(): MaterialAlertDialogBuilder {
    val dialogContentView = DialogPairBinding.inflate(layoutInflater)

    dialogContentView.receiverLink.setOnClickListener {
      val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "https://${getString(R.string.receiver_domain)}")
      }

      startActivity(Intent.createChooser(shareIntent, null))
    }

    return MaterialAlertDialogBuilder(this, R.style.M3AlertDialog_Centered_FullWidthButtons)
      .setIcon(R.drawable.ic_handshake_24)
      .setTitle(getString(R.string.pair_dialog_title))
      .setView(dialogContentView.root)
      .setNeutralButton(getString(R.string.pair_dialog_paste_button)) { _, _ ->
        val clipboard = (getSystemService(CLIPBOARD_SERVICE)) as? ClipboardManager
        val clipboardText: CharSequence =
          clipboard?.primaryClip?.getItemAt(0)?.text ?: ""

        saveToken(clipboardText.toString())
      }
      .setPositiveButton(getString(R.string.pair_dialog_scan_button)) { _, _ ->
        qrCodeScanner.launch(qrScannerConfig)
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
    refreshAfterPairingUnpairing()

    // Send a test push notification
    pushServerClient.postNotification(token, "Successfully Paired", "It is working correctly!")
  }

  private fun buildUnpairingDialog(): MaterialAlertDialogBuilder {
    return MaterialAlertDialogBuilder(this)
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
  }

  private fun refreshMonitoringServiceState() {
    val switchNotificationService: MaterialSwitch = mainActivityBinding.switchNotificationService

    userPreferences.run {
      // At least one of the "NOTIFY WHEN" option needs to be checked for the switch to be enabled.
      val isNotifyWhenEnabled = isMaxLevelNotificationEnabled || isLowBatteryNotificationEnabled

      // Enable / Disable means Activation (Clickable) / Deactivation (Gray out) in Views.
      switchNotificationService.isEnabled = isNotifyWhenEnabled && notifierGcmToken.isNotBlank()

      switchNotificationService.isChecked =
        switchNotificationService.isEnabled && isMonitoringServiceEnabled

      if (switchNotificationService.isChecked) BroadcastReceiverRegistererService.start(this@MainActivity)
      else BroadcastReceiverRegistererService.stop(this@MainActivity)
    }
  }

  private fun refreshAfterPairingUnpairing() {
    refreshMonitoringServiceState()
    refreshFabUi()

    if (userPreferences.notifierGcmToken.isBlank()) {
      Snackbar.make(mainActivityBinding.root, R.string.unpaired, Snackbar.LENGTH_SHORT)
        .setAnchorView(mainActivityBinding.fabPair)
        .show()
    } else {
      Snackbar.make(mainActivityBinding.root, R.string.successful_pairing, Snackbar.LENGTH_SHORT)
        .setAnchorView(mainActivityBinding.fabPair)
        .show()
    }
  }
}
