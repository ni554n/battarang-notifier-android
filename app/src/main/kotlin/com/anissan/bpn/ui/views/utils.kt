package com.anissan.bpn.ui.views

import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox

/**
 * Checkboxes has a ripple animation set only on its checkmark icon area, not the text beside it.
 * Setting a custom ripple on the whole checkbox doesn't look good if there's a margin applied because
 * the ripple won't reach the edges.
 *
 * The only workaround I've found is to use a full bleed wrapper card (which already has a nice
 * ripple effect built-in) and disabling the checkbox so that
 * the card receives the clicks which it then forwards it to the checkbox.
 */
fun MaterialCheckBox.bindClicksFrom(card: MaterialCardView) {
  card.setOnClickListener { performClick() }
}
