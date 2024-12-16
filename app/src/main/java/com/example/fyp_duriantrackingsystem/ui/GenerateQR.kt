package com.example.fyp_duriantrackingsystem.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.databinding.FragmentGenerateQrBinding
import com.example.fyp_duriantrackingsystem.service.EthereumService
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.encoder.QRCode
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.math.BigInteger

class GenerateQR : Fragment() {

    private lateinit var binding: FragmentGenerateQrBinding
    private lateinit var ethereumService: EthereumService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGenerateQrBinding.inflate(inflater, container, false)

        // Get shared preferences for account and other details
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

        binding.generateQRCodeButton.setOnClickListener {
            val batchCode = binding.batchCodeInput.text.toString().trim()
            if (TextUtils.isEmpty(batchCode)) {
                Toast.makeText(
                    requireContext(),
                    "Please enter a valid batch code.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Check if the user is the owner before generating QR code
            checkOwnershipAndGenerateQRCode(batchCode)
        }
        binding.printQRCodeButton.setOnClickListener {
            printQRCode()
        }

        return binding.root
    }

    private fun checkOwnershipAndGenerateQRCode(batchCode: String) {
        val sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs
        val (account, _, _) = sharedPrefs.getAccountDetails()

        // Convert batchCode to BigInteger
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

        // Check ownership using a background task (Coroutine)
        GlobalScope.launch(Dispatchers.Main) {
            try {
                // Retrieve Durian object for the batchCode
                val durian =
                    withContext(Dispatchers.IO) { ethereumService.getDurian(batchCodeBigInt) }

                if (durian == null) {
                    Toast.makeText(
                        requireContext(),
                        "Durian with this batch code does not exist.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Check if the current owner is the same as the account from shared preferences
                if (durian.currentOwner.toString().lowercase() != account.toString().lowercase()) {
                    Toast.makeText(
                        requireContext(),
                        "You are not the owner of this batch.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Proceed to generate the QR code if the user is the owner
                generateQRCode(batchCode)

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error checking ownership or retrieving data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun generateQRCode(batchCode: String) {
        try {
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix: BitMatrix =
                multiFormatWriter.encode(batchCode, BarcodeFormat.QR_CODE, 400, 400)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
            binding.qrCodeCanvas.setImageBitmap(bitmap)
            binding.printQRCodeButton.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error generating QR Code: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun generateQRCodeBitmap(batchCode: String): Bitmap? {
        try {
            val size = 512 // Size of the QR Code
            val bitMatrix: BitMatrix =
                MultiFormatWriter().encode(batchCode, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun printQRCode() {
        try {
            val printManager =
                requireContext().getSystemService(Context.PRINT_SERVICE) as PrintManager
            // Create a custom PrintDocumentAdapter
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    // Notify the print framework about layout
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                    } else {
                        callback?.onLayoutFinished(
                            android.print.PrintDocumentInfo.Builder("QR_Code_Print")
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
                                android.graphics.pdf.PdfDocument.PageInfo.Builder(400, 500, 1)
                                    .create()
                            val page = pdfDocument.startPage(pageInfo)
                            // Retrieve the batch code entered by the user
                            val batchCode = binding.batchCodeInput.text.toString().trim()
                            // Generate the QR code bitmap
                            val qrCodeBitmap: Bitmap? =
                                generateQRCodeBitmap(batchCode)  // Generate QR with batch code
                            // Check if qrCodeBitmap is not null and draw it onto the PDF canvas
                            qrCodeBitmap?.let { bitmap ->
                                val canvas: Canvas = page.canvas
                                canvas.drawBitmap(
                                    bitmap, null, android.graphics.Rect(50, 50, 350, 350), null
                                )
                                // Draw the batch code text below the QR code
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    textSize = 20f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                val textY = 400f  // Position for batch code text below the QR code
                                canvas.drawText(
                                    batchCode,
                                    200f,
                                    textY,
                                    paint
                                )  // Batch code centered
                            }
                            pdfDocument.finishPage(page)
                            // Write the PDF content to the output stream
                            FileOutputStream(destination.fileDescriptor).use { outputStream ->
                                pdfDocument.writeTo(
                                    outputStream
                                )
                            }
                            pdfDocument.close()
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }

                // Function to generate a QR code bitmap (for demonstration purposes)

            }

            // Start the print job
            printManager.print("QR Code Print Job", printAdapter, PrintAttributes.Builder().build())

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error printing QR Code: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


}
