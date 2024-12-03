package com.example.chargercharts2

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.chargercharts2.databinding.ActivityMainBinding
import com.example.chargercharts2.ui.dashboard.DashboardViewModel
import com.example.chargercharts2.ui.home.HomeViewModel
import com.example.chargercharts2.utils.isLandscape

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Check the current destination
            Log.i("MainActivity", "Destination: $destination")
        }

        adjustNavBarVisibility()

        homeViewModel.isStarted.observe(this) { isStarted ->
            if(getCurrentNavigation() == R.id.navigation_home) {
                Log.i("MainActivity", "homeViewModel.isStarted.observe isStarted: $isStarted")
                binding.navView.visibility =
                    if (isLandscape() && isStarted) View.GONE else View.VISIBLE
            }
        }

        dashboardViewModel.isFilled.observe(this) { isFilled ->
            if(getCurrentNavigation() == R.id.navigation_dashboard) {
                Log.i("MainActivity", "dashboardViewModel.isFilled.observe isFilled: $isFilled")
                binding.navView.visibility =
                    if (isLandscape() && isFilled) View.GONE else View.VISIBLE
            }
        }
    }

    private val homeViewModel: HomeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(application)
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(application)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustNavBarVisibility()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun adjustNavBarVisibility() {
        when (getCurrentNavigation()) {
            R.id.navigation_home -> {
                //binding.navView.visibility = if (isLandscape()) View.GONE else View.VISIBLE
            }
            R.id.navigation_dashboard -> {
                //binding.navView.visibility = if (isLandscape() && dashboardViewModel.isFilled.value == true) View.GONE else View.VISIBLE
            }
            R.id.navigation_notifications -> {
                binding.navView.visibility = View.VISIBLE
            }
            else -> {
                binding.navView.visibility = View.VISIBLE
            }
        }
        supportActionBar?.apply {
            if (isLandscape()) hide() else show()
        }
    }

    private fun getCurrentNavigation(): Int? {
        return navController.currentDestination?.id
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                dashboardViewModel.onVolumeUpPressed()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                dashboardViewModel.onVolumeDownPressed()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}