package com.rpsms.org

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.rpsms.org.database.SmsDatabase
import com.rpsms.org.models.SmsMessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsBackupService : Service() {
    
    companion object {
        private const val TAG = "SmsBackupService"
        private const val CHANNEL_ID = "backup_channel"
        private const val NOTIFICATION_ID = 1002
        
        const val ACTION_BACKUP = "com.rpsms.org.ACTION_BACKUP"
        const val ACTION_RESTORE = "com.rpsms.org.ACTION_RESTORE"
        const val EXTRA_BACKUP_PATH = "backup_path"
    }
    
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_BACKUP -> startBackup()
            ACTION_RESTORE -> {
                val backupPath = intent.getStringExtra(EXTRA_BACKUP_PATH)
                if (backupPath != null) {
                    startRestore(backupPath)
                }
            }
        }
        return START_NOT_STICKY
    }
    
    private fun startBackup() {
        showNotification("Backing up SMS...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = SmsDatabase.getDatabase(applicationContext)
                val allMessages = database.smsDao().getAllMessages()
                
                // Convert to JSON
                val backupData = BackupData(
                    messages = allMessages,
                    backupDate = System.currentTimeMillis(),
                    totalMessages = allMessages.size
                )
                
                val json = gson.toJson(backupData)
                
                // Save to file
                val timestamp = dateFormat.format(Date())
                val backupDir = File(getExternalFilesDir(null), "Backups")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                
                val backupFile = File(backupDir, "sms_backup_$timestamp.json")
                FileOutputStream(backupFile).use { fos ->
                    fos.write(json.toByteArray())
                }
                
                // Also save to app-specific directory
                val internalBackup = File(filesDir, "sms_backup_$timestamp.json")
                FileOutputStream(internalBackup).use { fos ->
                    fos.write(json.toByteArray())
                }
                
                // Upload to Google Drive (optional)
                // uploadToGoogleDrive(backupFile)
                
                updateNotification("Backup completed: ${backupFile.path}")
                sendBroadcast(Intent("BACKUP_COMPLETED").apply {
                    putExtra("file_path", backupFile.absolutePath)
                })
                
                Log.d(TAG, "Backup completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                updateNotification("Backup failed: ${e.message}")
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }
    }
    
    private fun startRestore(backupPath: String) {
        showNotification("Restoring SMS...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val backupFile = File(backupPath)
                if (!backupFile.exists()) {
                    throw Exception("Backup file not found")
                }
                
                val json = backupFile.readText()
                val backupData = gson.fromJson(json, BackupData::class.java)
                
                val database = SmsDatabase.getDatabase(applicationContext)
                
                // Clear existing data
                database.clearAllTables()
                
                // Restore messages
                backupData.messages.forEach { message ->
                    database.smsDao().insert(message.copy(id = 0))
                }
                
                updateNotification("Restore completed: ${backupData.totalMessages} messages")
                sendBroadcast(Intent("RESTORE_COMPLETED"))
                
                Log.d(TAG, "Restore completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                updateNotification("Restore failed: ${e.message}")
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }
    }
    
    private fun showNotification(message: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RP SMS Org")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_backup)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RP SMS Org")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_backup)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) 
            as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backup Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows backup and restore progress"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) 
                as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    data class BackupData(
        val messages: List<SmsMessageModel>,
        val backupDate: Long,
        val totalMessages: Int
    )
}
