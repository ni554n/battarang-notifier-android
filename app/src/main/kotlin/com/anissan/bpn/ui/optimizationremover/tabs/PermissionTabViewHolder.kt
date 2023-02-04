package com.anissan.bpn.ui.optimizationremover.tabs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import androidx.recyclerview.widget.RecyclerView
import com.anissan.bpn.BuildConfig
import com.anissan.bpn.databinding.TabPermissionBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.common.util.concurrent.ListenableFuture
import com.judemanutd.autostarter.AutoStartPermissionHelper
import dev.chrisbanes.insetter.applyInsetter

class PermissionTabViewHolder(
  private val permissionBinding: TabPermissionBinding,
  private val activityResultLauncher: ActivityResultLauncher<Intent>,
) :
  RecyclerView.ViewHolder(permissionBinding.root) {

  companion object {
    const val TAB_TITLE = "Permissions"
  }

  init {
    permissionBinding.apply {
      layoutOptimizerConfig.applyInsetter {
        type(navigationBars = true) {
          margin(vertical = true)
        }
      }

      setupIgnoreOptimizationSwitch()
      setupDisableHibernation()
      setupAutoStarter()
    }
  }

  @SuppressLint("BatteryLife")
  private fun TabPermissionBinding.setupIgnoreOptimizationSwitch() {
    switchIgnoreBatteryOptimization.run {
      if (Build.VERSION.SDK_INT < 23) {
        cardIgnoreBatteryOptimization.disableCard()
        isEnabled = false
        return
      }

      disableSwitchDragging()
      bindClicksFrom(cardIgnoreBatteryOptimization)

      setOnClickListener { switch: View ->
        val intent = Intent()

        if ((switch as MaterialSwitch).isChecked) {
          intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
          intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        } else {
          intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }

        context.startActivity(intent)
      }

      refreshIgnoreOptimizationSwitch()
    }
  }

  private fun MaterialSwitch.refreshIgnoreOptimizationSwitch() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      isChecked =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
          BuildConfig.APPLICATION_ID
        )
    }
  }


  private fun TabPermissionBinding.setupDisableHibernation() {
    switchDisableHibernation.run {
      disableSwitchDragging()

      bindClicksFrom(cardDisableHibernation)

      cardDisableHibernation.disableIfAppHibernationUnSupported(this)

      val optionName = when (Build.VERSION.SDK_INT) {
        in 23..30 -> "Remove permissions if app isn't used"
        31, 32 -> "Remove permissions and free up space"
        else -> "Pause app activity if unused"
      }

      setOnClickListener {
        val intent =
          IntentCompat.createManageUnusedAppRestrictionsIntent(context, BuildConfig.APPLICATION_ID)

        activityResultLauncher.launch(intent)

        val onOffText: String = if (switchDisableHibernation.isChecked) "OFF" else "ON"
        Toast.makeText(context, "Turn $onOffText: \"$optionName\"", Toast.LENGTH_LONG).show()
      }
    }
  }

  private fun MaterialCardView.disableIfAppHibernationUnSupported(materialSwitch: MaterialSwitch) {
    val future = PackageManagerCompat.getUnusedAppRestrictionsStatus(context)
    try {
      future.addListener(
        {
          when (future.get()) {
            UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE -> {
              materialSwitch.isEnabled = false
              disableCard()
            }

            UnusedAppRestrictionsConstants.DISABLED -> materialSwitch.isChecked = true
          }
        },
        ContextCompat.getMainExecutor(context)
      )
    } catch (e: Exception) {
      materialSwitch.isEnabled = false
      disableCard()
    }
  }

  private fun MaterialSwitch.refreshDisableHibernationSwitch() {
    val future: ListenableFuture<Int> = PackageManagerCompat.getUnusedAppRestrictionsStatus(context)
    try {
      future.addListener(
        { isChecked = future.get() == UnusedAppRestrictionsConstants.DISABLED },
        ContextCompat.getMainExecutor(context)
      )
    } catch (e: Exception) {
      isChecked = false
    }
  }

  private fun TabPermissionBinding.setupAutoStarter() {
    val context = root.context
    val autoStartPermissionHelper = AutoStartPermissionHelper.getInstance()

    if (autoStartPermissionHelper.isAutoStartPermissionAvailable(context, false).not()) {
      cardAllowAutoStart.disableCard()
    }

    cardAllowAutoStart.setOnClickListener {
      autoStartPermissionHelper.getAutoStartPermission(context, open = true)
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun MaterialSwitch.disableSwitchDragging() {
    setOnTouchListener { _, event ->
      event.actionMasked == MotionEvent.ACTION_MOVE
    }
  }

  /**
   * Checkbox have a ripple animation set only on its checkmark icon, not the text beside it.
   * Setting a custom ripple on the whole checkbox doesn't work if margin is applied and the ripple won't reach the edges.
   * The only workaround I found is to use a full bleed wrapper card (which already has a nice ripple effect built-in)
   * and disabling the checkbox so that the card behind gets the click. But then I have to programmatically
   * pass the card clicks to the checkbox.
   */
  private fun MaterialSwitch.bindClicksFrom(card: MaterialCardView) {
    card.setOnClickListener { performClick() }
  }

  private fun MaterialCardView.disableCard() {
    isEnabled = false
    alpha = 0.5f
  }

  fun refreshSwitchStates() {
    permissionBinding.switchIgnoreBatteryOptimization.refreshIgnoreOptimizationSwitch()
    permissionBinding.switchDisableHibernation.refreshDisableHibernationSwitch()
  }
}
