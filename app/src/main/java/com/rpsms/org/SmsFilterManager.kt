package com.rpsms.org

import android.content.Context
import com.rpsms.org.database.SmsDatabase
import com.rpsms.org.models.FilterGroup
import com.rpsms.org.models.SmsMessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsFilterManager(private val context: Context) {
    
    private val database = SmsDatabase.getDatabase(context)
    
    fun createFilterGroup(
        name: String,
        keywords: List<String>,
        folderName: String,
        autoDelete: Boolean = false,
        deleteAfterHours: Int = 24
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val filter = FilterGroup(
                name = name,
                keywords = keywords.joinToString(","),
                folderName = folderName,
                color = getRandomColor(),
                autoDelete = autoDelete,
                deleteAfterHours = deleteAfterHours
            )
            database.filterDao().insert(filter)
            
            // Apply filter to existing messages
            applyFilterToExistingMessages(filter)
        }
    }
    
    fun applyFiltersToMessage(message: SmsMessageModel): SmsMessageModel {
        val filters = database.filterDao().getAllFilters()
        
        filters.forEach { filter ->
            val keywords = filter.keywords.split(",")
            val messageLower = message.body.lowercase()
            
            if (keywords.any { keyword -> 
                keyword.trim().isNotEmpty() && messageLower.contains(keyword.trim().lowercase())
            }) {
                return message.copy(folder = filter.folderName)
            }
        }
        
        return message
    }
    
    private fun applyFilterToExistingMessages(filter: FilterGroup) {
        CoroutineScope(Dispatchers.IO).launch {
            val keywords = filter.keywords.split(",").map { it.trim().lowercase() }
            val allMessages = database.smsDao().getAllMessages()
            
            allMessages.forEach { message ->
                val messageLower = message.body.lowercase()
                if (keywords.any { keyword -> 
                    keyword.isNotEmpty() && messageLower.contains(keyword)
                }) {
                    val updatedMessage = message.copy(folder = filter.folderName)
                    database.smsDao().update(updatedMessage)
                }
            }
        }
    }
    
    fun deleteFilterGroup(filterId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            database.filterDao().deleteById(filterId)
        }
    }
    
    fun updateFilterGroup(filter: FilterGroup) {
        CoroutineScope(Dispatchers.IO).launch {
            database.filterDao().update(filter)
            applyFilterToExistingMessages(filter)
        }
    }
    
    fun getMessagesByFolder(folderName: String): List<SmsMessageModel> {
        return database.smsDao().getMessagesByFolder(folderName)
    }
    
    fun autoDeleteOldMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val filters = database.filterDao().getAllFilters()
            val now = System.currentTimeMillis()
            
            filters.filter { it.autoDelete }.forEach { filter ->
                val cutoffTime = now - (filter.deleteAfterHours * 3600000L)
                val messages = getMessagesByFolder(filter.folderName)
                
                messages.filter { it.date < cutoffTime }.forEach { message ->
                    database.smsDao().delete(message)
                }
            }
        }
    }
    
    private fun getRandomColor(): Int {
        val colors = listOf(
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
            0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
            0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722
        )
        return colors.random().toInt()
    }
    
    fun isPromotionalMessage(message: SmsMessageModel): Boolean {
        val promotionalKeywords = listOf(
            "offer", "sale", "discount", "promo", "deal", "win", "free",
            "cashback", "loan", "credit card", "insurance", "investment",
            "subscription", "unsubscribe", "limited time", "shop now",
            "buy now", "click here", "apply now", "call now"
        )
        
        val messageLower = message.body.lowercase()
        return promotionalKeywords.any { keyword ->
            messageLower.contains(keyword)
        } || message.address.contains("DM", ignoreCase = true)
    }
    
    fun categorizeMessage(message: SmsMessageModel): String {
        val otpManager = OtpManager(context)
        
        return when {
            otpManager.isOTPMessage(message.body) -> "otp"
            isPromotionalMessage(message) -> "promotional"
            message.body.contains("bill", ignoreCase = true) -> "bills"
            message.body.contains("bank", ignoreCase = true) -> "banking"
            message.body.contains("travel", ignoreCase = true) -> "travel"
            else -> "personal"
        }
    }
}
