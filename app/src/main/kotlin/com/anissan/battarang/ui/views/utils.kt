package com.anissan.battarang.ui.views

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.elevation.SurfaceColors

/**
 * Checkboxes has a ripple animation set only on its checkmark icon area, not the text beside it.
 * Setting a custom ripple on the whole checkbox doesn't look good if there's a margin applied because
 * the ripple won't reach the edges.
 *
 * The only workaround I've found is to use a full bleed wrapper card (which already has a nice
 * ripple effect built-in) and disabling the checkbox so that
 * the card receives the clicks which it then forwards it to the checkbox.
 */
fun MaterialCheckBox.bindClicksFrom(card: MaterialCardView) {
  card.setOnClickListener { performClick() }
}

val AppCompatActivity.isDarkModeEnabled: Boolean
  get() {
    val darkModeFlag = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
  }

val AppCompatActivity.dynamicSurfaceColor: Int
  get() = SurfaceColors.getColorForElevation(this, if (isDarkModeEnabled) 4f else 8f)

val Context.defaultDeviceName: String
  get() {
    val deviceName: String? = if (Build.VERSION.SDK_INT >= 25) {
      Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
    } else {
      Settings.Secure.getString(contentResolver, "bluetooth_name")
    }

    return (deviceName ?: Build.MODEL).trim().ifBlank { "Unknown" }
  }
