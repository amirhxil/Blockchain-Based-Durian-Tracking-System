package com.example.fyp_duriantrackingsystem.ui

import DurianAdapter
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fyp_duriantrackingsystem.R
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.databinding.FragmentDurianListBinding
import com.example.fyp_duriantrackingsystem.model.Durian
import com.example.fyp_duriantrackingsystem.model.DurianState
import com.example.fyp_duriantrackingsystem.service.EthereumService
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DurianList : Fragment() {

    private lateinit var binding: FragmentDurianListBinding
    private lateinit var ethereumService: EthereumService
    private lateinit var durianAdapter: DurianAdapter
    private var durianList: List<Durian> = listOf()  // Hold the full list of durians

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDurianListBinding.inflate(inflater, container, false)
        // Initialize the adapter
        durianAdapter = DurianAdapter()

        // Set up RecyclerView
        binding.rvDurianList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDurianList.adapter = durianAdapter

        // Set up the spinner for sorting
        val spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_options, // Reference the string array in res/values/strings.xml
            android.R.layout.simple_spinner_item
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = spinnerAdapter

        // Handle sorting selection
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> sortDuriansByDateAscending() // Latest to Oldest
                    1 -> sortDuriansByDateDescending()  // Oldest to Latest
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Initialize Ethereum service
        val sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs
        val rpcUrl = (requireActivity().applicationContext as MyApplication).defaultRpcUrl
        val contractAddress =
            (requireActivity().applicationContext as MyApplication).defaultContractAddress
        val (account, privateKey, userRole) = sharedPrefs.getAccountDetails()
        ethereumService = EthereumService(
            rpcUrl.toString(),
            contractAddress.toString(),
            privateKey.toString(),
            requireContext()
        )

        // Fetch durians from Ethereum
        fetchAllDurians()

        return binding.root
    }

    private fun fetchAllDurians() {
        Thread {
            try {
                val batchCodes = ethereumService.getBatchCodes()
                if (batchCodes.isNullOrEmpty()) {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "No durians found on the blockchain.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@Thread
                }

                val allDurians = batchCodes.mapNotNull { batchCode ->
                    ethereumService.getDurian(batchCode)?.let {
                        val harvestDate = it.addedTimestamp // From the contract
                        val ownedTimestamp = it.timestamp  // From the contract
                        val distributorName =
                            it.distributorName.takeIf { name -> name.isNotBlank() } ?: "N/A"
                        val retailerName =
                            it.retailerName.takeIf { name -> name.isNotBlank() } ?: "N/A"
                        val dateFormat =
                            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

                        var arrivedAtDistributor =
                            it.arriveAtDistributorTime.toString() // From the contract
                        var arrivedAtRetailer =
                            it.arriveAtRetailerTime.toString() // From the contract
                        if (arrivedAtDistributor == "0") arrivedAtDistributor = "N/A"
                        if (arrivedAtRetailer == "0") arrivedAtRetailer = "N/A"
                        Durian(
                            batchCode = it.batchCode,
                            durianType = it.durianType,
                            weightInGram = it.weightInGram,
                            currentOwner = it.currentOwner,
                            state = DurianState.values()[it.state.toInt()],
                            farmLocation = it.farmLocation,
                            addedTimestamp = dateFormat.format(Date(harvestDate.toLong() * 1000)),
                            ownedTimestamp = dateFormat.format(Date(ownedTimestamp.toLong() * 1000)),
                            distributorName = distributorName,
                            arrivedAtDistributor = arrivedAtDistributor.toString(),  // Same for distributor
                            retailerName = retailerName,
                            arrivedAtRetailer = arrivedAtRetailer.toString()  // Same for retailer
                        )
                    }
                }

                activity?.runOnUiThread {
                    durianList = allDurians
                    durianAdapter.submitList(durianList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching durians: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun sortDuriansByDateDescending() {
        val sortedList = durianList.sortedByDescending { it.addedTimestamp }
        durianAdapter.submitList(sortedList)
    }

    private fun sortDuriansByDateAscending() {
        val sortedList = durianList.sortedBy { it.addedTimestamp }
        durianAdapter.submitList(sortedList)
    }

    fun copyToClipboard(view: View, textToCopy: CharSequence) {
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
