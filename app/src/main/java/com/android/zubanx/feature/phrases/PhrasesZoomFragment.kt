package com.android.zubanx.feature.phrases

import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.tts.TtsManager
import com.android.zubanx.databinding.FragmentPhrasesZoomBinding
import org.koin.android.ext.android.inject

class PhrasesZoomFragment : BaseFragment<FragmentPhrasesZoomBinding>(
    FragmentPhrasesZoomBinding::inflate
) {

    private val args: PhrasesZoomFragmentArgs by navArgs()
    private val ttsManager: TtsManager by inject()

    override fun onPause() {
        super.onPause()
        ttsManager.stop()
    }

    override fun setupViews() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.tvZoomText.text = args.translatedText
        binding.btnSpeak.setOnClickListener {
            ttsManager.speak(args.translatedText, args.langCode)
        }
    }
}
