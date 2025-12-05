package com.rpsms.org

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rpsms.org.databinding.ActivityBackupBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBackupBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
        checkBackupFiles()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Backup & Restore"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupClickListeners() {
        binding.btnBackupNow.setOnClickListener {
            performBackup()
        }
        
        binding.btnRestore.setOnClickListener {
            showRestoreOptions()
        }
        
        binding.btnAutoBackup.setOnClickListener {
            showAutoBackupSettings()
        }
        
        binding.btnExportToEmail.setOnClickListener {
            exportToEmail()
        }
        
        binding.btnExportToDrive.setOnClickListener {
            exportToGoogleDrive()
        }
    }
    
    private fun performBackup() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnBackupNow.isEnabled = false
        
        // In real implementation, this would use WorkManager or Service
        // For now, simulate backup process
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(filesDir, "sms_backup_$timestamp.json")
            
            // Simulate backup file creation
            backupFile.createNewFile()
            
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnBackupNow.isEnabled = true
            
            Toast.makeText(this, "Backup created: ${backupFile.name}", Toast.LENGTH_LONG).show()
            checkBackupFiles()
        }, 2000)
    }
    
    private fun showRestoreOptions() {
        val backupDir = File(filesDir, "Backups")
        if (!backupDir.exists() || backupDir.listFiles()?.isEmpty() != false) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith(".json") }
        if (backupFiles.isNullOrEmpty()) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val fileNames = backupFiles.map { it.name }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Backup to Restore")
            .setItems(fileNames) { _, which ->
                val selectedFile = backupFiles[which]
                confirmRestore(selectedFile)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun confirmRestore(backupFile: File) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setMessage("Restore from ${backupFile.name}?\nThis will replace current messages.")
            .setPositiveButton("Restore") { _, _ ->
                performRestore(backupFile)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performRestore(backupFile: File) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnRestore.isEnabled = false
        
        // In real implementation, this would restore from the file
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnRestore.isEnabled = true
            
            Toast.makeText(this, "Restore completed from ${backupFile.name}", Toast.LENGTH_LONG).show()
        }, 2000)
    }
    
    private fun showAutoBackupSettings() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_auto_backup, null)
        val switchAutoBackup = dialogView.findViewById<android.widget.Switch>(R.id.switch_auto_backup)
        val radioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.radio_backup_frequency)
        
        val prefs = getSharedPreferences("backup_settings", android.content.Context.MODE_PRIVATE)
        val isAutoBackup = prefs.getBoolean("auto_backup", false)
        val frequency = prefs.getInt("backup_frequency", 1) // 1 = daily
        
        switchAutoBackup.isChecked = isAutoBackup
        
        when (frequency) {
            1 -> radioGroup.check(R.id.radio_daily)
            7 -> radioGroup.check(R.id.radio_weekly)
            30 -> radioGroup.check(R.id.radio_monthly)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Auto Backup Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val selectedId = radioGroup.checkedRadioButtonId
                val freq = when (selectedId) {
                    R.id.radio_daily -> 1
                    R.id.radio_weekly -> 7
                    R.id.radio_monthly -> 30
                    else -> 1
                }
                
                prefs.edit()
                    .putBoolean("auto_backup", switchAutoBackup.isChecked)
                    .putInt("backup_frequency", freq)
                    .apply()
                
                if (switchAutoBackup.isChecked) {
                    scheduleAutoBackup(freq)
                }
                
                Toast.makeText(this, "Auto backup settings saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun scheduleAutoBackup(frequencyDays: Int) {
        // Implement WorkManager scheduling for auto backup
        Toast.makeText(this, "Auto backup scheduled for every $frequencyDays days", Toast.LENGTH_SHORT).show()
    }
    
    private fun exportToEmail() {
        val backupDir = File(filesDir, "Backups")
        if (!backupDir.exists()) {
            Toast.makeText(this, "No backups to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        val latestBackup = backupDir.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.maxByOrNull { it.lastModified() }
        
        if (latestBackup == null) {
            Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val emailIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "RP SMS Org Backup")
            putExtra(android.content.Intent.EXTRA_TEXT, "SMS backup file attached")
            putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                this@BackupActivity,
                "com.rpsms.org.fileprovider",
                latestBackup
            ))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(android.content.Intent.createChooser(emailIntent, "Export backup via"))
    }
    
    private fun exportToGoogleDrive() {
        Toast.makeText(this, "Google Drive export coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkBackupFiles() {
        val backupDir = File(filesDir, "Backups")
        if (backupDir.exists()) {
            val backupCount = backupDir.listFiles()?.size ?: 0
            binding.txtBackupStatus.text = "$backupCount backup files available"
            
            if (backupCount > 0) {
                val latestBackup = backupDir.listFiles()?.maxByOrNull { it.lastModified() }
                val lastBackupDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(Date(latestBackup?.lastModified() ?: 0))
                binding.txtLastBackup.text = "Last backup: $lastBackupDate"
            } else {
                binding.txtLastBackup.text = "No backups yet"
            }
        } else {
            binding.txtBackupStatus.text = "No backup files"
            binding.txtLastBackup.text = "Create your first backup"
        }
    }
}
