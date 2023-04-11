package com.anissan.bpn.ui.views

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.CompoundButton
import com.anissan.bpn.R
import com.anissan.bpn.ui.MainActivity
import com.google.android.material.color.MaterialColors

fun MainActivity.setupLowBatteryLevelCheckbox() {
  binding.checkBoxBatteryLevelLow.run {
    isChecked = localKvStore.isLowBatteryNotificationEnabled

    // Formatting the "LOW" portion as bold with color
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

    bindClicksFrom(binding.cardBatteryLevelLow)

    setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
      localKvStore.isLowBatteryNotificationEnabled = isChecked
    }
  }

}
