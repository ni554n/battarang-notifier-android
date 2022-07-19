package io.github.ni554n.bpn.ui.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.AppBarLayout

// Not unused. "app:layout_behavior" uses class by its full class name from XML.
@Suppress("unused")
class DragDisabledAppBarLayoutBehavior : AppBarLayout.Behavior {

  constructor() : super()
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

  init {
    setDragCallback(object : DragCallback() {
      override fun canDrag(appBarLayout: AppBarLayout): Boolean = false
    })
  }
}
