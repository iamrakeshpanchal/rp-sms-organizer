package com.rpsms.org

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.rpsms.org.workers.DailySummaryWorker
import com.rpsms.org.workers.AutoBackupWorker
import com.rpsms.org.workers.AutoDeleteWorker
import java.util.concurrent.TimeUnit

class SMSOrganizerApp : Application() {
    
    companion object {
        const val CHANNEL_SMS = "sms_channel"
        const val CHANNEL_BACKUP = "backup_channel"
        const val CHANNEL_SUMMARY = "summary_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleWorkers()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // SMS Notification Channel
            val smsChannel = NotificationChannel(
                CHANNEL_SMS,
                "SMS Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows SMS notifications and bubbles"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            // Backup Notification Channel
            val backupChannel = NotificationChannel(
                CHANNEL_BACKUP,
                "Backup Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows backup status"
                setShowBadge(false)
            }
            
            // Summary Notification Channel
            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY,
                "Daily Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily SMS summary"
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) 
                as NotificationManager
            notificationManager.createNotificationChannels(
                listOf(smsChannel, backupChannel, summaryChannel)
            )
        }
    }
    
    private fun scheduleWorkers() {
        // Schedule daily summary at 8 PM
        val dailySummaryRequest = PeriodicWorkRequest.Builder(
            DailySummaryWorker::class.java,
            1, TimeUnit.DAYS
        )
            .setInitialDelay(calculateInitialDelay(20), TimeUnit.HOURS) // 8 PM
            .build()
        
        // Schedule auto backup daily at 2 AM
        val autoBackupRequest = PeriodicWorkRequest.Builder(
            AutoBackupWorker::class.java,
            1, TimeUnit.DAYS
        )
            .setInitialDelay(calculateInitialDelay(2), TimeUnit.HOURS) // 2 AM
            .build()
        
        // Schedule auto delete hourly
        val autoDeleteRequest = PeriodicWorkRequest.Builder(
            AutoDeleteWorker::class.java,
            1, TimeUnit.HOURS
        )
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        
        val workManager = WorkManager.getInstance(this)
        
        workManager.enqueueUniquePeriodicWork(
            "daily_summary",
            ExistingPeriodicWorkPolicy.KEEP,
            dailySummaryRequest
        )
        
        workManager.enqueueUniquePeriodicWork(
            "auto_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            autoBackupRequest
        )
        
        workManager.enqueueUniquePeriodicWork(
            "auto_delete",
            ExistingPeriodicWorkPolicy.KEEP,
            autoDeleteRequest
        )
    }
    
    private fun calculateInitialDelay(targetHour: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        var delay = targetHour - currentHour
        if (delay < 0) {
            delay += 24 // Next day
        }
        
        return delay.toLong()
    }
}
