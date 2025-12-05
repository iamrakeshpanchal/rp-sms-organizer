package com.rpsms.org.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_groups")
data class FilterGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    
    val keywords: String, // Comma separated keywords
    
    val folderName: String,
    
    val color: Int,
    
    val autoDelete: Boolean = false,
    
    val deleteAfterHours: Int = 24,
    
    val notificationEnabled: Boolean = true,
    
    val createdDate: Long = System.currentTimeMillis()
)
