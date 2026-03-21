package com.android.zubanx.feature.story

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentStoryCategoryBinding
import com.android.zubanx.feature.translate.LanguageItem
import com.android.zubanx.tts.TtsManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class StoryCategoryFragment : BaseFragment<FragmentStoryCategoryBinding>(
    FragmentStoryCategoryBinding::inflate
) {

    private val args: StoryCategoryFragmentArgs by navArgs()
    private val ttsManager: TtsManager by inject()
    private val viewModel: StoryCategoryViewModel by viewModel { parametersOf(args.categoryId) }

    private val adapter = StoryCategoryAdapter(
        onExpand    = { viewModel.onEvent(StoryCategoryContract.Event.ExpandStory(it)) },
        onTranslate = { viewModel.onEvent(StoryCategoryContract.Event.TranslateStory(it)) },
        onSpeak     = { viewModel.onEvent(StoryCategoryContract.Event.SpeakStory(it)) },
        onCopy      = { viewModel.onEvent(StoryCategoryContract.Event.CopyStory(it)) },
        onRetry     = { viewModel.onEvent(StoryCategoryContract.Event.RetryTranslation(it)) }
    )

    override fun onPause() {
        super.onPause()
        ttsManager.stop()
    }

    override fun setupViews() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.rvStories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStories.adapter = adapter
        binding.btnSourceLang.setOnClickListener { showLanguagePicker(isSource = true) }
        binding.btnTargetLang.setOnClickListener { showLanguagePicker(isSource = false) }
        binding.btnSwapLang.setOnClickListener {
            viewModel.onEvent(StoryCategoryContract.Event.SwapLanguages)
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state !is StoryCategoryContract.State.Active) return@collectFlow
            binding.toolbar.title = state.category.displayName
            binding.btnSourceLang.text = state.langSource.name
            binding.btnTargetLang.text = state.langTarget.name

            val items = state.stories.mapIndexed { index, entry ->
                val cacheKey = "${state.langSource.code}:${state.langTarget.code}:${entry.id}"
                StoryDisplayItem(
                    index = index,
                    entry = entry,
                    translatedBody = state.translationCache[cacheKey],
                    isExpanded = state.expandedIndex == index,
                    isLoading = state.loadingIndices.contains(index),
                    hasError = state.errorIndices.contains(index)
                )
            }
            adapter.submitList(items)
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is StoryCategoryContract.Effect.SpeakText ->
                    ttsManager.speak(effect.text, effect.langCode)
                is StoryCategoryContract.Effect.CopyToClipboard -> {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("story", effect.text))
                    requireContext().toast(getString(R.string.toast_copied))
                }
                is StoryCategoryContract.Effect.ShowToast ->
                    requireContext().toast(effect.message)
            }
        }
    }

    private fun showLanguagePicker(isSource: Boolean) {
        val languages = LanguageItem.ALL.filter { it != LanguageItem.DETECT }
        val names = languages.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_select_language)
            .setItems(names) { _, which ->
                val selected = languages[which]
                viewModel.onEvent(
                    if (isSource) StoryCategoryContract.Event.LangSourceSelected(selected)
                    else StoryCategoryContract.Event.LangTargetSelected(selected)
                )
            }
            .show()
    }
}
