package com.rpsms.org.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "sms_messages")
data class SmsMessageModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "address")
    val address: String,
    
    @ColumnInfo(name = "body")
    val body: String,
    
    @ColumnInfo(name = "date")
    val date: Long,
    
    @ColumnInfo(name = "type")
    val type: Int, // 1 = received, 2 = sent
    
    @ColumnInfo(name = "read")
    val read: Boolean = false,
    
    @ColumnInfo(name = "thread_id")
    val threadId: Long,
    
    @ColumnInfo(name = "folder")
    val folder: String = "inbox",
    
    @ColumnInfo(name = "is_otp")
    val isOTP: Boolean = false,
    
    @ColumnInfo(name = "otp_code")
    val otpCode: String = "",
    
    @ColumnInfo(name = "is_promotional")
    val isPromotional: Boolean = false,
    
    @ColumnInfo(name = "saved")
    val saved: Boolean = false,
    
    @ColumnInfo(name = "contact_name")
    val contactName: String? = null
)
