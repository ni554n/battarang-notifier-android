package com.anissan.battarang.ui.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.anissan.battarang.ui.MainActivity
import com.anissan.battarang.ui.views.pairing.showPairingDialog
import com.anissan.battarang.ui.views.permission.registerForRequestingPermission
import com.anissan.battarang.ui.views.permission.requestNotificationPermission
import dev.chrisbanes.insetter.applyInsetter

fun MainActivity.setupPairReceiverFab() {
  binding.pairReceiverFab.run {
    applyInsetter {
      type(navigationBars = true) {
        margin(horizontal = true, vertical = true)
      }
    }

    if (paired) hide() else show()

    if (Build.VERSION.SDK_INT >= 33 && (ContextCompat.checkSelfPermission(
        this@setupPairReceiverFab,
        Manifest.permission.POST_NOTIFICATIONS
      ) != PackageManager.PERMISSION_GRANTED)
    ) {
      val requestPermissionLauncher = registerForRequestingPermission()
      setOnClickListener { requestNotificationPermission(requestPermissionLauncher) }
    } else setOnClickListener { showPairingDialog() }
  }

  binding.nestedScrollView.setOnScrollChangeListener(
    NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
      binding.pairReceiverFab.run {
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
      if (isKeyboardVisible) binding.pairReceiverFab.hide() else binding.pairReceiverFab.show()
    }

    insets
  }
}
