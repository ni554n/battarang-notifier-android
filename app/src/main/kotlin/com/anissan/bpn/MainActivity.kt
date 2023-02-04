package com.anissan.bpn

import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

      /**
       * Setting up a bold and text color Span on the "~%" portion of the checkbox text, so that,
       * during the slider value update any number inserted in the middle is going to retain the formatting.
       */

      val maxLevelSpannableStringBuilder =
        SpannableStringBuilder(getString(R.string.battery_level_reaches_template))

      // These span positions are like text input cursors => |
      // Insert at 1 means inserting before b => "a|bc", at 2 means inserting after b => "ab|c"
      val percentEnd = maxLevelSpannableStringBuilder.length // ... ~%|
      val tildeStart = percentEnd - 2 // ... |~%

      val colorSecondary =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary)

      maxLevelSpannableStringBuilder.setSpan(
        StyleSpan(Typeface.BOLD),
        tildeStart,
        percentEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
      )

      maxLevelSpannableStringBuilder.setSpan(
        ForegroundColorSpan(colorSecondary),
        tildeStart,
        percentEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
      )

      val tildeEnd = tildeStart + 1 // ~|%

      checkBoxMaxBatteryLevel.text = maxLevelSpannableStringBuilder.insert(
        tildeEnd,
        userPreferences.maxChargingLevelPercentage.toString(),
      )

      bindMaxLevelSlider(maxLevelSpannableStringBuilder, tildeEnd)

      bindClicksFrom(cardBatteryLevelReached)

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isMaxLevelNotificationEnabled = isChecked
      }
    }
  }

  private fun ActivityMainBinding.bindMaxLevelSlider(
    maxLevelSpannableStringBuilder: SpannableStringBuilder,
    tildeEnd: Int,
  ) {
    batteryLevelSlider.run {
      val savedMaxLevel: Int = userPreferences.maxChargingLevelPercentage
      value = savedMaxLevel.toFloat()

      // Keeping this length in memory so that it can be determined how many digits
      // should be replaced on slider value update.
      var currentMaxLevel: Int = "$savedMaxLevel".length

      addOnChangeListener { _: Slider, updatedValue: Float, _: Boolean ->
        val updatedLevelValue: Int = updatedValue.toInt()
        userPreferences.maxChargingLevelPercentage = updatedLevelValue

        val updatedLevelValueString = "$updatedLevelValue"

        checkBoxMaxBatteryLevel.text = maxLevelSpannableStringBuilder.replace(
          tildeEnd, // ~|85%
          tildeEnd + currentMaxLevel, // ~85|%
          updatedLevelValueString,
        )

        currentMaxLevel = updatedLevelValueString.length
      }
    }
  }

  private fun ActivityMainBinding.setupLowBatteryToggleCheckbox() {
    checkBoxBatteryLevelLow.run {
      isChecked = userPreferences.isLowBatteryNotificationEnabled

      // Formatting the "LOW" portion bold with color.
      text = SpannableString(getString(R.string.battery_is_low_template)).apply {
        val wEnd = length // Battery is LOW|
        val lStart = wEnd - 3 // Battery is |LOW

        setSpan(
          StyleSpan(Typeface.BOLD),
          lStart,
          wEnd,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        val colorSecondary = MaterialColors.getColor(
          this@run,
          com.google.android.material.R.attr.colorSecondary,
        )

        setSpan(
          ForegroundColorSpan(colorSecondary),
          lStart,
          wEnd,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }

      bindClicksFrom(cardBatteryLevelLow)

      setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
        userPreferences.isLowBatteryNotificationEnabled = isChecked
      }
    }
  }

  private fun ActivityMainBinding.setupSkipIfDisplayOnToggleCheckbox() {
    checkBoxSkipWhileDisplayOn.run {
      isChecked = userPreferences.isSkipWhileDisplayOnEnabled

      // Formatting the "ON" portion bold with color.
      text = SpannableString(getString(R.string.skip_if_display_on_template)).apply {
        val nEnd = length // Skip while display is ON|
        val oStart = nEnd - 2 // Skip while display is |ON

        setSpan(
          StyleSpan(Typeface.BOLD),
          oStart,
          nEnd,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        val colorSecondary = MaterialColors.getColor(
          this@run,
          com.google.android.material.R.attr.colorSecondary,
        )

        setSpan(
          ForegroundColorSpan(colorSecondary),
          oStart,
          nEnd,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }

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
