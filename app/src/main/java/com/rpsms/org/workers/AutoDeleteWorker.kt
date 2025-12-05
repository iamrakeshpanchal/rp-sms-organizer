package com.rpsms.org.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rpsms.org.database.SmsDatabase

class AutoDeleteWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    
    override fun doWork(): Result {
        return try {
            val database = SmsDatabase.getDatabase(applicationContext)
            val prefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            
            // Get auto delete settings
            val autoDeleteDays = prefs.getInt("auto_delete_days", 0)
            val autoDeleteOTP = prefs.getBoolean("auto_delete_otp", true)
            val autoDeletePromo = prefs.getBoolean("auto_delete_promo", false)
            
            val now = System.currentTimeMillis()
            
            // Delete old messages based on settings
            if (autoDeleteDays > 0) {
                val cutoffTime = now - (autoDeleteDays * 24 * 60 * 60 * 1000L)
                database.smsDao().deleteOldMessages(cutoffTime)
            }
            
            // Auto delete OTP messages after 24 hours
            if (autoDeleteOTP) {
                val otpCutoffTime = now - (24 * 60 * 60 * 1000L)
                val otpMessages = database.smsDao().getOTPMessages().value
                otpMessages?.forEach { message ->
                    if (message.date < otpCutoffTime) {
                        database.smsDao().delete(message)
                    }
                }
            }
            
            // Auto delete promotional messages after 24 hours
            if (autoDeletePromo) {
                val promoCutoffTime = now - (24 * 60 * 60 * 1000L)
                val promoMessages = database.smsDao().getMessagesByFolder("promotional").value
                promoMessages?.forEach { message ->
                    if (message.date < promoCutoffTime) {
                        database.smsDao().delete(message)
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
