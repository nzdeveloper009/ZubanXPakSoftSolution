package com.android.zubanx.core.base

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.viewbinding.ViewBinding
import com.android.zubanx.core.utils.LocaleHelper
import com.android.zubanx.core.utils.zubanDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

abstract class BaseActivity<T : ViewBinding>(
    private val bindingFactory: (LayoutInflater) -> T
) : AppCompatActivity() {

    private var _binding: T? = null
    protected val binding: T
        get() = checkNotNull(_binding) { "ViewBinding accessed after onDestroy" }

    override fun attachBaseContext(newBase: Context) {
        val langCode = runBlocking {
            newBase.zubanDataStore.data.first()[stringPreferencesKey("app_language")] ?: "en"
        }
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, langCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        _binding = bindingFactory(layoutInflater)
        setContentView(binding.root)
        // Dark icons on light background, light icons on dark background
        val isLightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = isLightMode
            isAppearanceLightNavigationBars = isLightMode
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
