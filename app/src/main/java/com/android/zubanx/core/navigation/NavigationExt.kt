package com.android.zubanx.core.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController

/**
 * Navigate safely — checks the action exists on the current destination before
 * navigating, preventing crashes from rapid double-taps.
 */
fun Fragment.safeNavigate(directions: NavDirections) {
    val navController = findNavController()
    val action = navController.currentDestination?.getAction(directions.actionId)
    if (action != null) {
        navController.navigate(directions)
    }
}

/**
 * Pop back stack to [destinationId].
 * @param inclusive If true, also pops [destinationId] itself.
 */
fun Fragment.popBackTo(@IdRes destinationId: Int, inclusive: Boolean = false) {
    findNavController().popBackStack(destinationId, inclusive)
}

/**
 * Read a result that was left by the screen this fragment navigated to via [setNavigationResult].
 */
fun <T> Fragment.getNavigationResult(key: String): T? =
    findNavController().currentBackStackEntry
        ?.savedStateHandle
        ?.get<T>(key)

/**
 * Deliver a result to the fragment that opened this screen.
 */
fun <T> Fragment.setNavigationResult(key: String, value: T) {
    findNavController().previousBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
}
