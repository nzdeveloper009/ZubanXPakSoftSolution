package com.android.zubanx.app

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseActivity
import com.android.zubanx.core.utils.isVisible
import com.android.zubanx.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id !in BOTTOM_NAV_HIDDEN_DESTINATIONS
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()

    companion object {
        /**
         * Fragment destination IDs where the bottom navigation bar is hidden.
         * Uncomment each ID as the corresponding fragment is added in feature plans.
         */
        private val BOTTOM_NAV_HIDDEN_DESTINATIONS = setOf<Int>(
            R.id.splashFragment,
            R.id.onboardingFragment,
            // R.id.premiumFragment,
            // R.id.wordDetailFragment,
        )
    }
}