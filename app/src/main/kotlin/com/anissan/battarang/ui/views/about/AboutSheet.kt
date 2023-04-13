package com.anissan.battarang.ui.views.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.anissan.battarang.BuildConfig
import com.anissan.battarang.R
import com.anissan.battarang.databinding.SheetAboutBinding
import com.anissan.battarang.utils.logW
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.aboutlibraries.LibsBuilder
import dev.chrisbanes.insetter.applyInsetter
import java.net.URI


class AboutSheet : BottomSheetDialogFragment() {

  companion object {
    fun show(fragmentManager: FragmentManager) {
      AboutSheet().show(fragmentManager, "Bottom Sheet for About")
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

  override fun onStart() {
    super.onStart()

    // The sheet dialog is initially set to WRAP_CONTENT. Works as expected when the bottom sheet has only one layout.
    // But when there's ViewPager with different sized tabs, the dialog leaves a big gap at the bottom.
    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.layoutParams?.height =
      ViewGroup.LayoutParams.MATCH_PARENT
  }

  private fun SheetAboutBinding.setupViews() {
    receiver.setOnClickListener { openLinkInBrowser(BuildConfig.RECEIVER_WEBSITE) }
    telegram.setOnClickListener { openLinkInBrowser(BuildConfig.TELEGRAM_BOT_URL) }

    source.setOnClickListener { openLinkInBrowser(BuildConfig.GITHUB_URL) }
    issues.setOnClickListener { openLinkInBrowser(BuildConfig.ISSUES_URL) }
    review.setOnClickListener { openLinkInBrowser(BuildConfig.PLAY_STORE_LINK) }

    authorName.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_WEBSITE) }
    authorWebsite.text = URI(BuildConfig.AUTHOR_WEBSITE).host
    authorWebsite.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_WEBSITE) }
    twitter.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_TWITTER) }
    linkedin.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_LINKEDIN) }

    license.setOnClickListener {
      LibsBuilder()
        .withActivityTitle(getString(R.string.licenses))
        .withSearchEnabled(true)
        .start(requireContext())
    }

    policy.setOnClickListener { openLinkInBrowser(BuildConfig.PRIVACY_POLICY_URL) }
    tos.setOnClickListener { openLinkInBrowser(BuildConfig.TOS_URL) }
  }

  private fun openLinkInBrowser(url: String) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
      logW { "No browser installed on the device so can't open the link." }
    }
  }
}
