package com.anissan.battarang.ui.views

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.CompoundButton
import com.anissan.battarang.R
import com.anissan.battarang.ui.MainActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider

fun MainActivity.setupMaxBatteryLevelCheckbox() {
  binding.cardConfig.setCardBackgroundColor(dynamicSurfaceColor)

  binding.checkBoxMaxBatteryLevel.run {
    isChecked = localKvStore.isMaxLevelNotificationEnabled

    /**
     * Setting up a bold and text color Span on the "~%" portion of the checkbox text, so that,
     * during the slider value update any number inserted in the middle is going to retain the formatting
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

    binding.checkBoxMaxBatteryLevel.text = maxLevelSpannableStringBuilder.insert(
      tildeEnd,
      localKvStore.maxChargingLevelPercentage.toString(),
    )

    bindMaxLevelSlider(maxLevelSpannableStringBuilder, tildeEnd)

    bindClicksFrom(binding.cardBatteryLevelReached)

    setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
      localKvStore.isMaxLevelNotificationEnabled = isChecked
    }
  }
}

private fun MainActivity.bindMaxLevelSlider(
  maxLevelSpannableStringBuilder: SpannableStringBuilder,
  tildeEnd: Int,
) {
  binding.batteryLevelSlider.run {
    val savedMaxLevel: Int = localKvStore.maxChargingLevelPercentage
    value = savedMaxLevel.toFloat()

    // Keeping this length in the memory to determine how many digits it
    // should be replaced on a slider value update
    var currentMaxLevel: Int = "$savedMaxLevel".length

    addOnChangeListener { _: Slider, updatedValue: Float, _: Boolean ->
      val updatedLevelValue: Int = updatedValue.toInt()
      localKvStore.maxChargingLevelPercentage = updatedLevelValue

      val updatedLevelValueString = "$updatedLevelValue"

      binding.checkBoxMaxBatteryLevel.text = maxLevelSpannableStringBuilder.replace(
        tildeEnd, // ~|85%
        tildeEnd + currentMaxLevel, // ~85|%
        updatedLevelValueString,
      )

      currentMaxLevel = updatedLevelValueString.length
    }
  }
}
