package com.rpsms.org

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rpsms.org.adapters.FilterListAdapter
import com.rpsms.org.database.SmsDatabase
import com.rpsms.org.databinding.ActivityFilterSettingsBinding
import com.rpsms.org.viewmodels.FilterViewModel

class FilterSettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFilterSettingsBinding
    private lateinit var viewModel: FilterViewModel
    private lateinit var adapter: FilterListAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Filter Settings"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = FilterListAdapter(
            onItemClick = { filter ->
                // Show filter details
                showFilterDetails(filter)
            },
            onToggleClick = { filter, isChecked ->
                // Update filter status
                updateFilterStatus(filter, isChecked)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupViewModel() {
        val database = SmsDatabase.getDatabase(this)
        val factory = FilterViewModel.Factory(database.filterDao())
        viewModel = ViewModelProvider(this, factory).get(FilterViewModel::class.java)
        
        viewModel.allFilters.observe(this) { filters ->
            if (filters.isEmpty()) {
                binding.emptyState.visibility = android.view.View.VISIBLE
                binding.recyclerView.visibility = android.view.View.GONE
            } else {
                binding.emptyState.visibility = android.view.View.GONE
                binding.recyclerView.visibility = android.view.View.VISIBLE
                adapter.submitList(filters)
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnAddFilter.setOnClickListener {
            showAddFilterDialog()
        }
        
        binding.btnAutoDeleteSettings.setOnClickListener {
            showAutoDeleteSettings()
        }
        
        binding.btnImportExport.setOnClickListener {
            showImportExportOptions()
        }
    }
    
    private fun showAddFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_filter, null)
        val editName = dialogView.findViewById<android.widget.EditText>(R.id.edit_filter_name)
        val editKeywords = dialogView.findViewById<android.widget.EditText>(R.id.edit_keywords)
        val editFolder = dialogView.findViewById<android.widget.EditText>(R.id.edit_folder_name)
        val switchNotification = dialogView.findViewById<android.widget.Switch>(R.id.switch_notification)
        val switchAutoDelete = dialogView.findViewById<android.widget.Switch>(R.id.switch_auto_delete)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add New Filter")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = editName.text.toString().trim()
                val keywords = editKeywords.text.toString().trim()
                val folder = editFolder.text.toString().trim()
                
                if (name.isNotEmpty() && keywords.isNotEmpty()) {
                    val filter = com.rpsms.org.models.FilterGroup(
                        name = name,
                        keywords = keywords,
                        folderName = if (folder.isEmpty()) name else folder,
                        notificationEnabled = switchNotification.isChecked,
                        autoDelete = switchAutoDelete.isChecked,
                        deleteAfterHours = 24
                    )
                    
                    viewModel.insertFilter(filter)
                    Toast.makeText(this, "Filter added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter name and keywords", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showFilterDetails(filter: com.rpsms.org.models.FilterGroup) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter_details, null)
        val txtName = dialogView.findViewById<android.widget.TextView>(R.id.txt_filter_name)
        val txtKeywords = dialogView.findViewById<android.widget.TextView>(R.id.txt_keywords)
        val txtFolder = dialogView.findViewById<android.widget.TextView>(R.id.txt_folder)
        val txtStats = dialogView.findViewById<android.widget.TextView>(R.id.txt_stats)
        
        txtName.text = filter.name
        txtKeywords.text = filter.keywords
        txtFolder.text = filter.folderName
        
        // Get message count for this filter
        val database = SmsDatabase.getDatabase(this)
        val count = database.smsDao().getMessagesByFolder(filter.folderName).value?.size ?: 0
        txtStats.text = "Messages in folder: $count"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filter Details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Edit") { _, _ ->
                editFilter(filter)
            }
            .setNegativeButton("Delete") { _, _ ->
                deleteFilter(filter)
            }
            .show()
    }
    
    private fun editFilter(filter: com.rpsms.org.models.FilterGroup) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_filter, null)
        val editName = dialogView.findViewById<android.widget.EditText>(R.id.edit_filter_name)
        val editKeywords = dialogView.findViewById<android.widget.EditText>(R.id.edit_keywords)
        val editFolder = dialogView.findViewById<android.widget.EditText>(R.id.edit_folder_name)
        val switchNotification = dialogView.findViewById<android.widget.Switch>(R.id.switch_notification)
        val switchAutoDelete = dialogView.findViewById<android.widget.Switch>(R.id.switch_auto_delete)
        
        editName.setText(filter.name)
        editKeywords.setText(filter.keywords)
        editFolder.setText(filter.folderName)
        switchNotification.isChecked = filter.notificationEnabled
        switchAutoDelete.isChecked = filter.autoDelete
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit Filter")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = editName.text.toString().trim()
                val keywords = editKeywords.text.toString().trim()
                val folder = editFolder.text.toString().trim()
                
                if (name.isNotEmpty() && keywords.isNotEmpty()) {
                    val updatedFilter = filter.copy(
                        name = name,
                        keywords = keywords,
                        folderName = if (folder.isEmpty()) name else folder,
                        notificationEnabled = switchNotification.isChecked,
                        autoDelete = switchAutoDelete.isChecked
                    )
                    
                    viewModel.updateFilter(updatedFilter)
                    Toast.makeText(this, "Filter updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteFilter(filter: com.rpsms.org.models.FilterGroup) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Filter")
            .setMessage("Delete filter '${filter.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFilter(filter)
                Toast.makeText(this, "Filter deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateFilterStatus(filter: com.rpsms.org.models.FilterGroup, isEnabled: Boolean) {
        val updatedFilter = filter.copy(notificationEnabled = isEnabled)
        viewModel.updateFilter(updatedFilter)
    }
    
    private fun showAutoDeleteSettings() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_auto_delete, null)
        val radioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.radio_group)
        val switchOtp = dialogView.findViewById<android.widget.Switch>(R.id.switch_otp_auto_delete)
        val switchPromo = dialogView.findViewById<android.widget.Switch>(R.id.switch_promo_auto_delete)
        
        val prefs = getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val currentSetting = prefs.getInt("auto_delete_days", 7)
        
        when (currentSetting) {
            1 -> radioGroup.check(R.id.radio_1_day)
            7 -> radioGroup.check(R.id.radio_7_days)
            30 -> radioGroup.check(R.id.radio_30_days)
            else -> radioGroup.check(R.id.radio_never)
        }
        
        switchOtp.isChecked = prefs.getBoolean("auto_delete_otp", true)
        switchPromo.isChecked = prefs.getBoolean("auto_delete_promo", false)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Auto Delete Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val selectedId = radioGroup.checkedRadioButtonId
                val days = when (selectedId) {
                    R.id.radio_1_day -> 1
                    R.id.radio_7_days -> 7
                    R.id.radio_30_days -> 30
                    else -> 0 // never
                }
                
                prefs.edit()
                    .putInt("auto_delete_days", days)
                    .putBoolean("auto_delete_otp", switchOtp.isChecked)
                    .putBoolean("auto_delete_promo", switchPromo.isChecked)
                    .apply()
                
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showImportExportOptions() {
        val options = arrayOf("Export Filters", "Import Filters", "Reset to Default")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Import/Export")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportFilters()
                    1 -> importFilters()
                    2 -> resetToDefault()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportFilters() {
        // Implement filter export logic
        Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun importFilters() {
        // Implement filter import logic
        Toast.makeText(this, "Import feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun resetToDefault() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Filters")
            .setMessage("Reset all filters to default settings?")
            .setPositiveButton("Reset") { _, _ ->
                // Reset to default filters
                createDefaultFilters()
                Toast.makeText(this, "Filters reset to default", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createDefaultFilters() {
        val defaultFilters = listOf(
            com.rpsms.org.models.FilterGroup(
                name = "OTP",
                keywords = "OTP,code,verification,password",
                folderName = "OTP",
                notificationEnabled = true,
                autoDelete = true,
                deleteAfterHours = 24
            ),
            com.rpsms.org.models.FilterGroup(
                name = "Promotional",
                keywords = "offer,sale,discount,promo,win,free,cashback",
                folderName = "Promotional",
                notificationEnabled = false,
                autoDelete = false
            ),
            com.rpsms.org.models.FilterGroup(
                name = "Banking",
                keywords = "bank,transaction,account,balance,withdrawal,deposit",
                folderName = "Banking",
                notificationEnabled = true,
                autoDelete = false
            )
        )
        
        defaultFilters.forEach { filter ->
            viewModel.insertFilter(filter)
        }
    }
}
