package com.anissan.battarang.ui.views

import android.widget.CompoundButton
import com.anissan.battarang.ui.MainActivity
import dev.chrisbanes.insetter.applyInsetter

fun MainActivity.setupNotifierServiceToggle() {
  binding.nestedScrollView.applyInsetter {
    type(navigationBars = true) {
      margin(horizontal = true)
    }
  }

  binding.contentLayout.applyInsetter {
    type(navigationBars = true) {
      margin(vertical = true)
    }
  }

  binding.notifierServiceCard.apply {
    applyInsetter {
      type(navigationBars = true) {
        padding(horizontal = true)
      }
    }

    setOnClickListener {
      if (paired) {
        binding.notifierServiceSwitch.performClick()
      }
    }
  }

  binding.notifierServiceSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
    localKvStore.isMonitoringServiceEnabled = isChecked
  }

  refreshServiceState()
}
