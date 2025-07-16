package com.example.craftket.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.craftket.interfaces.ActivityCallback
import com.example.craftket.Models.Activity
import com.example.craftket.databinding.ItemBinding
import com.example.craftket.utilites.ImageLoader

class ActivityAdapter(private val activities: MutableList<Activity>) :
    RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    var activityCallback: ActivityCallback? = null

    fun updateData(newActivities: List<Activity>) {
        activities.clear()
        activities.addAll(newActivities)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun getItemCount(): Int = activities.size

    fun getItem(position: Int) = activities[position]

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                binding.itemLBLTitle.text = name
                binding.itemLBLPrice.text = price.toString()
                binding.itemLBLLocation.text = location.getFullAddress()
                ImageLoader.getInstance().loadImage(imageUrl, binding.itemIMGPoster)

//                binding.itemBTNMoreInfo.setOnClickListener {
//                    activityCallback?.moreInfoClicked(getItem(position), position)
//                }
            }

        }
    }

    inner class ActivityViewHolder(val binding: ItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
//            binding.root.setOnClickListener {
//                activityCallback?.activityClicked(getItem(adapterPosition), adapterPosition)
//            }
            binding.itemBTNMoreInfo.setOnClickListener {
                activityCallback?.moreInfoClicked(getItem(adapterPosition), adapterPosition)
            }
        }
    }
}
