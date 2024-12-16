package com.example.fyp_duriantrackingsystem.utils


import android.content.Context
import java.math.BigInteger

class SharedPrefsHelper(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    fun saveAccountDetails(account: String, privateKey: String, userRole: BigInteger) {
        val editor = sharedPreferences.edit()
        editor.putString("account", account)
        editor.putString("privateKey", privateKey) // Save private key
        editor.putString(
            "userRole",
            userRole.toString()
        ) // Save role as string (to convert back to BigInteger)
        editor.putBoolean("isLoggedIn", true) // Set the login state
        editor.apply()
    }


    fun getAccountDetails(): AccountDetails {
        val account = sharedPreferences.getString("account", null)
        val privateKey = sharedPreferences.getString("privateKey", null)
        val userRoleString = sharedPreferences.getString("userRole", null)
        val userRole = if (userRoleString != null) BigInteger(userRoleString) else null
        return AccountDetails(account, privateKey, userRole)
    }


    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    fun getUserRole(): BigInteger? {
        val roleString = sharedPreferences.getString("userRole", null)
        return roleString?.let { BigInteger(it) } // Convert back to BigInteger
    }

    fun saveUserRole(role: BigInteger) {
        val editor = sharedPreferences.edit()
        editor.remove("userRole")
        editor.apply()
        val editor2 = sharedPreferences.edit()
        editor2.putString("userRole", role.toString()) // Save BigInteger as String
        editor2.apply()
    }


    fun clearAccountDetails() {
        val editor = sharedPreferences.edit()
        editor.remove("account")
        editor.remove("privateKey")
        editor.remove("userRole")
        editor.remove("isLoggedIn")
        editor.apply()
    }


    data class AccountDetails(
        val account: String?,
        val privateKey: String?,
        val userRole: BigInteger?
    )

}
