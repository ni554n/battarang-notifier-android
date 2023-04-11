package com.anissan.bpn.ui.views

import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.anissan.bpn.data.LocalKvStore
import com.anissan.bpn.ui.MainActivity

fun MainActivity.setupDeviceNameInput() {
  binding.textInputLayoutDeviceName.apply {
    // Hiding the Save icon initially. It'll be shown on EditText focus later.
    isEndIconVisible = false

    setEndIconOnClickListener { endIconView: View ->
      localKvStore.deviceName =
        binding.editTextDeviceName.text.toString().ifBlank { LocalKvStore.DEFAULT_DEVICE_NAME }

      // Let the ripple animation finish before hiding the save icon.
      handler.postDelayed({ isEndIconVisible = false }, 450)

      // Clear the EditText focus, otherwise blinking cursor won't hide.
      binding.editTextDeviceName.clearFocus()

      // Hide the keyboard.
      WindowCompat.getInsetsController(window, endIconView).hide(WindowInsetsCompat.Type.ime())
    }
  }

  binding.editTextDeviceName.apply {
    setText(localKvStore.deviceName)

    setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
      when {
        hasFocus -> binding.textInputLayoutDeviceName.isEndIconVisible = true
        text.toString().isBlank() -> setText(localKvStore.deviceName)
      }
    }
  }
}
