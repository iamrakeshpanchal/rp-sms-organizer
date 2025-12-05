package com.rpsms.org

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rpsms.org.database.SmsDatabase
import com.rpsms.org.models.SmsMessageModel
import java.util.Date
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
        private const val CHANNEL_ID = "sms_channel"
        private val OTP_PATTERN = Pattern.compile("\\b\\d{4,6}\\b")
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received")
        
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>?
                if (pdus != null) {
                    val messages = arrayOfNulls<SmsMessage>(pdus.size)
                    val sb = StringBuilder()
                    var sender = ""
                    
                    for (i in pdus.indices) {
                        messages[i] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            SmsMessage.createFromPdu(pdus[i] as ByteArray, bundle.getString("format"))
                        } else {
                            SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        }
                        sb.append(messages[i]?.messageBody)
                        if (sender.isEmpty()) {
                            sender = messages[i]?.originatingAddress ?: ""
                        }
                    }
                    
                    val messageBody = sb.toString()
                    val smsModel = SmsMessageModel(
                        id = 0,
                        address = sender,
                        body = messageBody,
                        date = Date().time,
                        type = 1, // Incoming
                        read = false,
                        threadId = getThreadId(context, sender)
                    )
                    
                    // Save to database
                    saveSmsToDatabase(context, smsModel)
                    
                    // Check for OTP
                    val otp = extractOTP(messageBody)
                    if (otp.isNotEmpty()) {
                        handleOTP(context, smsModel, otp)
                    }
                    
                    // Check for promotional
                    if (isPromotional(messageBody, sender)) {
                        handlePromotional(context, smsModel)
                    } else {
                        // Show bubble notification for non-promotional
                        showNotification(context, smsModel, otp)
                    }
                    
                    // Auto-delete OTP after 24 hours (schedule)
                    if (otp.isNotEmpty()) {
                        scheduleOTPDeletion(context, smsModel.id)
                    }
                }
            }
        }
    }
    
    private fun saveSmsToDatabase(context: Context, sms: SmsMessageModel) {
        try {
            val database = SmsDatabase.getDatabase(context)
            database.smsDao().insert(sms)
            Log.d(TAG, "SMS saved to database")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving SMS to database", e)
        }
    }
    
    private fun extractOTP(message: String): String {
        val matcher = OTP_PATTERN.matcher(message)
        return if (matcher.find()) {
            matcher.group()
        } else {
            ""
        }
    }
    
    private fun isPromotional(message: String, sender: String): Boolean {
        val promotionalKeywords = listOf(
            "offer", "sale", "discount", "promo", "win", "free", "cashback",
            "loan", "credit", "insurance", "subscription", "unsubscribe"
        )
        
        val messageLower = message.lowercase()
        return promotionalKeywords.any { keyword ->
            messageLower.contains(keyword) || sender.contains("DM", ignoreCase = true)
        }
    }
    
    private fun handleOTP(context: Context, sms: SmsMessageModel, otp: String) {
        // Save OTP to shared preferences for quick access
        val prefs = context.getSharedPreferences("otp_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("latest_otp", otp).apply()
        
        // Copy to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("OTP", otp)
        clipboard.setPrimaryClip(clip)
        
        // Show OTP notification
        showOTPNotification(context, sms.address, otp)
    }
    
    private fun handlePromotional(context: Context, sms: SmsMessageModel) {
        // Move to promotional folder (update in database)
        val database = SmsDatabase.getDatabase(context)
        sms.folder = "promotional"
        database.smsDao().update(sms)
        
        // Don't show bubble, just log
        Log.d(TAG, "Promotional SMS moved to folder: ${sms.address}")
    }
    
    private fun showNotification(context: Context, sms: SmsMessageModel, otp: String = "") {
        val notificationId = System.currentTimeMillis().toInt()
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_message)
            .setContentTitle(sms.address)
            .setContentText(sms.body.take(50))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        
        if (otp.isNotEmpty()) {
            builder.setContentText("OTP: $otp")
        }
        
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
        
        // Show bubble if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBubbleNotification(context, sms)
        }
    }
    
    private fun showOTPNotification(context: Context, sender: String, otp: String) {
        val notificationId = 1001
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle("OTP Received from $sender")
            .setContentText("OTP: $otp (Copied to clipboard)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
    
    private fun showBubbleNotification(context: Context, sms: SmsMessageModel) {
        // Implement bubble notification for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(context, BubbleActivity::class.java).apply {
                putExtra("sender", sms.address)
                putExtra("message", sms.body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            }
            
            val bubbleIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.app.PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                )
            } else {
                android.app.PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            
            // Create bubble metadata
            val bubbleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                NotificationCompat.BubbleMetadata.Builder(bubbleIntent)
                    .setDesiredHeight(600)
                    .build()
            } else {
                null
            }
            
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(sms.address)
                .setContentText(sms.body.take(30))
                .setBubbleMetadata(bubbleData)
                .setAutoCancel(true)
            
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }
    
    private fun getThreadId(context: Context, address: String): Long {
        return address.hashCode().toLong()
    }
    
    private fun scheduleOTPDeletion(context: Context, smsId: Long) {
        // Schedule deletion after 24 hours using WorkManager
        val deleteRequest = androidx.work.OneTimeWorkRequestBuilder<DeleteOTPWorker>()
            .setInitialDelay(24, java.util.concurrent.TimeUnit.HOURS)
            .setInputData(androidx.work.Data.Builder()
                .putLong("sms_id", smsId)
                .build())
            .build()
        
        androidx.work.WorkManager.getInstance(context).enqueue(deleteRequest)
    }
}
