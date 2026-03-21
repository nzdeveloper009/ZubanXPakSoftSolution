package com.android.zubanx.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.android.zubanx.BuildConfig
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentSettingsBinding
import com.android.zubanx.service.FloatingOverlayService
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModel: SettingsViewModel by viewModel()

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(requireContext())) {
            viewModel.onEvent(SettingsContract.Event.SetFloatingOverlay(true))
        } else {
            // User denied permission — uncheck the switch
            binding.switchFloating.isChecked = false
        }
    }

    override fun setupViews() {
        binding.cardPremium.setOnClickListener { viewModel.onEvent(SettingsContract.Event.NavigateToPremium) }
        binding.rowHistory.setOnClickListener { viewModel.onEvent(SettingsContract.Event.NavigateToHistory) }
        binding.rowFavourites.setOnClickListener { viewModel.onEvent(SettingsContract.Event.NavigateToFavourites) }
        binding.rowAiTone.setOnClickListener { viewModel.onEvent(SettingsContract.Event.ShowAiTonePicker) }
        binding.rowLanguage.setOnClickListener { viewModel.onEvent(SettingsContract.Event.NavigateToLanguage) }
        binding.rowPrivacyPolicy.setOnClickListener { viewModel.onEvent(SettingsContract.Event.OpenPrivacyPolicy) }
        binding.rowTerms.setOnClickListener { viewModel.onEvent(SettingsContract.Event.OpenTerms) }
        binding.rowRateUs.setOnClickListener { viewModel.onEvent(SettingsContract.Event.RateUs) }
        binding.rowShareApp.setOnClickListener { viewModel.onEvent(SettingsContract.Event.ShareApp) }
        binding.rowContactSupport.setOnClickListener { viewModel.onEvent(SettingsContract.Event.ContactSupport) }

        binding.switchOffline.setOnCheckedChangeListener { _, checked ->
            viewModel.onEvent(SettingsContract.Event.SetOfflineMode(checked))
        }
        binding.switchFloating.setOnCheckedChangeListener { _, checked ->
            if (checked && !Settings.canDrawOverlays(requireContext())) {
                binding.switchFloating.isChecked = false
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                overlayPermissionLauncher.launch(intent)
            } else {
                viewModel.onEvent(SettingsContract.Event.SetFloatingOverlay(checked))
            }
        }
        binding.switchAutoSpeak.setOnCheckedChangeListener { _, checked ->
            viewModel.onEvent(SettingsContract.Event.SetAutoSpeak(checked))
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            binding.tvPremiumStatus.text = if (state.isPremium) "Premium Active ✓" else "Upgrade to Premium"
            binding.tvAiTone.text = state.aiTone.label
            binding.tvVersion.text = "v${state.appVersion}"

            // Set switches without triggering listeners to avoid feedback loops
            binding.switchOffline.setOnCheckedChangeListener(null)
            binding.switchFloating.setOnCheckedChangeListener(null)
            binding.switchAutoSpeak.setOnCheckedChangeListener(null)

            binding.switchOffline.isChecked = state.offlineMode
            binding.switchFloating.isChecked = state.floatingOverlay
            binding.switchAutoSpeak.isChecked = state.autoSpeak

            binding.switchOffline.setOnCheckedChangeListener { _, checked ->
                viewModel.onEvent(SettingsContract.Event.SetOfflineMode(checked))
            }
            binding.switchFloating.setOnCheckedChangeListener { _, checked ->
                if (checked && !Settings.canDrawOverlays(requireContext())) {
                    binding.switchFloating.isChecked = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                } else {
                    viewModel.onEvent(SettingsContract.Event.SetFloatingOverlay(checked))
                }
            }
            binding.switchAutoSpeak.setOnCheckedChangeListener { _, checked ->
                viewModel.onEvent(SettingsContract.Event.SetAutoSpeak(checked))
            }
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is SettingsContract.Effect.Navigate -> {
                    // TODO(Task 7): actionId will be 0 until nav XML is wired; guard against invalid IDs
                    if (effect.actionId != 0) {
                        try {
                            findNavController().navigate(effect.actionId)
                        } catch (e: Exception) {
                            requireContext().toast("Navigation unavailable")
                        }
                    } else {
                        requireContext().toast("Coming soon")
                    }
                }
                is SettingsContract.Effect.OpenUrl -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))
                }
                SettingsContract.Effect.LaunchShare -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Check out ZubanX: https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
                        )
                    }
                    startActivity(Intent.createChooser(intent, "Share ZubanX"))
                }
                SettingsContract.Effect.LaunchRateUs -> {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
                            )
                        )
                    } catch (e: Exception) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                            )
                        )
                    }
                }
                SettingsContract.Effect.LaunchContactSupport -> {
                    startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@zubanx.app")))
                }
                is SettingsContract.Effect.StartFloatingService -> {
                    val serviceIntent = Intent(requireContext(), FloatingOverlayService::class.java)
                    if (effect.enable) {
                        requireContext().startForegroundService(serviceIntent)
                    } else {
                        requireContext().stopService(serviceIntent)
                    }
                }
                is SettingsContract.Effect.ShowToast -> requireContext().toast(effect.message)
                is SettingsContract.Effect.ShowAiToneDialog -> {
                    // TODO(Task 5): Show AI tone picker dialog
                }
            }
        }
    }
}
