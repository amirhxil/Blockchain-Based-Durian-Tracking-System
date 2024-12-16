package com.example.fyp_duriantrackingsystem.ui

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

class GenerateReport : Fragment() {

    private lateinit var ethereumService: EthereumService

    private lateinit var batchCodeInput: EditText
    private lateinit var reportContent: View
    private lateinit var reportBatchCode: TextView
    private lateinit var reportDurianType: TextView
    private lateinit var reportCurrentState: TextView
    private lateinit var reportHarvestDate: TextView
    private lateinit var reportFarmLocation: TextView
    private lateinit var reportDistributor: TextView
    private lateinit var reportRetailer: TextView
    private lateinit var printButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_generate_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        batchCodeInput = view.findViewById(R.id.batchCodeInput)
        reportContent = view.findViewById(R.id.reportContent)
        printButton = view.findViewById(R.id.exportPDFButton)

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

        printButton.setOnClickListener { printReport() }
    }

    private fun generateTrackingReport() {
        val batchCode = batchCodeInput.text.toString().trim()
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
                        printButton.visibility = View.VISIBLE
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

    private fun printReport() {
        val printManager = requireContext().getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                } else {
                    callback?.onLayoutFinished(
                        android.print.PrintDocumentInfo.Builder("Durian_Report")
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1).build(),
                        true
                    )
                }
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                destination?.let {
                    try {
                        val pdfDocument = android.graphics.pdf.PdfDocument()
                        val pageInfo =
                            android.graphics.pdf.PdfDocument.PageInfo.Builder(612, 792, 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        val canvas: Canvas = page.canvas
                        val paint = Paint().apply {
                            color = android.graphics.Color.BLACK
                        }
                        // Text for "Durian Tracking Report" in big size and centered
                        val titlePaint = TextPaint().apply {
                            textSize = 36f  // Large font size
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }
                        val title = "Durian Tracking Report"
                        canvas.drawText(
                            title,
                            306f,
                            100f,
                            titlePaint
                        )  // Centered horizontally at 306px (half of 612px width)
                        // Add space below the title
                        var currentY = 160f  // Increase space below the title
                        // Draw the report content with bigger text
                        val contentPaint = TextPaint().apply {
                            textSize = 20f  // Increased font size for content
                            isAntiAlias = true
                        }

                        // Helper function to wrap text to the next line if it exceeds the width
                        fun drawTextWithWrap(
                            text: String,
                            x: Float,
                            y: Float,
                            paint: TextPaint,
                            maxWidth: Float
                        ): Float {
                            val words = text.split(" ")
                            var line = ""
                            var nextLineY = y
                            for (word in words) {
                                val testLine = "$line $word".trim()
                                val testLineWidth = paint.measureText(testLine)
                                // If the line is too wide, draw the current line and start a new line with the current word
                                if (testLineWidth <= maxWidth) {
                                    line = testLine
                                } else {
                                    if (line.isNotEmpty()) {
                                        canvas.drawText(line, x, nextLineY, paint)
                                        nextLineY += 50f // Add spacing after the line
                                    }
                                    line = word  // Start new line with the current word
                                }
                            }
                            // Draw the last line
                            if (line.isNotEmpty()) {
                                canvas.drawText(line, x, nextLineY, paint)
                                nextLineY += 50f // Add spacing after the line
                            }
                            return nextLineY
                        }
                        // Batch Code
                        currentY = drawTextWithWrap(
                            "Batch Code: ${reportBatchCode.text}",
                            50f,
                            currentY,
                            contentPaint,
                            550f
                        )
                        // Durian Type
                        currentY = drawTextWithWrap(
                            "Type: ${reportDurianType.text}",
                            50f,
                            currentY,
                            contentPaint,
                            550f
                        )
                        // Current State
                        currentY = drawTextWithWrap(
                            "Current State: ${reportCurrentState.text}",
                            50f,
                            currentY,
                            contentPaint,
                            550f
                        )
                        // Harvest Date
                        currentY = drawTextWithWrap(
                            "Harvest Date: ${reportHarvestDate.text}",
                            50f,
                            currentY,
                            contentPaint,
                            550f
                        )
                        // Farm Location (wrap text if too long)
                        currentY = drawTextWithWrap(
                            "Farm Location: ${reportFarmLocation.text}",
                            50f,
                            currentY,
                            contentPaint,
                            550f
                        )
                        // Distributor
                        currentY = drawTextWithWrap(
                            "Distributor: ${reportDistributor.text}",
                            50f,
                            currentY,
                            contentPaint,
                            550f
                        )
                        // Retailer
                        currentY = drawTextWithWrap(
                            "Retailer: ${reportRetailer.text}",
                            50f,
                            currentY,
                            contentPaint,
                            550f
                        )
                        pdfDocument.finishPage(page)
                        // Write the PDF content to the destination
                        FileOutputStream(destination.fileDescriptor).use { pdfDocument.writeTo(it) }
                        pdfDocument.close()
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
        }
        printManager.print(
            "Durian Report Print Job",
            printAdapter,
            PrintAttributes.Builder().build()
        )
    }


    private fun launchCoroutine(block: suspend () -> Unit) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) { block() }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
