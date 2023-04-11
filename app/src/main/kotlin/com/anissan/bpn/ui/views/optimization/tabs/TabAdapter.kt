package com.anissan.bpn.ui.views.optimization.tabs

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import com.anissan.bpn.databinding.TabPermissionBinding
import com.anissan.bpn.databinding.TabTutorialBinding

class TabAdapter(private val activityResultLauncher: ActivityResultLauncher<Intent>) :
  RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return when (viewType) {
      0 -> PermissionTabViewHolder(
        TabPermissionBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false,
        ), activityResultLauncher
      )

      1 -> TutorialTabViewHolder(
        TabTutorialBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false,
        ).root
      )

      else -> throw IllegalStateException("Currently supports two types of ViewHolder for two tabs.")
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder) {
      is PermissionTabViewHolder -> holder.refreshSwitchStates()
      is TutorialTabViewHolder -> holder.requestApiData()
    }
  }

  override fun getItemCount(): Int = 2

  override fun getItemViewType(position: Int): Int = position
}
