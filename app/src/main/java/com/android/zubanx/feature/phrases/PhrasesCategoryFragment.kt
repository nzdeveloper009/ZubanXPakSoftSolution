package com.android.zubanx.feature.phrases

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentPhrasesCategoryBinding
import com.android.zubanx.feature.translate.LanguageItem
import com.android.zubanx.tts.TtsManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PhrasesCategoryFragment : BaseFragment<FragmentPhrasesCategoryBinding>(
    FragmentPhrasesCategoryBinding::inflate
) {

    private val args: PhrasesCategoryFragmentArgs by navArgs()
    private val ttsManager: TtsManager by inject()

    private val viewModel: PhrasesCategoryViewModel by viewModel {
        parametersOf(args.categoryId)
    }

    private val adapter = PhrasesCategoryAdapter(
        onExpand = { index -> viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(index)) },
        onSpeak = { index -> viewModel.onEvent(PhrasesCategoryContract.Event.SpeakPhrase(index)) },
        onCopy = { index -> viewModel.onEvent(PhrasesCategoryContract.Event.CopyPhrase(index)) },
        onZoom = { text, lang ->
            findNavController().navigate(
                PhrasesCategoryFragmentDirections.actionCategoryToZoom(text, lang)
            )
        },
        onRetry = { index -> viewModel.onEvent(PhrasesCategoryContract.Event.RetryTranslation(index)) }
    )

    override fun setupViews() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.rvPhrases.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPhrases.adapter = adapter

        binding.btnSourceLang.setOnClickListener { showLanguagePicker(isSource = true) }
        binding.btnTargetLang.setOnClickListener { showLanguagePicker(isSource = false) }
        binding.btnSwapLang.setOnClickListener { viewModel.onEvent(PhrasesCategoryContract.Event.SwapLanguages) }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state is PhrasesCategoryContract.State.Active) {
                binding.toolbar.title = state.category.displayName
                binding.btnSourceLang.text = state.langSource.name
                binding.btnTargetLang.text = state.langTarget.name
                adapter.updateTargetLang(state.langTarget.code)

                val items = state.displayPhrases.mapIndexed { index, phrase ->
                    PhraseItem(
                        index = index,
                        displayText = phrase,
                        translatedText = state.translationCache["${state.langSource.code}:${state.langTarget.code}:$index"],
                        isExpanded = state.expandedIndex == index,
                        isLoading = state.loadingIndices.contains(index),
                        isError = state.errorIndices.contains(index)
                    )
                }
                adapter.submitList(items)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is PhrasesCategoryContract.Effect.SpeakText ->
                    ttsManager.speak(effect.text, effect.langCode)
                is PhrasesCategoryContract.Effect.CopyToClipboard -> {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("phrase", effect.text))
                    requireContext().toast("Copied")
                }
                is PhrasesCategoryContract.Effect.ShowToast ->
                    requireContext().toast(effect.message)
                is PhrasesCategoryContract.Effect.NavigateToZoom ->
                    findNavController().navigate(
                        PhrasesCategoryFragmentDirections.actionCategoryToZoom(effect.translatedText, effect.langCode)
                    )
            }
        }
    }

    private fun showLanguagePicker(isSource: Boolean) {
        val languages = LanguageItem.ALL.filter { it != LanguageItem.DETECT }
        val names = languages.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select language")
            .setItems(names) { _, which ->
                val selected = languages[which]
                viewModel.onEvent(
                    if (isSource) PhrasesCategoryContract.Event.LangSourceSelected(selected)
                    else PhrasesCategoryContract.Event.LangTargetSelected(selected)
                )
            }
            .show()
    }
}
