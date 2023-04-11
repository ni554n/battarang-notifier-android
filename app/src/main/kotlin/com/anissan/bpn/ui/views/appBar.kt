package com.anissan.bpn.ui.views

import com.anissan.bpn.R
import com.anissan.bpn.ui.MainActivity
import com.anissan.bpn.ui.views.optimization.OptimizationRemoverSheet
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
