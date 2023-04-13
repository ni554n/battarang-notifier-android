package com.anissan.battarang.ui.views.optimization

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.anissan.battarang.databinding.SheetOptimizationRemoverBinding
import com.anissan.battarang.ui.views.optimization.tabs.PermissionTabViewHolder
import com.anissan.battarang.ui.views.optimization.tabs.TabAdapter
import com.anissan.battarang.ui.views.optimization.tabs.TutorialTabViewHolder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * This [BottomSheetDialogFragment] holds the ViewPager which in turns holds the two tabs.
 * */
class OptimizationRemoverSheet : BottomSheetDialogFragment() {

  companion object {
    fun show(fragmentManager: FragmentManager) {
      OptimizationRemoverSheet().show(
        fragmentManager,
        "Bottom Sheet for Removing Battery Optimizations"
      )
    }
  }

  private val activityResultLauncher: ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

  private val tabAdapter = TabAdapter(activityResultLauncher)
  private var viewBindings: SheetOptimizationRemoverBinding? = null

  private val tabChangeCallback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageSelected(position: Int) {
      when (position) {
        // Expanding the "Tutorial" tab to its full height, otherwise it won't get expanded upon dragging.
        // See {setupTabViewPager} for more details.
        1 -> (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = SheetOptimizationRemoverBinding.inflate(inflater, container, false).run {
    viewBindings = this

    // Bottom sheet's default auto peek height on widescreen is very low.
    // Setting it around 67% of the screen height.
    (dialog as BottomSheetDialog).behavior.peekHeight =
      (resources.displayMetrics.heightPixels / 1.5).toInt()

    setupTabViewPager()

    root
  }

  override fun onStart() {
    super.onStart()

    // The sheet dialog is initially set to WRAP_CONTENT. Works as expected when the bottom sheet has only one layout.
    // But when there's ViewPager with different sized tabs, the dialog leaves a big gap at the bottom.
    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.layoutParams?.height =
      ViewGroup.LayoutParams.MATCH_PARENT
  }

  @SuppressLint("NotifyDataSetChanged")
  override fun onResume() {
    super.onResume()

    // Refreshes the "Permission" tab to sync the Switch states after returning.
    // Tried calling the more specific version: `notifyItemChanged(0)`, but it freezes the
    // the bottom sheet dragging behavior somehow.
    tabAdapter.notifyDataSetChanged()
  }

  override fun onDestroy() {
    viewBindings?.viewPager?.unregisterOnPageChangeCallback(tabChangeCallback)
    viewBindings = null

    super.onDestroy()
  }

  private fun SheetOptimizationRemoverBinding.setupTabViewPager() {
    viewPager.run {
      adapter = tabAdapter

      registerOnPageChangeCallback(tabChangeCallback)

      // ViewPager2 internally uses a RecyclerView to manage the pages. But it is scrollable; so
      // the bottom sheet underneath don't expand or collapse upon dragging. Turning it off so that
      // bottom sheet don't stuck to the default height. But it only works for the "Permission" tab,
      // "Tutorial" tab won't expand on dragging.
      // As a workaround, I'm manually expanding the "Tutorial" tab on {tabChangeCallback}.
      for (view: View in children) {
        if (view is RecyclerView) {
          view.isNestedScrollingEnabled = false
          view.overScrollMode = View.OVER_SCROLL_NEVER

          break
        }
      }
    }

    // Linking up the Tab names and swiping behavior with the ViewPager.
    TabLayoutMediator(tabLayout, viewPager) { tab: TabLayout.Tab, position: Int ->
      when (position) {
        0 -> tab.text = PermissionTabViewHolder.TAB_TITLE
        1 -> tab.text = TutorialTabViewHolder.TAB_TITLE
      }
    }.attach()
  }
}
