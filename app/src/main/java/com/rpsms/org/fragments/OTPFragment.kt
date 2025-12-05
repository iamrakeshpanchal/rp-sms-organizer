package com.rpsms.org.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rpsms.org.OtpManager
import com.rpsms.org.adapters.SmsListAdapter
import com.rpsms.org.database.SmsDatabase
import com.rpsms.org.databinding.FragmentOtpBinding
import com.rpsms.org.viewmodels.SmsViewModel

class OTPFragment : Fragment() {
    
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SmsViewModel
    private lateinit var adapter: SmsListAdapter
    private lateinit var otpManager: OtpManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        otpManager = OtpManager(requireContext())
        setupRecyclerView()
        setupViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = SmsListAdapter(
            onItemClick = { sms ->
                // Open SMS preview
                openSmsPreview(sms)
            },
            onCopyClick = { sms ->
                // Copy OTP from message
                val otp = otpManager.extractOTP(sms.body)
                if (otp.isNotEmpty()) {
                    otpManager.saveOTPToClipboard(otp)
                } else {
                    copyToClipboard(sms.body)
                }
            },
            onDeleteClick = { sms ->
                // Delete message
                deleteMessage(sms)
            },
            onReplyClick = { sms ->
                // Reply to message (usually not needed for OTP)
                replyToMessage(sms)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupViewModel() {
        val database = SmsDatabase.getDatabase(requireContext())
        val factory = SmsViewModel.Factory(database.smsDao())
        viewModel = ViewModelProvider(this, factory).get(SmsViewModel::class.java)
        
        viewModel.allMessages.observe(viewLifecycleOwner) { messages ->
            val otpMessages = messages.filter { sms ->
                otpManager.isOTPMessage(sms.body)
            }
            
            if (otpMessages.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.emptyText.text = "No OTP messages found"
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(otpMessages)
                
                // Show OTP statistics
                binding.otpStats.text = "Found ${otpMessages.size} OTP messages"
                binding.otpStats.visibility = View.VISIBLE
            }
        }
    }
    
    private fun openSmsPreview(sms: com.rpsms.org.models.SmsMessageModel) {
        val intent = Intent(requireContext(), com.rpsms.org.SmsPreviewActivity::class.java).apply {
            putExtra("sms_id", sms.id)
            putExtra("sender", sms.address)
            putExtra("message", sms.body)
            putExtra("timestamp", sms.date)
            putExtra("is_otp", true)
        }
        startActivity(intent)
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("OTP", text)
        clipboard.setPrimaryClip(clip)
        
        android.widget.Toast.makeText(
            requireContext(),
            "OTP copied",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun deleteMessage(sms: com.rpsms.org.models.SmsMessageModel) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete OTP")
            .setMessage("Delete this OTP message?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMessage(sms)
                android.widget.Toast.makeText(
                    requireContext(),
                    "OTP deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun replyToMessage(sms: com.rpsms.org.models.SmsMessageModel) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:${sms.address}")
            putExtra("sms_body", "")
        }
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
