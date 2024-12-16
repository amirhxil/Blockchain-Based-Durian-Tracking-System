package com.example.fyp_duriantrackingsystem.ui

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.fyp_duriantrackingsystem.R
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.databinding.FragmentLoginBinding
import com.example.fyp_duriantrackingsystem.service.EthereumService
import org.web3j.crypto.Credentials
import java.math.BigInteger

class Login : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var ethereumService: EthereumService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        val sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs

        // Get the fixed values from MyApplication
        val rpcUrl = (requireActivity().applicationContext as MyApplication).defaultRpcUrl
        val contractAddress =
            (requireActivity().applicationContext as MyApplication).defaultContractAddress

        var isPasswordVisible = false

        binding.ivTogglePrivateKeyVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                binding.etPrivateKey.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePrivateKeyVisibility.setImageResource(R.drawable.ic_visibility)
            } else {
                binding.etPrivateKey.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePrivateKeyVisibility.setImageResource(R.drawable.ic_visibility_off)
            }

            // Set the cursor to the end of the text
            binding.etPrivateKey.setSelection(binding.etPrivateKey.text?.length ?: 0)
        }

        binding.btnLogin.setOnClickListener {
            val accountNumber = binding.etAccountNumber.text.toString()
            val privateKey = binding.etPrivateKey.text.toString()

            if (accountNumber.isNotEmpty() && privateKey.isNotEmpty()) {
                // Initialize EthereumService with fixed RPC URL and contract address
                ethereumService =
                    EthereumService(rpcUrl, contractAddress, privateKey, requireContext())

                // Run checks in a background thread
                Thread {
                    try {
                        // Check if RPC URL is active
                        val isRpcActive = ethereumService.isRpcUrlActive()
                        if (!isRpcActive) {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Blockchain RPC Server is not active",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return@Thread
                        }

                        // Check if contract address exists
                        val isContractValid =
                            ethereumService.isContractAddressValid(contractAddress)
                        if (!isContractValid) {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Invalid contract address on the blockchain",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@Thread
                        }

                        // Validate private key and proceed with account verification
                        val credentials = Credentials.create(privateKey)
                        val userAddressFromPrivateKey = credentials.address

                        if (userAddressFromPrivateKey.equals(accountNumber, ignoreCase = true)) {
                            val isRegistered =
                                ethereumService.isProfileSet(userAddressFromPrivateKey)
                            activity?.runOnUiThread {
                                if (isRegistered) {
                                    sharedPrefs.saveAccountDetails(
                                        accountNumber,
                                        privateKey,
                                        BigInteger("0")
                                    )
                                    (activity as MainActivity).unlockDrawer()
                                    findNavController().navigate(R.id.action_login_to_profile)
                                } else {
                                    val action = LoginDirections.actionLoginToSetupProfileFragment(
                                        accountNumber = accountNumber,
                                        rpcUrl = rpcUrl,
                                        privateKey = privateKey,
                                        contractAddress = contractAddress
                                    )
                                    findNavController().navigate(action)
                                }
                            }
                        } else {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Invalid private key for the given account",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Login", "Error during login: ${e.message}")
                        activity?.runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.start()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Account number and private key are required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return binding.root
    }
}
