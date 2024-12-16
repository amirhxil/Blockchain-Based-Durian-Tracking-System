package com.example.fyp_duriantrackingsystem.ui

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.NavigationUI
import com.example.fyp_duriantrackingsystem.databinding.ActivityMainBinding
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.fyp_duriantrackingsystem.R
import com.example.fyp_duriantrackingsystem.core.MyApplication
import java.math.BigInteger

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install SplashScreen before calling super.onCreate
        super.onCreate(savedInstanceState)
        // Handle SplashScreen compatibility

        var sharedPrefs = (applicationContext as MyApplication).sharedPrefs

        window.statusBarColor = Color.BLACK
        // Setup ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPrefsHelper

        // Setup DrawerLayout and NavigationView
        drawerLayout = binding.drawerLayout
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""  // Clear default title

        // Set up DrawerToggle (Hamburger Button)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.open, R.string.close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Initialize NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup NavigationView (side menu)
        val navigationView = binding.navigationView
        NavigationUI.setupWithNavController(navigationView, navController)

        // Check if the user is logged in
        if (sharedPrefs.isLoggedIn()) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            navController.navigate(R.id.profile)
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            navController.navigate(R.id.login)
        }

        // Handle Navigation Item Clicks
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    sharedPrefs.clearAccountDetails()
                    navController.navigate(R.id.login)
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_my_profile -> {
                    navController.navigate(R.id.profile)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_durians_list -> {
                    navController.navigate(R.id.durianList)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_durians_owned -> {
                    navController.navigate(R.id.durianOwned)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_transfer_ownership -> {
                    navController.navigate(R.id.transferOwnership)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_generate_report -> {
                    navController.navigate(R.id.generateReport)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_mark_as_spoiled -> {
                    navController.navigate(R.id.markAsSpoiled)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_my_spoiled_durian -> {
                    navController.navigate(R.id.mySpoiledDurians)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_generate_qr -> {
                    navController.navigate(R.id.generateQR)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_own_transactions -> {
                    navController.navigate(R.id.userTransaction)
                    drawerLayout.closeDrawers()
                }

                R.id.nav_add_durian -> {
                    val userRole =
                        sharedPrefs.getUserRole() // Assume getUserRole returns the user's role as BigInteger
                    if (userRole == BigInteger("1")) { // Check if userRole is 1 (Farmer)
                        navController.navigate(R.id.addDurian)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Authorized Farmers only.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    drawerLayout.closeDrawers()
                }

                else -> {
                    NavigationUI.onNavDestinationSelected(item, navController)
                    drawerLayout.closeDrawers()
                }
            }
            true
        }
    }

    // Unlock the drawer when the user logs in successfully
    fun unlockDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    fun lockDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout)
    }
}
