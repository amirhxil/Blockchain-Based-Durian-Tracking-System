package com.example.fyp_duriantrackingsystem.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp_duriantrackingsystem.databinding.ItemTransactionBinding
import com.example.fyp_duriantrackingsystem.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionsAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding =
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int = transactions.size

    class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            binding.fromTextView.text = transaction.from
            binding.toTextView.text = transaction.to
            binding.durianCodeTextView.text = transaction.durianId.toString()
            // Format timestamp to human-readable format
            val timestamp = transaction.timestamp
            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val formattedDate =
                dateFormat.format(Date(timestamp)) // Convert timestamp to Date object
            binding.timestampTextView.text = formattedDate
            binding.actionTextView.text =
                transaction.action.toString() // "Harvested", "Transferred", etc.

        }
    }
}
