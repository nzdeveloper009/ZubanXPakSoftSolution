package com.android.zubanx.feature.favourite

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.zubanx.databinding.BottomSheetFavouriteDetailBinding
import com.android.zubanx.domain.model.Favourite
import com.android.zubanx.tts.TtsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.android.ext.android.inject

class FavouriteDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFavouriteDetailBinding? = null
    private val binding get() = _binding!!
    private val ttsManager: TtsManager by inject()

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
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                "favourite",
                "${favourite.sourceText} → ${favourite.translatedText}"
            ))
            android.widget.Toast.makeText(requireContext(), "Copied", android.widget.Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${favourite.sourceText} → ${favourite.translatedText}")
            }
            startActivity(Intent.createChooser(intent, "Share"))
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
