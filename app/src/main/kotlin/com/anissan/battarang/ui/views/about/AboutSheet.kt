package com.anissan.battarang.ui.views.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.anissan.battarang.BuildConfig
import com.anissan.battarang.R
import com.anissan.battarang.databinding.SheetAboutBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.elevation.SurfaceColors
import com.mikepenz.aboutlibraries.LibsBuilder
import dev.chrisbanes.insetter.applyInsetter
import java.net.URI


class AboutSheet : BottomSheetDialogFragment() {

  companion object {
    fun show(fragmentManager: FragmentManager) {
      AboutSheet().show(fragmentManager, "about_sheet")
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = SheetAboutBinding.inflate(inflater, container, false).run {
    // Bottom sheet's default auto peek height on widescreen is very low.
    // Setting it around 67% of the screen height.
    (dialog as BottomSheetDialog).behavior.peekHeight =
      (resources.displayMetrics.heightPixels / 1.5).toInt()

    aboutLayout.applyInsetter {
      type(navigationBars = true) {
        padding(vertical = true)
      }
    }

    setupViews()

    root
  }

  private fun SheetAboutBinding.setupViews() {
    authorDetailsCard.setCardBackgroundColor(SurfaceColors.SURFACE_3.getColor(requireContext()))

    receiverLinkButton.setOnClickListener { openLinkInBrowser(BuildConfig.RECEIVER_WEBSITE) }
    telegramLinkButton.setOnClickListener { openLinkInBrowser(BuildConfig.TELEGRAM_BOT_URL) }

    sourceLinkButton.setOnClickListener { openLinkInBrowser(BuildConfig.GITHUB_URL) }
    issuesLinkButton.setOnClickListener { openLinkInBrowser(BuildConfig.ISSUES_URL) }
    reviewLinkButton.setOnClickListener { openLinkInBrowser(BuildConfig.PLAY_STORE_LINK) }

    authorNameText.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_WEBSITE) }
    authorWebsiteLinkButton.text = URI(BuildConfig.AUTHOR_WEBSITE).host
    authorWebsiteLinkButton.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_WEBSITE) }
    twitterLinkButton.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_TWITTER) }
    linkedinLinkButton.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_LINKEDIN) }

    licenseChip.setOnClickListener {
      LibsBuilder()
        .withActivityTitle(getString(R.string.licenses))
        .withSearchEnabled(true)
        .start(requireContext())
    }

    policyLinkChip.setOnClickListener { openLinkInBrowser(BuildConfig.PRIVACY_POLICY_URL) }
    tosLinkChip.setOnClickListener { openLinkInBrowser(BuildConfig.TOS_URL) }
  }

  private fun openLinkInBrowser(url: String) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
      Toast.makeText(requireContext(), R.string.no_browser_found, Toast.LENGTH_LONG).show()
    }
  }

  override fun onStart() {
    super.onStart()

    // The sheet dialog is initially set to WRAP_CONTENT, which leaves a big gap at the bottom on Fullscreen mode.
    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.layoutParams?.height =
      ViewGroup.LayoutParams.MATCH_PARENT
  }
}
