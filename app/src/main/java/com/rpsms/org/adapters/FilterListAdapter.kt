package com.rpsms.org.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rpsms.org.databinding.ItemFilterBinding
import com.rpsms.org.models.FilterGroup

class FilterListAdapter(
    private val onItemClick: (FilterGroup) -> Unit,
    private val onToggleClick: (FilterGroup, Boolean) -> Unit
) : RecyclerView.Adapter<FilterListAdapter.FilterViewHolder>() {
    
    private var filters: List<FilterGroup> = emptyList()
    
    inner class FilterViewHolder(val binding: ItemFilterBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(filters[position])
                }
            }
            
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onToggleClick(filters[position], isChecked)
                }
            }
        }
        
        fun bind(filter: FilterGroup) {
            binding.txtFilterName.text = filter.name
            binding.txtKeywords.text = filter.keywords
            binding.switchEnabled.isChecked = filter.notificationEnabled
            
            if (filter.autoDelete) {
                binding.txtSettings.text = "Auto delete after ${filter.deleteAfterHours}h"
            } else {
                binding.txtSettings.text = "No auto delete"
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filters[position])
    }
    
    override fun getItemCount(): Int = filters.size
    
    fun submitList(newFilters: List<FilterGroup>) {
        filters = newFilters
        notifyDataSetChanged()
    }
}
