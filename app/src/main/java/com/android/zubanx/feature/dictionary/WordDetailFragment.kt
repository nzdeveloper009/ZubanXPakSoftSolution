package com.android.zubanx.feature.dictionary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.view.isVisible
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentWordDetailBinding
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class WordDetailFragment : BaseFragment<FragmentWordDetailBinding>(FragmentWordDetailBinding::inflate) {

    private val viewModel: WordDetailViewModel by viewModel()
    private val repository: DictionaryRepository by inject()

    override fun setupViews() {
        val word = arguments?.getString("word") ?: ""
        val language = arguments?.getString("language") ?: "en"

        val entry = runBlocking { repository.getCached(word, language) }
            ?: DictionaryEntry(word = word, language = language, definition = "", timestamp = 0L)

        viewModel.onEvent(WordDetailContract.Event.Load(entry))

        binding.btnDetailSpeak.setOnClickListener {
            viewModel.onEvent(WordDetailContract.Event.SpeakWord)
        }
        binding.btnDetailCopy.setOnClickListener {
            viewModel.onEvent(WordDetailContract.Event.CopyDefinition)
        }
        binding.btnAiEnrich.setOnClickListener {
            viewModel.onEvent(WordDetailContract.Event.EnrichWithAi("GPT"))
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            when (state) {
                is WordDetailContract.State.Loaded -> renderLoaded(state)
                is WordDetailContract.State.Error -> requireContext().toast(state.message)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is WordDetailContract.Effect.ShowToast -> requireContext().toast(effect.message)
                is WordDetailContract.Effect.SpeakText -> requireContext().toast("Speaking: ${effect.text}")
                is WordDetailContract.Effect.CopyToClipboard -> {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("definition", effect.text))
                }
            }
        }
    }

    private fun renderLoaded(state: WordDetailContract.State.Loaded) {
        val entry = state.entry
        binding.tvDetailWord.text = entry.word
        binding.tvDetailPhonetic.text = entry.phonetic ?: ""
        binding.tvDetailPhonetic.isVisible = !entry.phonetic.isNullOrBlank()
        binding.tvDetailDefinition.text = entry.definition

        if (!entry.partOfSpeech.isNullOrBlank()) {
            binding.chipDetailPartOfSpeech.isVisible = true
            binding.chipDetailPartOfSpeech.text = entry.partOfSpeech
        }

        if (entry.examples.isNotEmpty()) {
            binding.cardExamples.isVisible = true
            binding.tvDetailExamples.text = entry.examples.joinToString("\n\n") { "\"$it\"" }
        } else {
            binding.cardExamples.isVisible = false
        }

        binding.progressAi.isVisible = state.aiLoading
        binding.btnAiEnrich.isVisible = !state.aiLoading && state.aiInsight == null
        if (state.aiInsight != null) {
            binding.tvAiInsight.isVisible = true
            binding.tvAiInsight.text = state.aiInsight
            binding.btnAiEnrich.isVisible = false
        }
    }
}
