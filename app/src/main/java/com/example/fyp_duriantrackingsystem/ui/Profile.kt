package com.example.fyp_duriantrackingsystem.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fyp_duriantrackingsystem.R
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.databinding.FragmentProfileBinding
import com.example.fyp_duriantrackingsystem.service.EthereumService
import com.example.fyp_duriantrackingsystem.service.UserDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class Profile : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var ethereumService: EthereumService


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        var sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs
        val rpcUrl = (requireActivity().applicationContext as MyApplication).defaultRpcUrl
        val contractAddress =
            (requireActivity().applicationContext as MyApplication).defaultContractAddress
        // Access contract address from MyApplication

        // Get account details from SharedPrefs
        val (account, privateKey, userRole) = sharedPrefs.getAccountDetails()

        // Ensure rpcUrl and account are valid
        if (account != null) {
            // Initialize EthereumService
            ethereumService = EthereumService(
                rpcUrl.toString(),
                contractAddress.toString(),
                privateKey.toString(),
                requireContext()
            )

            // Fetch user details (for example, use account address to fetch details)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userDetails = ethereumService.getUserDetails(account)

                    // Update UI on main thread with user details
                    withContext(Dispatchers.Main) {
                        updateUI(userDetails)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle error if needed (e.g., show error message to the user)
                }
            }
        }

        return binding.root
    }

    private fun updateUI(userDetails: UserDetails) {
        var sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs

        // Check if the user's state is deactivated
        if (userDetails.state == 1) {
            // Redirect to login page
            CoroutineScope(Dispatchers.Main).launch {
                (activity as MainActivity).lockDrawer() // Optional: Lock the drawer if applicable
                Toast.makeText(
                    requireContext(),
                    "Account is deactivated. Redirecting to login...",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().navigate(R.id.action_profile_to_login)
            }
            return // Stop further updates
        }

        // Update UI with user details
        binding.tvUserAddress.text = userDetails.userAddress
        binding.tvUserName.text = userDetails.name
        binding.tvCompanyName.text = userDetails.companyName
        binding.tvCompanyAddress.text = userDetails.companyAddress
        binding.tvRole.text = getRoleString(userDetails.role)
        binding.tvState.text = getStateString(userDetails.state)
        sharedPrefs.saveUserRole(BigInteger.valueOf(userDetails.role.toLong()))
    }


    private fun getRoleString(role: Int): String {
        return when (role) {
            0 -> "Unassigned"
            1 -> "Farmer"
            2 -> "Distributor"
            3 -> "Retailer"
            4 -> "Consumer"
            else -> "Unknown"
        }
    }

    private fun getStateString(state: Int): String {
        return when (state) {
            0 -> "Active"
            1 -> "Deactivated"
            else -> "Unknown"
        }
    }
}
