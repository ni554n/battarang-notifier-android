package com.anissan.battarang.ui.views.permission

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.anissan.battarang.R
import com.anissan.battarang.ui.MainActivity
import com.anissan.battarang.ui.views.pairing.showPairingDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun MainActivity.registerForRequestingPermission(): ActivityResultLauncher<String> {
  requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      showPairingDialog()
    } else {
      requestNotificationPermission(requestPermissionLauncher, true)
    }
  }

  return requestPermissionLauncher
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun MainActivity.requestNotificationPermission(
  requestPermissionLauncher: ActivityResultLauncher<String>,
  showSettingsAction: Boolean = false,
) {
  when {
    ActivityCompat.shouldShowRequestPermissionRationale(
      this, Manifest.permission.POST_NOTIFICATIONS
    ) -> {
      MaterialAlertDialogBuilder(this)
        .setCancelable(false)
        .setTitle(R.string.notification_permission_rational_dialog_title)
        .setMessage(R.string.notification_permission_rational_dialog_description)
        .setPositiveButton("Allow") { _, _ ->
          requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }.show()
    }

    showSettingsAction -> {
      MaterialAlertDialogBuilder(this)
        .setCancelable(false)
        .setTitle(R.string.notification_permission_rational_dialog_title)
        .setMessage(R.string.notification_permission_rational_dialog_description)
        .setPositiveButton("Settings") { _, _ ->
          startActivity(Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
          })
        }.show()
    }

    else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
  }
}
