package com.android.zubanx.feature.idioms

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
import com.android.zubanx.databinding.FragmentIdiomsCategoryBinding
import com.android.zubanx.feature.translate.LanguageItem
import com.android.zubanx.tts.TtsManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class IdiomsCategoryFragment : BaseFragment<FragmentIdiomsCategoryBinding>(
    FragmentIdiomsCategoryBinding::inflate
) {

    private val args: IdiomsCategoryFragmentArgs by navArgs()
    private val ttsManager: TtsManager by inject()
    private val viewModel: IdiomsCategoryViewModel by viewModel { parametersOf(args.categoryId) }

    private val adapter = IdiomsCategoryAdapter(
        onExpand = { viewModel.onEvent(IdiomsCategoryContract.Event.ExpandIdiom(it)) },
        onSpeak  = { viewModel.onEvent(IdiomsCategoryContract.Event.SpeakIdiom(it)) },
        onCopy   = { viewModel.onEvent(IdiomsCategoryContract.Event.CopyIdiom(it)) },
        onRetry  = { viewModel.onEvent(IdiomsCategoryContract.Event.RetryTranslation(it)) }
    )

    override fun onPause() {
        super.onPause()
        ttsManager.stop()
    }

    override fun setupViews() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.rvIdioms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvIdioms.adapter = adapter
        binding.btnSourceLang.setOnClickListener { showLanguagePicker(isSource = true) }
        binding.btnTargetLang.setOnClickListener { showLanguagePicker(isSource = false) }
        binding.btnSwapLang.setOnClickListener {
            viewModel.onEvent(IdiomsCategoryContract.Event.SwapLanguages)
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state !is IdiomsCategoryContract.State.Active) return@collectFlow
            binding.toolbar.title = state.category.displayName
            binding.btnSourceLang.text = state.langSource.name
            binding.btnTargetLang.text = state.langTarget.name

            val items = state.idioms.mapIndexed { index, entry ->
                val cacheKey = "${state.langSource.code}:${state.langTarget.code}:$index"
                IdiomDisplayItem(
                    index = index,
                    entry = entry,
                    translatedMeaning = state.translationCache[cacheKey],
                    isExpanded = state.expandedIndex == index,
                    isLoading = state.loadingIndices.contains(index),
                    hasError = state.errorIndices.contains(index)
                )
            }
            adapter.submitList(items)
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is IdiomsCategoryContract.Effect.SpeakText ->
                    ttsManager.speak(effect.text, effect.langCode)
                is IdiomsCategoryContract.Effect.CopyToClipboard -> {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("idiom", effect.text))
                    requireContext().toast(getString(R.string.toast_copied))
                }
                is IdiomsCategoryContract.Effect.ShowToast ->
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
                    if (isSource) IdiomsCategoryContract.Event.LangSourceSelected(selected)
                    else IdiomsCategoryContract.Event.LangTargetSelected(selected)
                )
            }
            .show()
    }
}
