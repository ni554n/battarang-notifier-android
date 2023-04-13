package com.anissan.battarang.ui.views.optimization

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.anissan.battarang.R
import com.anissan.battarang.ui.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private lateinit var optimizationRequestDialog: AlertDialog

fun MainActivity.showOptimizationRequestDialog() {
  optimizationRequestDialog = MaterialAlertDialogBuilder(this, R.style.CenteredDialog)
    .setIcon(R.drawable.ic_heart)
    .setTitle(getString(R.string.optimization_exemption_title))
    .setMessage(R.string.optimization_exemption_dialog)
    .setPositiveButton(R.string.optimization_exemption_button) { _, _ ->
      OptimizationRemoverSheet.show(supportFragmentManager)
    }
    .show()
}

private const val OPTIMIZATION_REQUEST_DIALOG = "optimization_request_dialog"

fun saveOptimizationRequestDialogState(outState: Bundle) {
  outState.putBoolean(
    OPTIMIZATION_REQUEST_DIALOG,
    if (::optimizationRequestDialog.isInitialized) optimizationRequestDialog.isShowing else false,
  )
}

fun MainActivity.restoreOptimizationRequestDialogState(savedInstanceState: Bundle) {
  if (savedInstanceState.getBoolean(OPTIMIZATION_REQUEST_DIALOG)) showOptimizationRequestDialog()
}
