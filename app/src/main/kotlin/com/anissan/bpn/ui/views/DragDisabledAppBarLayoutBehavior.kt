package com.anissan.bpn.ui.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.AppBarLayout

/**
 * Disables the scrolling behavior of AppBarLayout.
 * Apply in XML if AppBarLayout over-scrolls and goes underneath the status bar.
 */
class DragDisabledAppBarLayoutBehavior(
  context: Context? = null,
  attrs: AttributeSet? = null,
) : AppBarLayout.Behavior(context, attrs) {

  init {
    setDragCallback(object : DragCallback() {
      override fun canDrag(appBarLayout: AppBarLayout): Boolean = false
    })
  }
}
