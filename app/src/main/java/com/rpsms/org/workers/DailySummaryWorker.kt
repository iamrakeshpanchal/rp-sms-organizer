package com.rpsms.org.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rpsms.org.R
import com.rpsms.org.database.SmsDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailySummaryWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    
    companion object {
        const val CHANNEL_ID = "summary_channel"
        const val NOTIFICATION_ID = 1003
    }
    
    override fun doWork(): Result {
        return try {
            val database = SmsDatabase.getDatabase(applicationContext)
            val now = System.currentTimeMillis()
            val yesterday = now - (24 * 60 * 60 * 1000)
            
            // Get messages from last 24 hours
            val recentMessages = database.smsDao().getMessagesBetweenDates(yesterday, now)
            
            // Calculate statistics
            var totalMessages = 0
            var unreadCount = 0
            var otpCount = 0
            var promotionalCount = 0
            
            recentMessages.value?.forEach { message ->
                totalMessages++
                if (!message.read) unreadCount++
                if (message.isOTP) otpCount++
                if (message.isPromotional) promotionalCount++
            }
            
            // Create and show notification
            showSummaryNotification(totalMessages, unreadCount, otpCount, promotionalCount)
            
            // Log to database or shared preferences
            saveSummaryStats(totalMessages, unreadCount, otpCount, promotionalCount)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun showSummaryNotification(
        total: Int,
        unread: Int,
        otp: Int,
        promotional: Int
    ) {
        createNotificationChannel()
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.format(Date())
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_summary)
            .setContentTitle("Daily SMS Summary - $date")
            .setContentText("Total: $total | Unread: $unread | OTP: $otp | Promo: $promotional")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        """
                        📱 Daily SMS Summary - $date
                        
                        Total Messages: $total
                        Unread Messages: $unread
                        OTP Messages: $otp
                        Promotional Messages: $promotional
                        
                        Open app to view details.
                        """.trimIndent()
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily SMS summary notifications"
            }
            
            val notificationManager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun saveSummaryStats(
        total: Int,
        unread: Int,
        otp: Int,
        promotional: Int
    ) {
        val prefs = applicationContext.getSharedPreferences("summary_stats", Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        prefs.edit()
            .putInt("total_$today", total)
            .putInt("unread_$today", unread)
            .putInt("otp_$today", otp)
            .putInt("promotional_$today", promotional)
            .putString("last_summary_date", today)
            .apply()
    }
}
