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
import com.rpsms.org.databinding.FragmentAllMessagesBinding
import com.rpsms.org.viewmodels.SmsViewModel
import android.content.Intent
import android.net.Uri

class AllMessagesFragment : Fragment() {
    
    private var _binding: FragmentAllMessagesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SmsViewModel
    private lateinit var adapter: SmsListAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
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
                // Copy message to clipboard
                copyToClipboard(sms.body)
            },
            onDeleteClick = { sms ->
                // Delete message
                deleteMessage(sms)
            },
            onReplyClick = { sms ->
                // Reply to message
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
            if (messages.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(messages)
            }
        }
    }
    
    private fun openSmsPreview(sms: com.rpsms.org.models.SmsMessageModel) {
        // Open full screen preview activity
        val intent = Intent(requireContext(), com.rpsms.org.SmsPreviewActivity::class.java).apply {
            putExtra("sms_id", sms.id)
            putExtra("sender", sms.address)
            putExtra("message", sms.body)
            putExtra("timestamp", sms.date)
        }
        startActivity(intent)
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("SMS", text)
        clipboard.setPrimaryClip(clip)
        
        android.widget.Toast.makeText(
            requireContext(),
            "Message copied",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun deleteMessage(sms: com.rpsms.org.models.SmsMessageModel) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMessage(sms)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Message deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun replyToMessage(sms: com.rpsms.org.models.SmsMessageModel) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${sms.address}")
            putExtra("sms_body", "")
        }
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
