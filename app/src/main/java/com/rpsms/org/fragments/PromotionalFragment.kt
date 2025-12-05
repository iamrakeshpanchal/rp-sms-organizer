package com.rpsms.org.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rpsms.org.adapters.SmsListAdapter
import com.rpsms.org.database.SmsDatabase
import com.rpsms.org.databinding.FragmentPromotionalBinding
import com.rpsms.org.viewmodels.SmsViewModel

class PromotionalFragment : Fragment() {
    
    private var _binding: FragmentPromotionalBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SmsViewModel
    private lateinit var adapter: SmsListAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromotionalBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupViewModel()
        setupAutoDeleteToggle()
    }
    
    private fun setupRecyclerView() {
        adapter = SmsListAdapter(
            onItemClick = { sms ->
                openSmsPreview(sms)
            },
            onCopyClick = { sms ->
                copyToClipboard(sms.body)
            },
            onDeleteClick = { sms ->
                deleteMessage(sms)
            },
            onReplyClick = { sms ->
                // Usually don't reply to promotional
                showUnsubscribeOption(sms)
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
            val promotionalMessages = messages.filter { sms ->
                sms.folder == "promotional" || isPromotional(sms.body)
            }
            
            if (promotionalMessages.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.emptyText.text = "No promotional messages"
                binding.summaryCard.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(promotionalMessages)
                
                // Show summary
                showDailySummary(promotionalMessages)
            }
        }
    }
    
    private fun isPromotional(message: String): Boolean {
        val promotionalKeywords = listOf(
            "offer", "sale", "discount", "promo", "deal", "win", "free",
            "cashback", "loan", "credit card", "insurance", "buy now",
            "shop now", "limited time", "exclusive", "discount code"
        )
        
        val lowerMessage = message.lowercase()
        return promotionalKeywords.any { keyword ->
            lowerMessage.contains(keyword)
        }
    }
    
    private fun showDailySummary(messages: List<com.rpsms.org.models.SmsMessageModel>) {
        val today = System.currentTimeMillis()
        val yesterday = today - (24 * 60 * 60 * 1000)
        
        val todayMessages = messages.filter { it.date >= yesterday }
        val totalToday = todayMessages.size
        
        val senders = messages.map { it.address }.distinct()
        
        binding.summaryCard.visibility = View.VISIBLE
        binding.summaryText.text = """
            Today's Promotional Messages: $totalToday
            Total Senders: ${senders.size}
            Last 7 days: ${messages.size}
            
            Tap for detailed report
        """.trimIndent()
        
        binding.summaryCard.setOnClickListener {
            showDetailedReport(messages)
        }
    }
    
    private fun showDetailedReport(messages: List<com.rpsms.org.models.SmsMessageModel>) {
        val senders = messages.groupBy { it.address }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
        
        val report = buildString {
            append("Promotional Messages Report\n")
            append("===========================\n\n")
            append("Total Messages: ${messages.size}\n")
            append("Unique Senders: ${senders.size}\n\n")
            append("Top Senders:\n")
            senders.take(5).forEach { (sender, count) ->
                append("• $sender: $count messages\n")
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Promotional Report")
            .setMessage(report)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun openSmsPreview(sms: com.rpsms.org.models.SmsMessageModel) {
        val intent = android.content.Intent(requireContext(), com.rpsms.org.SmsPreviewActivity::class.java).apply {
            putExtra("sms_id", sms.id)
            putExtra("sender", sms.address)
            putExtra("message", sms.body)
            putExtra("timestamp", sms.date)
            putExtra("is_promotional", true)
        }
        startActivity(intent)
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Promotional", text)
        clipboard.setPrimaryClip(clip)
    }
    
    private fun deleteMessage(sms: com.rpsms.org.models.SmsMessageModel) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Promotional")
            .setMessage("Delete this promotional message?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMessage(sms)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showUnsubscribeOption(sms: com.rpsms.org.models.SmsMessageModel) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Unsubscribe")
            .setMessage("Send STOP to unsubscribe from ${sms.address}?")
            .setPositiveButton("Send STOP") { _, _ ->
                sendUnsubscribeMessage(sms.address)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun sendUnsubscribeMessage(address: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:$address")
            putExtra("sms_body", "STOP")
        }
        startActivity(intent)
    }
    
    private fun setupAutoDeleteToggle() {
        binding.switchAutoDelete.setOnCheckedChangeListener { _, isChecked ->
            val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("auto_delete_promotional", isChecked).apply()
            
            if (isChecked) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Promotional messages will auto-delete after 24h",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        // Load saved setting
        val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        binding.switchAutoDelete.isChecked = prefs.getBoolean("auto_delete_promotional", false)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
