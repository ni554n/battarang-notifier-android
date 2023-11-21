package com.anissan.battarang.ui.views

import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import com.anissan.battarang.R
import com.anissan.battarang.network.MessageType
import com.anissan.battarang.ui.MainActivity
import com.anissan.battarang.ui.views.about.AboutSheet
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.google.android.material.snackbar.Snackbar

fun MainActivity.setupButtonBar() {
  binding.buttonBarCard.setCardBackgroundColor(dynamicSurfaceColor)

  unpairButton()
  testButton()
  aboutButton()
}

fun MainActivity.unpairButton() {
  binding.unpairButton.apply {
    visibility = if (paired) View.VISIBLE else View.GONE

    setOnClickListener {
      showSnackbar(R.string.unpair_instruction, Snackbar.LENGTH_SHORT)
    }

    setOnLongClickListener {
      localKvStore.receiverToken = null
      true
    }
  }
}

fun MainActivity.testButton() {
  val sendIcon: Drawable = binding.testButton.icon

  val loadingIcon = IndeterminateDrawable.createCircularDrawable(
    this,
    CircularProgressIndicatorSpec(
      this,
      null,
      0,
      com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall,
    )
  )

  binding.testButton.apply {
    visibility = if (paired) View.VISIBLE else View.GONE

    TooltipCompat.setTooltipText(this, contentDescription)

    setOnClickListener {
      icon = loadingIcon

      receiverApiClient.sendNotification(MessageType.TEST) { response: String? ->
        icon = sendIcon

        if (response == null) showSnackbar(R.string.network_error)
        else {
          val message = response.split("||")

          showSnackbar(
            if (message.size == 3)
              message.drop(1).joinToString(". ") { it.trim() }
            else response)
        }
      }
    }
  }

  binding.serviceNameText.text = localKvStore.pairedServiceName
}

fun MainActivity.aboutButton() {
  binding.aboutButton.setOnClickListener {
    AboutSheet.show(supportFragmentManager)
  }
}
