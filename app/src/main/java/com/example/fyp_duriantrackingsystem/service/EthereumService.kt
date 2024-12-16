package com.example.fyp_duriantrackingsystem.service

import org.web3j.abi.TypeReference
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.fyp_duriantrackingsystem.model.Action
import com.example.fyp_duriantrackingsystem.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction as Web3jTransaction
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Convert
import java.math.BigInteger
import java.util.concurrent.CompletableFuture
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.abi.datatypes.Function


class EthereumService(
    private val rpcUrl: String,
    private val contractAddress: String,
    private val privateKey: String,
    private val context: Context
) {


    private val web3j: Web3j = Web3j.build(HttpService(rpcUrl))
    private val credentials: Credentials = Credentials.create(privateKey)
    private val gasPrice: BigInteger =
        Convert.toWei("20", Convert.Unit.GWEI).toBigInteger() // Step 1
    private val gasLimit: BigInteger = BigInteger.valueOf(6721975) // Step 1

    // Custom gas provider
    private val gasProvider: ContractGasProvider = StaticGasProvider(gasPrice, gasLimit) // Step 2

    private val contract: DurianContract = loadContract()
    private val FUNC_USERS = "users"

    fun isRpcUrlActive(): Boolean {
        return try {
            val web3 = Web3j.build(HttpService(rpcUrl))
            web3.web3ClientVersion().send().web3ClientVersion.isNotEmpty()
        } catch (e: Exception) {
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
            }
            Log.e("EthereumService", "RPC URL not active: ${e.message}")
            false
        }
    }

    fun isContractAddressValid(contractAddress: String): Boolean {
        return try {
            val web3 = Web3j.build(HttpService(rpcUrl))
            val code =
                web3.ethGetCode(contractAddress, DefaultBlockParameterName.LATEST).send().code
            code != "0x" // Contract exists if code is not "0x"
        } catch (e: Exception) {
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
            }
            Log.e("EthereumService", "Invalid contract address: ${e.message}")
            false
        }
    }

    // Initialize the contract instance
    private fun loadContract(): DurianContract {
        return DurianContract.load(contractAddress, web3j, credentials, gasProvider)
    }

    // Check if a user profile is set
    fun isProfileSet(userAddress: String): Boolean {
        return try {
            contract.isProfileSet(userAddress).send()
        } catch (e: Exception) {
            Log.e("EthereumService", "Error checking profile: ${e.message}")
            false
        }
    }

    // Set a user profile
    fun setUser(
        name: String,
        userAddress: String,
        companyAddress: String,
        companyName: String,
        role: BigInteger,
        context: Context
    ): String? {
        return try {
            val receipt = contract.setUser(
                name, userAddress, companyAddress, companyName, role
            ).send()
            receipt.transactionHash
        } catch (e: Exception) {
            Log.e("EthereumService", "Error setting user: ${e.message}")
            // Show a toast with the error message
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
            }
            null
        }
    }

    fun getUserDetails(userAddress: String): UserDetails {
        // Construct the data for the function call
        val encodedFunction = org.web3j.abi.FunctionEncoder.encode(
            org.web3j.abi.datatypes.Function(
                FUNC_USERS, listOf(Address(userAddress)), listOf(
                    TypeReference.create(Address::class.java),
                    TypeReference.create(Utf8String::class.java),
                    TypeReference.create(Utf8String::class.java),
                    TypeReference.create(Utf8String::class.java),
                    TypeReference.create(Uint8::class.java),
                    TypeReference.create(Uint8::class.java)
                )
            )
        )
        // Create the transaction for the call
        val transaction =
            Web3jTransaction.createEthCallTransaction(userAddress, contractAddress, encodedFunction)
        // Send the call and get the response
        val ethCall: EthCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send()
        // Decode the result
        val results = org.web3j.abi.FunctionReturnDecoder.decode(
            ethCall.value, Function(
                FUNC_USERS, listOf(Address(userAddress)), listOf(
                    TypeReference.create(Address::class.java),
                    TypeReference.create(Utf8String::class.java),
                    TypeReference.create(Utf8String::class.java),
                    TypeReference.create(Utf8String::class.java),
                    TypeReference.create(Uint8::class.java),
                    TypeReference.create(Uint8::class.java)
                )
            ).outputParameters
        )

        // Extract the decoded values
        val userAddressResult = results[0] as Address
        val name = results[1] as Utf8String
        val companyAddress = results[2] as Utf8String
        val companyName = results[3] as Utf8String
        val role = results[4] as Uint8
        val state = results[5] as Uint8

        // Returning the full user details in a data class
        return UserDetails(
            userAddress = userAddressResult.value,
            name = name.value,
            companyAddress = companyAddress.value,
            companyName = companyName.value,
            role = role.value.toInt(),
            state = state.value.toInt()
        )
    }

    fun getUserDurians(user: String): List<DurianContract.Durian>? {
        try {
            // Get all batch codes from the contract
            val batchCodes = getBatchCodes() ?: return null

            // Initialize list for durians belonging to the user
            val userDurians = mutableListOf<DurianContract.Durian>()

            for (batchCode in batchCodes) {
                // Fetch the durian for each batch code
                val durian = getDurian(batchCode)

                // Check if the currentOwner matches the provided user address
                if (durian?.currentOwner == user) {
                    userDurians.add(durian)
                }
            }

            return userDurians
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Function to retrieve all transactions related to a user
    // Function to get all transactions for a specific user

    // Record a harvested durian
    fun recordHarvestDurian(
        batchCode: BigInteger,
        durianType: String,
        farmLocation: String,
        weightInGram: BigInteger
    ): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            try {
                val receipt =
                    contract.recordHarvestDurian(batchCode, durianType, farmLocation, weightInGram)
                        .send()
                receipt.transactionHash // return transaction hash
            } catch (e: Exception) {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("EthereumService", "Error recording harvest: ${e.message}")
                null // return null on error
            }
        }
    }

    fun updateSpoiledState(batchCode: BigInteger): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            try {
                val receipt = contract.updateSpoiledState(batchCode).send()
                receipt.transactionHash // return transaction hash
            } catch (e: Exception) {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("EthereumService", "Error update spoiled durian: ${e.message}")
                null // return null on error
            }
        }
    }

    // Check if the batch code exists on the blockchain
    fun isBatchCodeExisted(batchCode: BigInteger): Boolean {
        return try {
            // Call the smart contract method to check if the batch code exists
            contract.isBatchCodeExisted(batchCode).send()

        } catch (e: Exception) {
            Log.e("EthereumService", "Error checking batch code existence: ${e.message}")
            false
        }
    }


    // Transfer durian ownership
    fun transferDurianOwnership(batchCode: BigInteger, newOwner: String): String? {
        return try {
            val receipt = contract.transferDurianOwnership(batchCode, newOwner).send()
            receipt.transactionHash
        } catch (e: Exception) {
            Log.e("EthereumService", "Error transferring ownership: ${e.stackTraceToString()}")
            null
        }
    }

    fun doesAddressExist(address: String): Boolean {
        return try {

            val response = web3j.ethGetCode(address, DefaultBlockParameterName.LATEST).send()
            val code = response.code.toString()

            // Debug logs
            Log.d(
                "EthereumService",
                "Address: $address, Code: $code, Error: ${response.error?.message}"
            )
            code.isNotEmpty()
        } catch (e: Exception) {

            false // If there's an error (e.g., invalid address), return false
        }
    }

    // Get durian details
    fun getDurian(batchCode: BigInteger): DurianContract.Durian? {
        return try {
            contract.getDurian(batchCode).send()
        } catch (e: Exception) {
            Log.e("EthereumService", "Error fetching durian: ${e.stackTraceToString()}")
            null
        }
    }
    // Fetch user transactions from contract
// Assuming you have a method to get the transactions or logs from the contract


    // Ensure that the network call is made on the IO dispatcher
    suspend fun getTransactions(): List<Transaction> {
        return withContext(Dispatchers.IO) {  // This ensures network calls are off the main thread
            try {
                // Fetch raw transaction data from the contract
                val transactionsResponse = contract.getTransactions().send()
                // Process each rawTransaction in the response
                transactionsResponse.map { rawTransaction ->
                    // Log the rawTransaction for debugging
                    Log.d("EthereumService", "Raw Transaction: $rawTransaction")
                    // Assuming rawTransaction.value is a list or map with fields
                    val value = rawTransaction.value as List<Any>
                    val from = (value[0] as Address).toString()  // Convert Address to String
                    val to = (value[1] as Address).toString()    // Convert Address to String
// Assuming value[2] is the Uint256 field (durianId)
                    val durianId = (value[2] as Uint256).value  // Access the BigInteger directly
                    val durianIdString = durianId.toString()  // If you need a string representation
                    val timestamp = (value[3] as Uint256).value
                    val timestampInMillis =
                        timestamp.toLong() * 1000  // Convert seconds to milliseconds
                    val actionValue = (value[4] as Uint8).value.toInt()  // Convert Uint8 to Int
                    // Map the action value to the Action enum
                    val action = Action.fromInt(actionValue)
                    // Return the Transaction object
                    Transaction(
                        from = from,
                        to = to,
                        durianId = durianIdString,
                        timestamp = timestampInMillis,
                        action = action
                    )
                }
            } catch (e: Exception) {
                Log.e("EthereumService", "Error fetching transactions: ${e.stackTraceToString()}")
                emptyList()
            }
        }
    }


    // Get all batch codes
    fun getBatchCodes(): List<BigInteger>? {
        return try {
            // Fetch the batch codes from the contract
            val batchCodesRaw = contract.getBatchCodes().send()
            // Convert raw list to a typed list of BigInteger
            batchCodesRaw.map { it as BigInteger }
        } catch (e: Exception) {
            Log.e("EthereumService", "Error fetching batch codes: ${e.message}")
            null
        }
    }


    // Generate tracking report

    // Generate a tracking report for a given batch code
    fun generateTrackingReport(batchCode: BigInteger): List<String> {
        return try {
            val response =
                contract.generateTrackingReport(BigInteger.valueOf(batchCode.toLong())).send()

            // Convert the response to a readable format
            val report = listOf(
                response.component1().toString(),     // Batch Code
                response.component2(),
                // Durian Type
                getDurianStateString(response.component3().toInt()), // Durian State
                formatTimestamp(response.component4().toLong()),  // Harvest Date
                response.component5(),                  // Farm Location
                response.component6().ifEmpty { "No distributor involved" }, // Distributor Name
                response.component7().ifEmpty { "No retailer involved" }  // Retailer Name
            )
            report
        } catch (e: Exception) {
            Log.e("EthereumService", "Error generating tracking report: ${e.message}")
            emptyList()
        }
    }


    // Helper function to format the timestamp (assuming it's a Unix timestamp)
    private fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp * 1000)
        return date.toString()
    }

    // Convert DurianState from contract to a readable format
    private fun getDurianStateString(state: Int): String {
        return when (state) {
            0 -> "Harvested"
            1 -> "At Distributor"
            2 -> "At Retailer"
            3 -> "Sold"
            4 -> "Spoiled"
            else -> "Unknown"
        }
    }
}

// Helper data class for tracking report
data class TrackingReport(
    val batchCode: BigInteger,
    val durianType: String,
    val state: BigInteger,
    val harvestDate: BigInteger,
    val farmLocation: String,
    val distributorName: String,
    val retailerName: String
)

data class UserDetails(
    val userAddress: String,
    val name: String,
    val companyAddress: String,
    val companyName: String,
    val role: Int,
    val state: Int
)

