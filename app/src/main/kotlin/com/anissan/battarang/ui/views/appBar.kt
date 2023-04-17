package com.anissan.battarang.ui.views

import com.anissan.battarang.R
import com.anissan.battarang.ui.MainActivity
import com.anissan.battarang.ui.views.optimization.OptimizationRemoverSheet
import com.google.android.material.elevation.SurfaceColors
import dev.chrisbanes.insetter.applyInsetter

fun MainActivity.setupAppBar() {
  binding.collapsingToolbarLayout.applyInsetter {
    type(navigationBars = true, statusBars = true) {
      margin(horizontal = true)
      padding(vertical = true)
    }
  }

  /* Calculating the marginStart value for the Expanded Title to align it with the centered cards. */

  val screenWidth = resources.displayMetrics.widthPixels
  val cardMaxWidth = resources.getDimension(R.dimen.card_max_width).toInt()
  val expandedTitleMargin = binding.collapsingToolbarLayout.expandedTitleMarginStart

  if (screenWidth > cardMaxWidth) {
    binding.collapsingToolbarLayout.expandedTitleMarginStart =
      (screenWidth - cardMaxWidth + expandedTitleMargin) / 2
  }

  binding.collapsingToolbarLayout.setContentScrimColor(SurfaceColors.SURFACE_3.getColor(this))

  binding.materialToolbar.setOnMenuItemClickListener { menuItem ->
    when (menuItem.itemId) {
      R.id.remove_battery_restriction -> {
        OptimizationRemoverSheet.show(supportFragmentManager)
        true
      }

      else -> false
    }
  }
}
