package com.anissan.battarang.ui.views

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.CompoundButton
import com.anissan.battarang.R
import com.anissan.battarang.ui.MainActivity
import com.google.android.material.color.MaterialColors

fun MainActivity.setupSkipIfDisplayOnToggleCheckbox() {
  binding.skipWhileDisplayOnCheckbox.run {
    isChecked = localKvStore.isSkipWhileDisplayOnEnabled

    // Formatting the "ON" portion bold with color
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
        com.google.android.material.R.attr.colorPrimary,
      )

      setSpan(
        ForegroundColorSpan(colorSecondary),
        oStart,
        nEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
      )
    }

    bindClicksFrom(binding.skipWhileDisplayOnCard)

    setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
      localKvStore.isSkipWhileDisplayOnEnabled = isChecked
    }
  }
}
