package com.rpsms.org.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rpsms.org.SmsBackupService
import com.rpsms.org.database.SmsDatabase

class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    
    override fun doWork(): Result {
        return try {
            // Check if auto backup is enabled
            val prefs = applicationContext.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
            val isAutoBackup = prefs.getBoolean("auto_backup", false)
            
            if (!isAutoBackup) {
                return Result.success() // Auto backup disabled
            }
            
            // Check if we have enough messages to backup
            val database = SmsDatabase.getDatabase(applicationContext)
            val messageCount = database.smsDao().getAllMessages().value?.size ?: 0
            
            if (messageCount == 0) {
                return Result.success() // No messages to backup
            }
            
            // Trigger backup service
            val intent = android.content.Intent(applicationContext, SmsBackupService::class.java).apply {
                action = SmsBackupService.ACTION_BACKUP
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
