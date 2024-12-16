package com.example.fyp_duriantrackingsystem.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fyp_duriantrackingsystem.R
import com.example.fyp_duriantrackingsystem.core.MyApplication
import com.example.fyp_duriantrackingsystem.databinding.FragmentSetupProfileBinding
import com.example.fyp_duriantrackingsystem.service.EthereumService
import java.math.BigInteger

class SetupProfile : Fragment() {

    private lateinit var binding: FragmentSetupProfileBinding
    private lateinit var ethereumService: EthereumService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_setup_profile, container, false
        )
        var sharedPrefs = (requireActivity().applicationContext as MyApplication).sharedPrefs

        // Retrieve arguments passed from Login fragment
        val args = SetupProfileArgs.fromBundle(requireArguments())
        val accountNumber = args.accountNumber
        val rpcUrl = args.rpcUrl
        val privateKey = args.privateKey
        val contractAddress = args.contractAddress

        ethereumService = EthereumService(rpcUrl, contractAddress, privateKey, requireContext())
        // Define the roles array and create a custom adapter
        val rolesArray =
            resources.getStringArray(R.array.user_roles) // Ensure roles_array is defined in res/values/strings.xml
        val spinnerAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            rolesArray
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0 // Disable the first item (index 0)
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (position == 0) {
                    (view as TextView).setTextColor(Color.GRAY) // Change the color to indicate it is disabled
                }
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent)
                if (position == 0) {
                    (view as TextView).setTextColor(Color.GRAY) // Change the dropdown item color to indicate it is disabled
                }
                return view
            }
        }

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRole.adapter = spinnerAdapter
        // Set index 1 as the default selected item
        binding.spRole.setSelection(1)


        binding.btnSetUser.setOnClickListener {
            val name = binding.etName.text.toString()
            val companyName = binding.etCompanyName.text.toString()
            val companyAddress = binding.etCompanyAddress.text.toString()
            val role = BigInteger.valueOf(binding.spRole.selectedItemPosition.toLong())

            if (name.isNotEmpty() && companyName.isNotEmpty() && companyAddress.isNotEmpty()) {
                // Set the user profile in the blockchain
                Thread {
                    val transactionHash = ethereumService.setUser(
                        name,
                        accountNumber,
                        companyAddress,
                        companyName,
                        role,
                        requireContext()
                    )
                    activity?.runOnUiThread {
                        if (transactionHash != null) {
                            // Success, navigate to transaction fragment
                            Toast.makeText(
                                requireContext(),
                                "Profile set successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            sharedPrefs.saveAccountDetails(accountNumber, privateKey, role)
                            (activity as MainActivity).unlockDrawer()  // Calls unlockDrawer function
                            findNavController().navigate(R.id.action_setupProfileFragment_to_profile)
                        } else {
                            // Error setting user
                            Toast.makeText(
                                requireContext(),
                                "Error setting user profile",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.start()
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        return binding.root
    }
}
