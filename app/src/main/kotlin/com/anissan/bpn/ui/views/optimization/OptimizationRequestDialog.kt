package com.anissan.bpn.ui.views.optimization

import com.anissan.bpn.R
import com.anissan.bpn.ui.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun MainActivity.showOptimizationRequestDialog() {
  MaterialAlertDialogBuilder(this, R.style.CenteredDialog)
    .setIcon(R.drawable.ic_heart)
    .setTitle(getString(R.string.optimization_exemption_title))
    .setMessage(R.string.optimization_exemption_dialog)
    .setPositiveButton(R.string.optimization_exemption_button) { _, _ ->
      OptimizationRemoverSheet.show(supportFragmentManager)
    }
    .show()
}
