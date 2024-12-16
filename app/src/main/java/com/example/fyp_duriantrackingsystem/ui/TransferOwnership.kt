package com.example.fyp_duriantrackingsystem.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.databinding.FragmentTransferOwnershipBinding
import com.example.fyp_duriantrackingsystem.service.EthereumService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class TransferOwnership : Fragment() {
    private lateinit var binding: FragmentTransferOwnershipBinding
    private lateinit var ethereumService: EthereumService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransferOwnershipBinding.inflate(inflater, container, false)

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
        val batchCodeText = binding.batchCodeInput
        val newOwner = binding.newOwnerInput
        binding.transferOwnershipButton.setOnClickListener {
            // Create the AlertDialog
            if (batchCodeText.text.isBlank() || newOwner.text.isBlank()) {
                Toast.makeText(requireContext(), "All fields are required.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Confirmation")
                builder.setMessage("${batchCodeText.text.toString()} will be send to: \n\n${newOwner.text.toString()}")

                // Set up the "Confirm" button
                builder.setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss() // Dismiss the dialog
                    transferOwnership() // Call the function to perform the action
                }

                // Set up the "Cancel" button
                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss() // Simply dismiss the dialog
                }

                // Show the dialog
                builder.create().show()
            }
        }
        return binding.root
    }

    private fun transferOwnership() {
        val sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs
        val (account, privateKey, userRole) = sharedPrefs.getAccountDetails()


        val batchCodeText = binding.batchCodeInput.text.toString().trim()
        val newOwner = binding.newOwnerInput.text.toString().trim()

        if (batchCodeText.isEmpty() || newOwner.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val batchCode = try {
            batchCodeText.toBigInteger()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid batch code format.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Perform network operations on a background thread using Coroutine
        GlobalScope.launch(Dispatchers.Main) {
            try {
                // Check if the new owner's address exists


                // Proceed with the rest of the transfer logic
                val durian = withContext(Dispatchers.IO) { ethereumService.getDurian(batchCode) }

                if (durian == null) {
                    binding.transferStatus.text = "Durian not found."
                    Toast.makeText(requireContext(), "Durian not found.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Verify ownership
                if (durian.currentOwner.toString().lowercase() != account.toString().lowercase()) {
                    binding.transferStatus.text = "You do not own this durian."
                    Toast.makeText(
                        requireContext(),
                        "You do not own this durian.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                val doesExist = withContext(Dispatchers.IO) {
                    ethereumService.doesAddressExist(newOwner)
                }

                if (!doesExist) {
                    binding.transferStatus.text = "The new owner's address does not exist."
                    Toast.makeText(
                        requireContext(),
                        "The new owner's address does not exist.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                // Prevent transferring to self
                if (durian.currentOwner.toString().lowercase() == newOwner.lowercase()) {
                    binding.transferStatus.text = "Cannot transfer ownership to your own account."
                    Toast.makeText(
                        requireContext(),
                        "Cannot transfer ownership to your own account.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Proceed with the transfer
                val result = withContext(Dispatchers.IO) {
                    ethereumService.transferDurianOwnership(batchCode, newOwner)
                }

                if (result?.contains("Error") == true || result == null) {
                    binding.transferStatus.text = result
                    Toast.makeText(requireContext(), "Transfer failed: $result", Toast.LENGTH_LONG)
                        .show()
                } else {
                    binding.transferStatus.text = result
                    Toast.makeText(
                        requireContext(),
                        "Transfer successful: $result",
                        Toast.LENGTH_LONG
                    ).show()
                    reloadFragment() // Reload the fragment after success or failure
                }

            } catch (e: Exception) {
                binding.transferStatus.text = "Unexpected error: ${e.message}"
                Toast.makeText(
                    requireContext(),
                    "Unexpected error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun reloadFragment() {
        findNavController().navigate(com.example.fyp_duriantrackingsystem.R.id.action_transferOwnership_self)

    }
}
