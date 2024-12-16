package com.example.fyp_duriantrackingsystem.core

import android.app.Application
import com.example.fyp_duriantrackingsystem.utils.SharedPrefsHelper

class MyApplication : Application() {

    lateinit var sharedPrefs: SharedPrefsHelper

    // Static values for Contract Address and RPC URL
    val defaultRpcUrl: String = "HTTP://10.0.2.2:7545" //to be changed accordingly
    val defaultContractAddress: String =
        "0x977c4F705BC62Fc949404f44170fb0A98f06c7FD" //to be changed accordingly

    override fun onCreate() {
        super.onCreate()
        sharedPrefs = SharedPrefsHelper(applicationContext)
    }
}
