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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fyp_duriantrackingsystem.R
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.model.Durian
import com.example.fyp_duriantrackingsystem.model.DurianState
import com.example.fyp_duriantrackingsystem.service.EthereumService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.fyp_duriantrackingsystem.databinding.FragmentMySpoiledDuriansBinding

class MySpoiledDurians : Fragment() {

    private lateinit var binding: FragmentMySpoiledDuriansBinding
    private lateinit var ethereumService: EthereumService
    private lateinit var durianAdapter: DurianAdapter
    private var durianList: List<Durian> = listOf()  // Hold the full list of durians

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentMySpoiledDuriansBinding.inflate(inflater, container, false)
        // Set up RecyclerView


// Reference the markAsSpoiledButton here after inflating the layout


        binding.rvDurianOwnedList.layoutManager = LinearLayoutManager(requireContext())
        durianAdapter = DurianAdapter()
        binding.rvDurianOwnedList.adapter = durianAdapter

        // Set up the Spinner
        val spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_options,  // Reference the string array in res/values/strings.xml
            android.R.layout.simple_spinner_item
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = spinnerAdapter

        // Set the listener for the spinner
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> sortDuriansByDateAscending() // Latest to Oldest
                    1 -> sortDuriansByDateDescending()  // Oldest to Latest
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
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

        // Fetch durians owned by the account
        fetchOwnedDurians(account.toString().lowercase())

        return binding.root


    }

    private fun fetchOwnedDurians(user: String) {
        Thread {
            try {
                // Fetch durians owned by the account
                val ownedDurians = ethereumService.getUserDurians(user)
                activity?.runOnUiThread {

                    if (!ownedDurians.isNullOrEmpty()) {
                        durianList = ownedDurians.map {
                            val harvestDate = it.addedTimestamp // From the contract
                            val ownedTimestamp = it.timestamp  // From the contract
                            val ownedTimestampFormatted = if (ownedTimestamp.toLong() == 0L) {
                                "N/A"
                            } else {
                                val dateFormat =
                                    SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                                dateFormat.format(Date(ownedTimestamp.toLong() * 1000))  // Convert Unix timestamp to milliseconds
                            }
                            val stateInt = it.state
                            val state = DurianState.values()[stateInt.toInt()] // Map state

                            // Only show `spoiled` durians
                            if (state == DurianState.Spoiled) {
                                var arrivedAtDistributor =
                                    it.arriveAtDistributorTime.toString() // From the contract
                                var arrivedAtRetailer =
                                    it.arriveAtRetailerTime.toString() // From the contract
                                var distributorName = it.distributorName
                                var retailerName = it.retailerName
                                val dateFormat =
                                    SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                                val harvestDateFormatted =
                                    dateFormat.format(Date(harvestDate.toLong() * 1000))  // Convert Unix timestamp to milliseconds

                                // Ensure the state is an integer (0, 1, 2, etc.) and map it to DurianState enum
                                val stateInt =
                                    it.state // Assuming `state` is already an integer value
                                val state =
                                    DurianState.values()[stateInt.toInt()]  // Convert integer to DurianState enum

                                // Check if arrivedAtDistributor or arrivedAtRetailer are 0 and set to "N/A"
                                if (arrivedAtDistributor == "0") arrivedAtDistributor = "N/A"
                                if (arrivedAtRetailer == "0") arrivedAtRetailer = "N/A"
                                if (distributorName == "") distributorName = "N/A"
                                if (retailerName == "") retailerName = "N/A"
                                // Map contract data to your `Durian` model
                                Durian(
                                    batchCode = it.batchCode,
                                    durianType = it.durianType,
                                    weightInGram = it.weightInGram,
                                    currentOwner = it.currentOwner,
                                    state = state,
                                    farmLocation = it.farmLocation,
                                    addedTimestamp = harvestDateFormatted.toString(),  // Use timestamp or convert to readable format
                                    ownedTimestamp = ownedTimestampFormatted, // Include formatted timestamp
                                    distributorName = distributorName,
                                    arrivedAtDistributor = arrivedAtDistributor.toString(),  // Same for distributor
                                    retailerName = retailerName,
                                    arrivedAtRetailer = arrivedAtRetailer.toString()  // Same for retailer
                                )
                            } else {
                                null
                            }
                        }.filterNotNull() // Remove null entries (only include Spoiled ones)

                        // Initially, show the durians unsorted
                        durianAdapter.submitList(durianList)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No durians owned by this account.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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


    fun copyToClipboard(view: View, textToCopy: CharSequence) {
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", textToCopy)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // Sort the list of durians from latest to oldest
    private fun sortDuriansByDateDescending() {
        val sortedList = durianList.sortedByDescending { it.addedTimestamp }
        durianAdapter.submitList(sortedList)
    }

    // Sort the list of durians from oldest to latest
    private fun sortDuriansByDateAscending() {
        val sortedList = durianList.sortedBy { it.addedTimestamp }
        durianAdapter.submitList(sortedList)
    }

}

