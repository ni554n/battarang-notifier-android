package io.github.ni554n.bpn.ui.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.AppBarLayout

class DragDisabledAppBarLayoutBehavior(context: Context? = null, attrs: AttributeSet? = null) :
  AppBarLayout.Behavior(context, attrs) {

  init {
    setDragCallback(object : DragCallback() {
      override fun canDrag(appBarLayout: AppBarLayout): Boolean = false
    })
  }
}
