package com.rpsms.org

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.rpsms.org.databinding.BubbleLayoutBinding

class BubbleActivity : Activity() {
    
    private lateinit var binding: BubbleLayoutBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make activity look like a bubble
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
        }
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        // Make window floating
        window.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        binding = BubbleLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBubble()
        setupClickListeners()
    }
    
    private fun setupBubble() {
        val sender = intent.getStringExtra("sender") ?: "Unknown"
        val message = intent.getStringExtra("message") ?: ""
        
        binding.txtBubbleSender.text = sender
        binding.txtBubbleMessage.text = message.take(50) + if (message.length > 50) "..." else ""
        
        // Show actions after a delay
        binding.root.postDelayed({
            binding.bubbleActions.visibility = android.view.View.VISIBLE
        }, 1000)
    }
    
    private fun setupClickListeners() {
        binding.btnBubbleClose.setOnClickListener {
            finish()
        }
        
        binding.btnBubbleOpen.setOnClickListener {
            // Open main app
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_sms_preview", true)
                putExtra("sender", intent.getStringExtra("sender"))
                putExtra("message", intent.getStringExtra("message"))
            }
            startActivity(mainIntent)
            finish()
        }
        
        binding.btnBubbleDismiss.setOnClickListener {
            finish()
        }
        
        // Make entire bubble draggable
        binding.bubbleContainer.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_MOVE -> {
                    val layoutParams = v.layoutParams as WindowManager.LayoutParams
                    layoutParams.x = event.rawX.toInt() - v.width / 2
                    layoutParams.y = event.rawY.toInt() - v.height / 2
                    windowManager.updateViewLayout(v, layoutParams)
                }
            }
            true
        }
    }
    
    override fun onBackPressed() {
        // Prevent back button from closing bubble immediately
        finish()
    }
}
