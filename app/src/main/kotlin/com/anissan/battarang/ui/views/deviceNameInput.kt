package com.anissan.battarang.ui.views

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.anissan.battarang.data.LocalKvStore
import com.anissan.battarang.ui.MainActivity
import com.google.android.material.textfield.TextInputEditText

fun MainActivity.setupDeviceNameInput() {
  binding.textInputLayoutDeviceName.apply {
    boxBackgroundColor = dynamicSurfaceColor

    isEndIconVisible = false

    setEndIconOnClickListener {
      binding.editTextDeviceName.clearFocus()
    }
  }

  binding.editTextDeviceName.apply {
    setText(localKvStore.deviceName)

    setOnEditorActionListener { _, actionId: Int, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        clearFocus()
        true
      } else {
        false
      }
    }

    setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
      binding.textInputLayoutDeviceName.isEndIconVisible = hasFocus

      if (!hasFocus) {
        saveEditedText(localKvStore)

        // Hiding the keyboard
        WindowCompat.getInsetsController(window, this).hide(WindowInsetsCompat.Type.ime())
      }
    }
  }
}

fun TextInputEditText.saveEditedText(localKvStore: LocalKvStore) {
  localKvStore.deviceName =
    text.toString().ifBlank { context.defaultDeviceName }.apply { setText(this) }
}
