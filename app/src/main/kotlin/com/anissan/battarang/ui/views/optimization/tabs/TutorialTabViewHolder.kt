package com.anissan.battarang.ui.views.optimization.tabs

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.anissan.battarang.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.elevation.SurfaceColors
import dev.chrisbanes.insetter.applyInsetter
import dev.doubledot.doki.views.DokiContentView

class TutorialTabViewHolder(private val dokiView: DokiContentView) :
  RecyclerView.ViewHolder(dokiView) {

  companion object {
    const val TAB_TITLE = "Tutorial"
  }

  init {
    dokiView.run {
      setButtonsVisibility(false)

      val appbar = findViewById<AppBarLayout>(dev.doubledot.doki.R.id.appbar)

      (appbar.getChildAt(0) as CollapsingToolbarLayout).setContentScrimColor(
        ContextCompat.getColor(context, android.R.color.transparent)
      )

      headerBackgroundColor = SurfaceColors.SURFACE_1.getColor(context)

      findViewById<View>(dev.doubledot.doki.R.id.divider3).visibility = View.GONE

      findViewById<View>(dev.doubledot.doki.R.id.footer).applyInsetter {
        type(navigationBars = true) {
          margin(vertical = true)
        }
      }
    }
  }

  fun requestApiData() {
    dokiView.loadContent(appName = dokiView.context.getString(R.string.app_name))
  }
}
