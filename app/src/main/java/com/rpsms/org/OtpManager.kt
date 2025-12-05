package com.rpsms.org

import android.content.Context
import android.util.Log
import java.util.regex.Pattern

class OtpManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OtpManager"
        private val OTP_PATTERNS = listOf(
            Pattern.compile("\\b\\d{4}\\b"),
            Pattern.compile("\\b\\d{5}\\b"),
            Pattern.compile("\\b\\d{6}\\b"),
            Pattern.compile("OTP[\\s:]*[0-9]{4,6}"),
            Pattern.compile("code[\\s:]*[0-9]{4,6}"),
            Pattern.compile("[0-9]{4,6}[\\s]*is your OTP")
        )
        
        private val OTP_KEYWORDS = listOf(
            "OTP", "One Time Password", "verification code", 
            "auth code", "security code", "login code"
        )
    }
    
    fun extractOTP(message: String): String {
        Log.d(TAG, "Extracting OTP from: ${message.take(50)}...")
        
        // Try patterns first
        for (pattern in OTP_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val found = matcher.group()
                // Extract only digits
                val digits = found.filter { it.isDigit() }
                if (digits.length in 4..6) {
                    Log.d(TAG, "OTP found via pattern: $digits")
                    return digits
                }
            }
        }
        
        // If no pattern match, look for standalone 4-6 digit numbers
        val words = message.split("\\s+".toRegex())
        for (word in words) {
            val cleanWord = word.filter { it.isDigit() }
            if (cleanWord.length in 4..6 && cleanWord.all { it.isDigit() }) {
                // Check if it's near OTP keywords
                val lowerMessage = message.lowercase()
                val wordIndex = lowerMessage.indexOf(word.lowercase())
                if (wordIndex != -1) {
                    val surroundingText = lowerMessage.substring(
                        maxOf(0, wordIndex - 30),
                        minOf(lowerMessage.length, wordIndex + 30)
                    )
                    if (OTP_KEYWORDS.any { keyword -> 
                        surroundingText.contains(keyword.lowercase()) 
                    }) {
                        Log.d(TAG, "OTP found near keyword: $cleanWord")
                        return cleanWord
                    }
                }
            }
        }
        
        Log.d(TAG, "No OTP found")
        return ""
    }
    
    fun isOTPMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Check for OTP keywords
        val hasKeyword = OTP_KEYWORDS.any { keyword ->
            lowerMessage.contains(keyword.lowercase())
        }
        
        if (!hasKeyword) return false
        
        // Check for OTP pattern
        return extractOTP(message).isNotEmpty()
    }
    
    fun saveOTPToClipboard(otp: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) 
                as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("OTP", otp)
            clipboard.setPrimaryClip(clip)
            
            // Show toast
            android.widget.Toast.makeText(
                context, 
                "OTP copied: $otp", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            Log.d(TAG, "OTP copied to clipboard: $otp")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy OTP", e)
        }
    }
    
    fun autoDeleteOTPAfterDelay(smsId: Long, delayHours: Int = 24) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        
        val deleteRequest = androidx.work.OneTimeWorkRequestBuilder<DeleteOTPWorker>()
            .setInitialDelay(delayHours.toLong(), java.util.concurrent.TimeUnit.HOURS)
            .setInputData(
                androidx.work.Data.Builder()
                    .putLong("sms_id", smsId)
                    .build()
            )
            .addTag("otp_deletion")
            .build()
        
        workManager.enqueue(deleteRequest)
        Log.d(TAG, "Scheduled OTP deletion for SMS ID: $smsId after $delayHours hours")
    }
    
    fun cancelOTPDeletion(smsId: Long) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("otp_deletion_$smsId")
        Log.d(TAG, "Cancelled OTP deletion for SMS ID: $smsId")
    }
}

class DeleteOTPWorker(
    context: Context,
    workerParams: androidx.work.WorkerParameters
) : androidx.work.Worker(context, workerParams) {
    
    override fun doWork(): Result {
        val smsId = inputData.getLong("sms_id", 0L)
        
        return try {
            if (smsId > 0) {
                val database = SmsDatabase.getDatabase(applicationContext)
                database.smsDao().deleteById(smsId)
                Log.d("DeleteOTPWorker", "Deleted OTP SMS with ID: $smsId")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("DeleteOTPWorker", "Error deleting OTP SMS", e)
            Result.failure()
        }
    }
}
