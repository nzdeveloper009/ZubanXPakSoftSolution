package com.android.zubanx.feature.phrases

import androidx.appcompat.app.AppCompatActivity
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

    override fun setupViews() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.tvZoomText.text = args.translatedText
        binding.btnSpeak.setOnClickListener {
            ttsManager.speak(args.translatedText, args.langCode)
        }
    }
}
