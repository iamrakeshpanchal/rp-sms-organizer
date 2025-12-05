package com.rpsms.org.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rpsms.org.databinding.SmsItemBinding
import com.rpsms.org.models.SmsMessageModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsListAdapter(
    private val onItemClick: (SmsMessageModel) -> Unit,
    private val onCopyClick: (SmsMessageModel) -> Unit,
    private val onDeleteClick: (SmsMessageModel) -> Unit,
    private val onReplyClick: (SmsMessageModel) -> Unit
) : RecyclerView.Adapter<SmsListAdapter.SmsViewHolder>() {
    
    private var messages: List<SmsMessageModel> = emptyList()
    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    inner class SmsViewHolder(val binding: SmsItemBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(messages[position])
                }
            }
            
            binding.btnCopy.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCopyClick(messages[position])
                }
            }
            
            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(messages[position])
                }
            }
            
            binding.btnReply.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onReplyClick(messages[position])
                }
            }
        }
        
        fun bind(message: SmsMessageModel) {
            binding.txtSender.text = message.contactName ?: message.address
            binding.txtMessagePreview.text = message.body
            
            val date = Date(message.date)
            binding.txtTime.text = dateFormat.format(date)
            
            // Show OTP indicator if it's an OTP message
            if (message.isOTP) {
                binding.txtOtpIndicator.visibility = android.view.View.VISIBLE
                binding.btnCopyOtp.visibility = android.view.View.VISIBLE
                binding.btnCopy.visibility = android.view.View.GONE
            } else {
                binding.txtOtpIndicator.visibility = android.view.View.GONE
                binding.btnCopyOtp.visibility = android.view.View.GONE
                binding.btnCopy.visibility = android.view.View.VISIBLE
            }
            
            // Show WhatsApp icon if contact is in phone book
            // This would require checking contacts database
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val binding = SmsItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SmsViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    fun submitList(newMessages: List<SmsMessageModel>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
