package com.anissan.battarang.ui.views.pairing

import androidx.activity.result.ActivityResultLauncher
import com.anissan.battarang.R
import com.anissan.battarang.ui.MainActivity
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig

private lateinit var qrCodeScanner: ActivityResultLauncher<ScannerConfig>
private lateinit var onQrSuccess: (result: String) -> Unit

fun MainActivity.registerQrScanner() {
  qrCodeScanner =
    registerForActivityResult(ScanCustomCode()) { scanResult: QRResult ->
      when (scanResult) {
        QRResult.QRUserCanceled -> logV { "User went back without scanning a QR code" }
        QRResult.QRMissingPermission -> showSnackbar(R.string.camera_permission_missing)

        is QRResult.QRError -> {
          logE(scanResult.exception)
          showSnackbar(R.string.camera_unavailable)
        }

        is QRResult.QRSuccess -> onQrSuccess(scanResult.content.rawValue!!)
      }
    }
}

fun launchQrScanner(onSuccess: (result: String) -> Unit) {
  val qrScannerConfig = ScannerConfig.build {
    setBarcodeFormats(listOf(BarcodeFormat.FORMAT_QR_CODE))
    setOverlayDrawableRes(R.drawable.ic_qr_code)
    setOverlayStringRes(R.string.quickie_overlay_string)
    setHapticSuccessFeedback(true)
  }

  onQrSuccess = onSuccess

  if (::qrCodeScanner.isInitialized) return qrCodeScanner.launch(qrScannerConfig)
  else throw Exception("Make sure to `registerQrScanner` on onCreate before launching the scanner.")
}
