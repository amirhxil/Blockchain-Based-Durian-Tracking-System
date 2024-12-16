package com.example.fyp_duriantrackingsystem.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fyp_duriantrackingsystem.R
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.service.EthereumService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Locale

class MarkAsSpoiled : Fragment() {

    private lateinit var ethereumService: EthereumService

    private lateinit var batchCodeInputs: EditText
    private lateinit var reportContent: View
    private lateinit var reportBatchCode: TextView
    private lateinit var reportDurianType: TextView
    private lateinit var reportCurrentState: TextView
    private lateinit var reportHarvestDate: TextView
    private lateinit var reportFarmLocation: TextView
    private lateinit var reportDistributor: TextView
    private lateinit var reportRetailer: TextView
    private lateinit var markAsSpoiled: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mark_as_spoiled, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        batchCodeInputs = view.findViewById(R.id.batchCodeInput)
        reportContent = view.findViewById(R.id.reportContent)
        markAsSpoiled = view.findViewById(R.id.mark)

        reportBatchCode = view.findViewById(R.id.reportBatchCode)
        reportDurianType = view.findViewById(R.id.reportDurianType)
        reportCurrentState = view.findViewById(R.id.reportCurrentState)
        reportHarvestDate = view.findViewById(R.id.reportHarvestDate)
        reportFarmLocation = view.findViewById(R.id.reportFarmLocation)
        reportDistributor = view.findViewById(R.id.reportDistributor)
        reportRetailer = view.findViewById(R.id.reportRetailer)

        val generateButton: Button = view.findViewById(R.id.generateReportButton)
        generateButton.setOnClickListener { generateTrackingReport() }

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
        markAsSpoiled.setOnClickListener {
            // Create the AlertDialog
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirmation")
            builder.setMessage(batchCodeInputs.text.toString() + " is SPOILED?")

            // Set up the "Confirm" button
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog
                markAsSpoiled() // Call the function to perform the action
            }

            // Set up the "Cancel" button
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Simply dismiss the dialog
            }

            // Show the dialog
            builder.create().show()
        }

    }

    private fun generateTrackingReport() {
        val batchCode = batchCodeInputs.text.toString().trim()

        if (batchCode.isEmpty()) {
            showToast("Please enter a batch code.")
            return
        }

        val sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs
        val (account, _, _) = sharedPrefs.getAccountDetails()

        val batchCodeBigInt: BigInteger? = try {
            batchCode.toBigInteger()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid batch code format.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (batchCodeBigInt == null) {
            Toast.makeText(requireContext(), "Invalid batch code format.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        launchCoroutine {
            try {
                val durian =
                    withContext(Dispatchers.IO) { ethereumService.getDurian(batchCodeBigInt) }

                if (durian == null) {
                    Toast.makeText(
                        requireContext(),
                        "Durian with this batch code does not exist.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launchCoroutine
                }

                if (durian.currentOwner.toString().lowercase() != account.toString().lowercase()) {
                    Toast.makeText(
                        requireContext(),
                        "You are not the owner of this batch.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launchCoroutine
                }

                val report = withContext(Dispatchers.IO) {
                    ethereumService.generateTrackingReport(batchCodeBigInt)
                }

                if (report.isEmpty()) {
                    showToast("This batch code does not exist.")
                } else {
                    withContext(Dispatchers.Main) {
                        reportBatchCode.text = report[0]
                        reportDurianType.text = report[1]
                        reportCurrentState.text = report[2]
                        val inputDate = report[3]
                        val inputFormat =
                            SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
                        val outputFormat =
                            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

                        reportHarvestDate.text = try {
                            outputFormat.format(inputFormat.parse(inputDate)!!)
                        } catch (e: Exception) {
                            "Invalid Date"
                        }
                        reportFarmLocation.text = report[4]
                        reportDistributor.text = report[5]
                        reportRetailer.text = report[6]

                        reportContent.visibility = View.VISIBLE
                        markAsSpoiled.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("GenerateReportFragment", "Error generating tracking report", e)
                withContext(Dispatchers.Main) {
                    showToast("Error generating tracking report.")
                }
            }
        }
    }

    private fun markAsSpoiled() {
        val batchCodeStr = batchCodeInputs.text.toString().trim()


        val batchCodeBigInt = try {
            BigInteger(batchCodeStr)
        } catch (e: NumberFormatException) {
            showToast("Invalid batch code format.")
            return
        }

        launchCoroutine {
            try {


                if (reportCurrentState.text == "Spoiled") {
                    showToast("This batch is already marked as spoiled.")
                    return@launchCoroutine
                }

                // If not already spoiled, proceed to mark it
                val transactionHash = withContext(Dispatchers.IO) {
                    ethereumService.updateSpoiledState(batchCodeBigInt)
                }

                if (transactionHash != null) {
                    showToast("Batch successfully marked as spoiled.")
                    findNavController().navigate(com.example.fyp_duriantrackingsystem.R.id.action_markAsSpoiled_self)

                } else {
                    showToast("Failed to mark batch as spoiled.")
                }
            } catch (e: Exception) {
                Log.e("MarkAsSpoiled", "Error marking as spoiled", e)
                showToast("Error processing request.")
            }
        }
    }


    private fun launchCoroutine(block: suspend () -> Unit) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) { block() }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
