package com.android.zubanx.feature.favourite

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.android.zubanx.databinding.BottomSheetFavouriteDetailBinding
import com.android.zubanx.domain.model.Favourite
import com.android.zubanx.tts.TtsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.android.ext.android.inject

class FavouriteDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFavouriteDetailBinding? = null
    private val binding get() = _binding!!
    private val ttsManager: TtsManager by inject()
    private val viewModel: FavouriteViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetFavouriteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val favourite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(ARG_FAVOURITE, Favourite::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(ARG_FAVOURITE)!!
        }

        binding.tvLangPair.text = "${favourite.sourceLang.uppercase()} → ${favourite.targetLang.uppercase()}"
        binding.tvSourceText.text = favourite.sourceText
        binding.tvTranslatedText.text = favourite.translatedText

        binding.btnCopy.setOnClickListener {
            viewModel.onEvent(FavouriteContract.Event.CopyText("${favourite.sourceText} → ${favourite.translatedText}"))
            dismiss()
        }

        binding.btnShare.setOnClickListener {
            viewModel.onEvent(FavouriteContract.Event.ShareText("${favourite.sourceText} → ${favourite.translatedText}"))
            dismiss()
        }

        binding.btnSpeak.setOnClickListener {
            ttsManager.speak(favourite.translatedText, favourite.targetLang.ifEmpty { favourite.sourceLang })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_FAVOURITE = "favourite"

        fun newInstance(favourite: Favourite) = FavouriteDetailBottomSheet().apply {
            arguments = Bundle().apply { putParcelable(ARG_FAVOURITE, favourite) }
        }
    }
}
