package com.rpsms.org

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rpsms.org.databinding.SmsPreviewBinding
import com.rpsms.org.OtpManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsPreviewActivity : AppCompatActivity() {
    
    private lateinit var binding: SmsPreviewBinding
    private lateinit var otpManager: OtpManager
    
    private var smsId: Long = 0
    private var sender: String = ""
    private var message: String = ""
    private var timestamp: Long = 0
    private var isOTP: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SmsPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        otpManager = OtpManager(this)
        
        extractIntentData()
        setupUI()
        setupClickListeners()
    }
    
    private fun extractIntentData() {
        smsId = intent.getLongExtra("sms_id", 0L)
        sender = intent.getStringExtra("sender") ?: "Unknown"
        message = intent.getStringExtra("message") ?: ""
        timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
        isOTP = intent.getBooleanExtra("is_otp", false)
    }
    
    private fun setupUI() {
        // Set sender name
        binding.txtSenderName.text = sender
        
        // Format and set time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        binding.txtMessageTime.text = dateFormat.format(Date(timestamp))
        
        // Set message content
        binding.txtFullMessage.text = message
        
        // Check for OTP
        if (isOTP) {
            val otp = otpManager.extractOTP(message)
            if (otp.isNotEmpty()) {
                showOTPSection(otp)
            }
        } else {
            // Check if message contains OTP anyway
            val otp = otpManager.extractOTP(message)
            if (otp.isNotEmpty()) {
                showOTPSection(otp)
            }
        }
    }
    
    private fun showOTPSection(otp: String) {
        binding.layoutOtp.visibility = android.view.View.VISIBLE
        binding.txtOtpNumber.text = otp
        binding.btnCopyOtpFull.visibility = android.view.View.VISIBLE
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnCopyMessage.setOnClickListener {
            copyToClipboard(message)
            Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnCopyOtpFull.setOnClickListener {
            val otp = otpManager.extractOTP(message)
            if (otp.isNotEmpty()) {
                otpManager.saveOTPToClipboard(otp)
            }
        }
        
        binding.btnSaveMessage.setOnClickListener {
            saveMessage()
            Toast.makeText(this, "Message saved", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnDeleteMessage.setOnClickListener {
            showDeleteConfirmation()
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("SMS", text)
        clipboard.setPrimaryClip(clip)
    }
    
    private fun saveMessage() {
        // Implement message saving logic
        // This would typically update the message in database with saved flag
        Toast.makeText(this, "Message saved to favorites", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMessage()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteMessage() {
        // Implement message deletion logic
        // This would typically delete from database
        Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
    }
}
