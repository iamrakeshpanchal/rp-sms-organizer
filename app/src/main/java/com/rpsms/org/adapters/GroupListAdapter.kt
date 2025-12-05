package com.rpsms.org.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rpsms.org.databinding.ItemGroupBinding
import com.rpsms.org.models.FilterGroup

class GroupListAdapter(
    private val onItemClick: (FilterGroup) -> Unit,
    private val onEditClick: (FilterGroup) -> Unit,
    private val onDeleteClick: (FilterGroup) -> Unit
) : RecyclerView.Adapter<GroupListAdapter.GroupViewHolder>() {
    
    private var groups: List<FilterGroup> = emptyList()
    
    inner class GroupViewHolder(val binding: ItemGroupBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(groups[position])
                }
            }
            
            binding.btnEdit.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(groups[position])
                }
            }
            
            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(groups[position])
                }
            }
        }
        
        fun bind(group: FilterGroup) {
            binding.txtGroupName.text = group.name
            binding.txtKeywords.text = "Keywords: ${group.keywords}"
            binding.txtFolder.text = "Folder: ${group.folderName}"
            
            if (group.autoDelete) {
                binding.txtAutoDelete.text = "Auto delete: Yes (${group.deleteAfterHours}h)"
                binding.txtAutoDelete.setTextColor(android.graphics.Color.parseColor("#FF4CAF50"))
            } else {
                binding.txtAutoDelete.text = "Auto delete: No"
                binding.txtAutoDelete.setTextColor(android.graphics.Color.parseColor("#FF9E9E9E"))
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }
    
    override fun getItemCount(): Int = groups.size
    
    fun submitList(newGroups: List<FilterGroup>) {
        groups = newGroups
        notifyDataSetChanged()
    }
}
