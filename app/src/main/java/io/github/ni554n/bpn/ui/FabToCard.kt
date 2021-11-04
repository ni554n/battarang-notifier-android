package io.github.ni554n.bpn.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import io.github.ni554n.bpn.R
import io.github.ni554n.bpn.databinding.ActivityMainBinding

class FabToCard(
  private val context: Context,
  mainActivityBinding: ActivityMainBinding,
) : OnBackPressedCallback(false) {
  private val root: CoordinatorLayout = mainActivityBinding.root
  private val scrim: View = mainActivityBinding.scrimView
  private val fab: ExtendedFloatingActionButton = mainActivityBinding.fabPair
  private val card: MaterialCardView = mainActivityBinding.pairingCardView

  init {
//    card.setOnClickListener { collapseToFab() }

    scrim.setOnClickListener { toggleCardVisibility() }
  }

  private val expandTransform: MaterialContainerTransform = generateContainerTransform(
    startView = fab,
    endView = card,
  )

  private val collapseTransform: MaterialContainerTransform = generateContainerTransform(
    startView = card,
    endView = fab,
  )

  private var isCardExpanded = false

  override fun handleOnBackPressed() = toggleCardVisibility()

  fun toggleCardVisibility() {
    // Remove the scrim view and on back pressed callbacks
    isCardExpanded = !isCardExpanded

    // Listening for back press:
    isEnabled = isCardExpanded

    scrim.visibility = if (isCardExpanded) View.VISIBLE else View.GONE

    val transform: MaterialContainerTransform =
      if (isCardExpanded) expandTransform else collapseTransform

    TransitionManager.beginDelayedTransition(root, transform)

    fab.visibility = if (isCardExpanded) View.GONE else View.VISIBLE
    card.visibility = if (isCardExpanded) View.VISIBLE else View.GONE
  }

  private fun generateContainerTransform(
    startView: View,
    endView: View,
  ): MaterialContainerTransform {
    return MaterialContainerTransform().apply {
      this.startView = startView
      this.endView = endView

      scrimColor = Color.TRANSPARENT

      // Have the transform match the endView card's native elevation as closely as possible.
      endElevation =
        context.resources.getDimension(R.dimen.email_recipient_card_popup_elevation_compat)

      setPathMotion(MaterialArcMotion())

      // Avoid having this transform from running on both the start and end views by setting
      // its target to the endView.
      addTarget(endView)
    }
  }
}
