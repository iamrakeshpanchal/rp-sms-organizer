package com.rpsms.org

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.rpsms.org.adapters.ViewPagerAdapter
import com.rpsms.org.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    
    // Permission request codes
    private val PERMISSION_REQUEST_CODE = 100
    private val DEFAULT_SMS_REQUEST_CODE = 101
    
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkAndRequestPermissions()
        checkDefaultSmsApp()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        
        // Setup ViewPager with tabs
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> "All"
                1 -> "OTP"
                2 -> "Promo"
                3 -> "Groups"
                else -> "Tab $position"
            }
        }.attach()
        
        // Setup Navigation Drawer
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.nav_all_messages -> binding.viewPager.currentItem = 0
                R.id.nav_otp -> binding.viewPager.currentItem = 1
                R.id.nav_promotional -> binding.viewPager.currentItem = 2
                R.id.nav_groups -> {
                    // Open groups fragment
                    Toast.makeText(this, "Groups Management", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_filters -> {
                    // Open filters activity
                    val intent = Intent(this, FilterSettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_backup_restore -> {
                    // Open backup activity
                    val intent = Intent(this, BackupActivity::class.java)
                    startActivity(intent)
                }
            }
            binding.drawerLayout.closeDrawers()
            true
        }
        
        // Setup toolbar navigation icon
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }
        
        // Setup FAB for new SMS
        binding.fabNewSms.setOnClickListener {
            composeNewSMS()
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun checkDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this)
            if (defaultSmsApp != packageName) {
                requestDefaultSmsApp()
            }
        }
    }
    
    private fun requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivityForResult(intent, DEFAULT_SMS_REQUEST_CODE)
        }
    }
    
    private fun composeNewSMS() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:")
        }
        startActivity(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            DEFAULT_SMS_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Now default SMS app", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
