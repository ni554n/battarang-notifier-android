package com.anissan.battarang.ui.views

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.anissan.battarang.ui.MainActivity
import com.anissan.battarang.ui.views.pairing.showPairingDialog
import dev.chrisbanes.insetter.applyInsetter

fun MainActivity.setupPairingFab() {
  binding.fabPair.run {
    applyInsetter {
      type(navigationBars = true) {
        margin(horizontal = true, vertical = true)
      }
    }

    if (paired) hide() else show()

    setOnClickListener { showPairingDialog() }
  }

  binding.nestedScrollView.setOnScrollChangeListener(
    NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
      binding.fabPair.run {
        if (scrollY > oldScrollY) {
          if (isExtended) shrink()
        } else if (isExtended.not()) extend()
      }
    }
  )

  // Hiding the FAB when keyboard shows up, otherwise it can overlap with the input field on a smaller device.
  ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _: View, insets: WindowInsetsCompat ->
    if (!paired) {
      val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
      if (isKeyboardVisible) binding.fabPair.hide() else binding.fabPair.show()
    }

    insets
  }
}
