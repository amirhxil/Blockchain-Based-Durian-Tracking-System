package com.example.fyp_duriantrackingsystem.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fyp_duriantrackingsystem.R
import com.example.fyp_duriantrackingsystem.adapter.TransactionsAdapter
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.databinding.FragmentUserTransactionBinding
import com.example.fyp_duriantrackingsystem.model.Transaction
import com.example.fyp_duriantrackingsystem.service.EthereumService
import kotlinx.coroutines.launch

class UserTransaction : Fragment(R.layout.fragment_user_transaction) {

    private lateinit var binding: FragmentUserTransactionBinding
    private lateinit var ethereumService: EthereumService
    private var userTransactions: List<Transaction> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewBinding
        binding = FragmentUserTransactionBinding.bind(view)
        binding.transactionsRecyclerView.adapter = TransactionsAdapter(emptyList())

        // Set up EthereumService
        val sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs
        val rpcUrl = (requireActivity().applicationContext as MyApplication).defaultRpcUrl
        val contractAddress =
            (requireActivity().applicationContext as MyApplication).defaultContractAddress
        val privateKey = sharedPrefs.getAccountDetails().privateKey

        ethereumService = EthereumService(
            rpcUrl.toString(),
            contractAddress.toString(),
            privateKey.toString(),
            requireContext()
        )

        // Set up RecyclerView
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set up Spinner for sorting transactions
        val spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_options, // Reference the string array in res/values/strings.xml
            android.R.layout.simple_spinner_item
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = spinnerAdapter

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> sortTransactionsByDateAscending() // Latest to Oldest
                    1 -> sortTransactionsByDateDescending()  // Oldest to Latest

                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Load transactions
        loadTransactions()
    }

    private fun loadTransactions() {
        val sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs
        val userAccount = sharedPrefs.getAccountDetails().account

        lifecycleScope.launch {
            try {
                // Fetch transactions from Ethereum service
                val transactions = ethereumService.getTransactions()

                // Filter transactions related to the user
                userTransactions = transactions.filter { transaction ->
                    transaction.from.equals(userAccount, ignoreCase = true) ||
                            transaction.to.equals(userAccount, ignoreCase = true)
                }

                // Display transactions in the RecyclerView
                binding.transactionsRecyclerView.adapter = TransactionsAdapter(userTransactions)
            } catch (e: Exception) {
                Log.e("UserTransaction", "Error loading transactions: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to load transactions.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun sortTransactionsByDateDescending() {
        val sortedList = userTransactions.sortedByDescending { it.timestamp }
        binding.transactionsRecyclerView.adapter = TransactionsAdapter(sortedList)
        Toast.makeText(requireContext(), "Latest on top", Toast.LENGTH_SHORT).show()

    }

    private fun sortTransactionsByDateAscending() {
        val sortedList = userTransactions.sortedBy { it.timestamp }
        binding.transactionsRecyclerView.adapter = TransactionsAdapter(sortedList)
        Toast.makeText(requireContext(), "Oldest on top", Toast.LENGTH_SHORT).show()

    }
}
