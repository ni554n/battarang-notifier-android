package com.anissan.bpn.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.anissan.bpn.BuildConfig
import com.anissan.bpn.databinding.SheetAboutBinding
import com.anissan.bpn.utils.logW
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.chrisbanes.insetter.applyInsetter


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

    setupExternalLinks()

    root
  }

  override fun onStart() {
    super.onStart()

    // The sheet dialog is initially set to WRAP_CONTENT. Works as expected when the bottom sheet has only one layout.
    // But when there's ViewPager with different sized tabs, the dialog leaves a big gap at the bottom.
    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.layoutParams?.height =
      ViewGroup.LayoutParams.MATCH_PARENT
  }

  private fun SheetAboutBinding.setupExternalLinks() {
    receiver.setOnClickListener { openLinkInBrowser(BuildConfig.RECEIVER_DOMAIN) }
    telegram.setOnClickListener { openLinkInBrowser(BuildConfig.TELEGRAM_BOT_URL) }

    source.setOnClickListener { openLinkInBrowser(BuildConfig.GITHUB_URL) }
    issues.setOnClickListener { openLinkInBrowser(BuildConfig.ISSUES_URL) }
    review.setOnClickListener { openLinkInBrowser(BuildConfig.PLAY_STORE_LINK) }

    authorName.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_WEBSITE) }
    authorWebsite.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_WEBSITE) }
    twitter.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_TWITTER) }
    linkedin.setOnClickListener { openLinkInBrowser(BuildConfig.AUTHOR_LINKEDIN) }

    license.setOnClickListener {
      startActivity(Intent(context, OssLicensesMenuActivity::class.java))
    }

    policy.setOnClickListener { openLinkInBrowser(BuildConfig.PRIVACY_POLICY_URL) }
    tos.setOnClickListener { openLinkInBrowser(BuildConfig.TOC_URL) }
  }

  private fun openLinkInBrowser(url: String) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
      logW { "No browser installed on the device so can't open the link." }
    }
  }
}
