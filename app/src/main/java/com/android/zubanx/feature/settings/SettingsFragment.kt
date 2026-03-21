package com.android.zubanx.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.android.zubanx.BuildConfig
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentSettingsBinding
import com.android.zubanx.domain.model.AiTone
import com.android.zubanx.service.FloatingOverlayService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            binding.tvPremiumStatus.text = if (state.isPremium) getString(R.string.settings_premium_active) else getString(R.string.settings_upgrade_premium)
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
                            requireContext().toast(getString(R.string.toast_nav_unavailable))
                        }
                    } else {
                        requireContext().toast(getString(R.string.toast_coming_soon))
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
                    startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_app)))
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
                is SettingsContract.Effect.ShowToast -> requireContext().toast(getString(effect.messageResId))
                is SettingsContract.Effect.ShowAiToneDialog -> {
                    val tones = AiTone.entries.toTypedArray()
                    val labels = tones.map { "${it.label} — ${it.description}" }.toTypedArray()
                    val currentIndex = tones.indexOf(effect.currentTone).coerceAtLeast(0)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_ai_tone_dialog_title)
                        .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                            viewModel.onEvent(SettingsContract.Event.SetAiTone(tones[which]))
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
                }
            }
        }
    }
}
