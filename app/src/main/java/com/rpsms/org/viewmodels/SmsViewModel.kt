package com.rpsms.org.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rpsms.org.database.SmsDao
import com.rpsms.org.models.SmsMessageModel
import kotlinx.coroutines.launch

class SmsViewModel(private val smsDao: SmsDao) : ViewModel() {
    
    val allMessages: LiveData<List<SmsMessageModel>> = smsDao.getAllMessages().asLiveData()
    
    fun getMessagesByFolder(folder: String): LiveData<List<SmsMessageModel>> {
        return smsDao.getMessagesByFolder(folder).asLiveData()
    }
    
    fun getOTPMessages(): LiveData<List<SmsMessageModel>> {
        return smsDao.getOTPMessages().asLiveData()
    }
    
    fun searchMessages(query: String): LiveData<List<SmsMessageModel>> {
        return smsDao.searchMessages(query).asLiveData()
    }
    
    fun insertMessage(message: SmsMessageModel) = viewModelScope.launch {
        smsDao.insert(message)
    }
    
    fun updateMessage(message: SmsMessageModel) = viewModelScope.launch {
        smsDao.update(message)
    }
    
    fun deleteMessage(message: SmsMessageModel) = viewModelScope.launch {
        smsDao.delete(message)
    }
    
    fun markAsRead(messageId: Long) = viewModelScope.launch {
        // This would require a separate query to update read status
        // For now, we'll fetch and update
        // In real implementation, you'd add a method to DAO
    }
    
    fun deleteOldMessages(olderThanTimestamp: Long) = viewModelScope.launch {
        smsDao.deleteOldMessages(olderThanTimestamp)
    }
    
    fun getUnreadCount(): LiveData<Int> {
        return smsDao.getUnreadCount().asLiveData()
    }
    
    fun getConversation(threadId: Long): LiveData<List<SmsMessageModel>> {
        return smsDao.getConversation(threadId).asLiveData()
    }
    
    @Suppress("UNCHECKED_CAST")
    class Factory(private val smsDao: SmsDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SmsViewModel::class.java)) {
                return SmsViewModel(smsDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
