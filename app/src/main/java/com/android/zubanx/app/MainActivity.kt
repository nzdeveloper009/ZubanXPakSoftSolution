package com.android.zubanx.app

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseActivity
import com.android.zubanx.core.utils.isVisible
import com.android.zubanx.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private lateinit var navController: NavController

    private var isBottomNavAllowedByDestination = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNavigation()
        setupInsets()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            isBottomNavAllowedByDestination = destination.id !in BOTTOM_NAV_HIDDEN_DESTINATIONS
            binding.bottomNav.isVisible = isBottomNavAllowedByDestination
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            // Push fragment content below the status bar
            binding.navHostFragment.updatePadding(top = systemBars.top)

            // Keep bottom nav above the system navigation bar; hide when keyboard is up
            binding.bottomNav.updatePadding(bottom = systemBars.bottom)
            binding.bottomNav.isVisible = isBottomNavAllowedByDestination && !imeVisible

            insets
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