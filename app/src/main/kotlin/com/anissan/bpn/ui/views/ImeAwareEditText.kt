package com.anissan.bpn.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.textfield.TextInputEditText

/**
 * Removes focus from EditText after hiding the keyboard.
 */
class ImeAwareEditText : TextInputEditText {
  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int,
  ) : super(context, attrs, defStyle)

  override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_BACK) clearFocus()

    return super.onKeyPreIme(keyCode, event)
  }
}
