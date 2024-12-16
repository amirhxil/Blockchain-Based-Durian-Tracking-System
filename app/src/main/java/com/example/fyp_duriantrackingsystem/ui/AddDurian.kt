package com.example.fyp_duriantrackingsystem.ui

import android.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.databinding.FragmentAddDurianBinding
import com.example.fyp_duriantrackingsystem.service.EthereumService
import java.math.BigInteger

class AddDurian : Fragment() {
    private var _binding: FragmentAddDurianBinding? = null
    private val binding get() = _binding!!

    private val durianTypes = listOf(
        "Black Pearl", "Black Thorn", "D101", "D13", "D24",
        "Golden Phoenix", "IOI", "Kampung", "Lao Tai Po",
        "Musang King", "Red Prawn", "Tekka", "Tupai King", "XO"
    )
    private lateinit var ethereumService: EthereumService


    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAddDurianBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs

        super.onViewCreated(view, savedInstanceState)
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

        // Set up the spinner with durian types
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item,
            durianTypes
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.durianType.adapter = spinnerAdapter

        // Generate Batch Code when the fragment is loaded
        generateBatchCode()

        // Set up click listener for the Add Durian button
        binding.addDurianButton.setOnClickListener {
            addDurian()
        }
    }

    private fun generateBatchCode() {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        // Repeatedly try generating a unique batch code
        fun attemptGenerateUniqueBatchCode() {
            val randomDigits = (10000000..99999999).random()
            val batchCode = "$currentYear$randomDigits"
            val batchCodeBigInt = BigInteger(batchCode)

            Thread {
                try {
                    val exists = ethereumService.isBatchCodeExisted(batchCodeBigInt)
                    activity?.runOnUiThread {
                        if (exists) {
                            // If batch code exists, try generating another
                            attemptGenerateUniqueBatchCode()
                        } else {
                            // If unique, set it to the field
                            binding.batchCode.setText(batchCode)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EthereumService", "Error checking batch code existence: ${e.message}")
                }
            }.start()
        }

        attemptGenerateUniqueBatchCode()
    }

    private fun addDurian() {
        val batchCode = binding.batchCode.text.toString()
        val durianType = binding.durianType.selectedItem.toString()
        val weightInGram = binding.weightInGram.text.toString()
        val farmLocation = binding.farmLocation.text.toString()

        // Validation checks
        if (batchCode.isEmpty()) {
            Toast.makeText(requireContext(), "Batch Code is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (durianType.isEmpty()) {
            Toast.makeText(requireContext(), "Durian Type is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (weightInGram.isEmpty()) {
            Toast.makeText(requireContext(), "Weight in Gram is required", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (farmLocation.isEmpty()) {
            Toast.makeText(requireContext(), "Farm Location is required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Convert batch code and weight to BigInteger
            val batchCodeBigInt = BigInteger(batchCode)
            val weightInGramBigInt = BigInteger(weightInGram)

            // Check if the batch code already exists on the blockchain
            val isBatchCodeExisted = ethereumService.isBatchCodeExisted(batchCodeBigInt)

            if (isBatchCodeExisted) {
                activity?.runOnUiThread {
                    // If batch code exists, show error message
                    binding.statusMessage.text = "Batch code already exists."
                    Toast.makeText(
                        requireContext(),
                        "Batch code already exists, reloading page......",
                        Toast.LENGTH_LONG
                    ).show()
                    reloadFragment() // Reload the fragment after failure
                }
            } else {
                // If batch code doesn't exist, proceed to record the harvest
                ethereumService.recordHarvestDurian(
                    batchCodeBigInt,
                    durianType,
                    farmLocation,
                    weightInGramBigInt
                )
                    .thenAccept { transactionHash ->
                        activity?.runOnUiThread {
                            if (transactionHash != null) {
                                binding.statusMessage.text =
                                    "Durian added successfully! Tx Hash: $transactionHash"
                                Toast.makeText(
                                    requireContext(),
                                    "Durian added successfully! Tx Hash: $transactionHash",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                binding.statusMessage.text = "Failed to add durian."
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to add durian.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            reloadFragment() // Reload the fragment after success or failure
                        }
                    }
                    .exceptionally { e ->
                        activity?.runOnUiThread {
                            Log.e("AddDurianFragment", "Error adding durian: ${e.message}")
                            Toast.makeText(
                                requireContext(),
                                "Error adding durian: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            binding.statusMessage.text = "Error adding durian: ${e.message}"
                        }
                        null // return null in case of an exception
                    }
            }
        } catch (e: Exception) {
            activity?.runOnUiThread {
                Log.e("AddDurianFragment", "Error adding durian: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Error adding durian: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.statusMessage.text = "Error adding durian: ${e.message}"
            }
        }
    }


    // Method to reload the fragment
    private fun reloadFragment() {
        findNavController().navigate(com.example.fyp_duriantrackingsystem.R.id.action_addDurian_self)

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

