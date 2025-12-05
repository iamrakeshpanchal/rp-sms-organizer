package com.rpsms.org.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rpsms.org.adapters.GroupListAdapter
import com.rpsms.org.database.SmsDatabase
import com.rpsms.org.databinding.FragmentGroupsBinding
import com.rpsms.org.models.FilterGroup
import com.rpsms.org.viewmodels.FilterViewModel

class GroupsFragment : Fragment() {
    
    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: FilterViewModel
    private lateinit var adapter: GroupListAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupViewModel()
        setupFAB()
    }
    
    private fun setupRecyclerView() {
        adapter = GroupListAdapter(
            onItemClick = { filter ->
                // Open filter details
                openFilterDetails(filter)
            },
            onEditClick = { filter ->
                // Edit filter
                editFilter(filter)
            },
            onDeleteClick = { filter ->
                // Delete filter
                deleteFilter(filter)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupViewModel() {
        val database = SmsDatabase.getDatabase(requireContext())
        val factory = FilterViewModel.Factory(database.filterDao())
        viewModel = ViewModelProvider(this, factory).get(FilterViewModel::class.java)
        
        viewModel.allFilters.observe(viewLifecycleOwner) { filters ->
            if (filters.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.emptyText.text = "No groups/filters created\nTap + to create your first group"
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(filters)
            }
        }
    }
    
    private fun setupFAB() {
        binding.fabAddGroup.setOnClickListener {
            showCreateGroupDialog()
        }
    }
    
    private fun showCreateGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val editName = dialogView.findViewById<android.widget.EditText>(R.id.edit_group_name)
        val editKeywords = dialogView.findViewById<android.widget.EditText>(R.id.edit_keywords)
        val editFolder = dialogView.findViewById<android.widget.EditText>(R.id.edit_folder_name)
        val switchAutoDelete = dialogView.findViewById<android.widget.Switch>(R.id.switch_auto_delete)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Create New Group/Filter")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = editName.text.toString().trim()
                val keywords = editKeywords.text.toString().trim()
                val folder = editFolder.text.toString().trim()
                val autoDelete = switchAutoDelete.isChecked
                
                if (name.isNotEmpty() && keywords.isNotEmpty()) {
                    val keywordList = keywords.split(",").map { it.trim() }
                    
                    val filter = FilterGroup(
                        name = name,
                        keywords = keywordList.joinToString(","),
                        folderName = folder.ifEmpty { name },
                        autoDelete = autoDelete
                    )
                    
                    viewModel.insertFilter(filter)
                    
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Group '$name' created",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Please enter name and keywords",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun openFilterDetails(filter: FilterGroup) {
        val intent = android.content.Intent(requireContext(), com.rpsms.org.FilterDetailsActivity::class.java).apply {
            putExtra("filter_id", filter.id)
        }
        startActivity(intent)
    }
    
    private fun editFilter(filter: FilterGroup) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val editName = dialogView.findViewById<android.widget.EditText>(R.id.edit_group_name)
        val editKeywords = dialogView.findViewById<android.widget.EditText>(R.id.edit_keywords)
        val editFolder = dialogView.findViewById<android.widget.EditText>(R.id.edit_folder_name)
        val switchAutoDelete = dialogView.findViewById<android.widget.Switch>(R.id.switch_auto_delete)
        
        editName.setText(filter.name)
        editKeywords.setText(filter.keywords)
        editFolder.setText(filter.folderName)
        switchAutoDelete.isChecked = filter.autoDelete
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Group")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = editName.text.toString().trim()
                val keywords = editKeywords.text.toString().trim()
                val folder = editFolder.text.toString().trim()
                
                if (name.isNotEmpty() && keywords.isNotEmpty()) {
                    val updatedFilter = filter.copy(
                        name = name,
                        keywords = keywords,
                        folderName = folder.ifEmpty { name },
                        autoDelete = switchAutoDelete.isChecked
                    )
                    
                    viewModel.updateFilter(updatedFilter)
                    
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Group updated",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteFilter(filter: FilterGroup) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Group")
            .setMessage("Delete '${filter.name}'? This will not delete messages, only the filter.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFilter(filter)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Group deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
