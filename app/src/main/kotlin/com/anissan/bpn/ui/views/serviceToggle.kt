package com.anissan.bpn.ui.views

import android.widget.CompoundButton
import com.anissan.bpn.ui.MainActivity
import com.anissan.bpn.ui.views.pairing.showPairingDialog
import dev.chrisbanes.insetter.applyInsetter

fun MainActivity.setupServiceToggle() {
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

  binding.cardNotificationService.apply {
    applyInsetter {
      type(navigationBars = true) {
        padding(horizontal = true)
      }
    }

    setOnClickListener {
      if (paired) {
        binding.switchNotificationService.performClick()
      } else {
        // Giving an option to open the Pairing Dialog even though this card looks disabled in this state.
        // Not going to enable the Switch itself because it turns on instantly when clicked.
        showPairingDialog()
      }
    }
  }

  binding.switchNotificationService.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
    localKvStore.isMonitoringServiceEnabled = isChecked
  }

  refreshServiceState()
}
