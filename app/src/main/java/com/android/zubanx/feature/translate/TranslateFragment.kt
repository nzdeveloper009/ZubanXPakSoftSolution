package com.android.zubanx.feature.translate

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.DialogLanguageSelectBinding
import com.android.zubanx.databinding.FragmentTranslateBinding
import com.android.zubanx.databinding.ItemTranslationHistoryBinding
import com.android.zubanx.domain.model.Translation
import com.android.zubanx.tts.TtsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class TranslateFragment : BaseFragment<FragmentTranslateBinding>(FragmentTranslateBinding::inflate) {

    private val viewModel: TranslateViewModel by viewModel()
    private val ttsManager: TtsManager by inject()

    private val historyAdapter = HistoryAdapter(
        onItemClick = { viewModel.onEvent(TranslateContract.Event.HistoryItemClicked(it)) },
        onDeleteClick = { viewModel.onEvent(TranslateContract.Event.DeleteHistoryItem(it.id)) },
        onSpeakClick = { viewModel.onEvent(TranslateContract.Event.SpeakHistoryItem(it)) },
        onFavouriteClick = { viewModel.onEvent(TranslateContract.Event.ToggleHistoryFavourite(it)) },
        onShareClick = { viewModel.onEvent(TranslateContract.Event.ShareHistoryItem(it)) },
        onCopyClick = { viewModel.onEvent(TranslateContract.Event.CopyHistoryItem(it)) }
    )

    private val micLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onEvent(TranslateContract.Event.MicResult(spoken))
            }
        }
    }

    override fun setupViews() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    findNavController().navigate(R.id.nav_settings)
                    true
                }
                R.id.action_premium -> {
                    findNavController().navigate(R.id.nav_premium)
                    true
                }
                else -> false
            }
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        binding.etSourceText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                binding.tvCharCount.text = "${text.length} / 5000"
                binding.btnClearInput.isVisible = text.isNotEmpty()
                viewModel.onEvent(TranslateContract.Event.InputChanged(text))
            }
        })

        binding.btnTranslate.setOnClickListener {
            hideKeyboard()
            viewModel.onEvent(TranslateContract.Event.TranslateClicked)
        }
        binding.btnClearInput.setOnClickListener {
            binding.etSourceText.text?.clear()
            viewModel.onEvent(TranslateContract.Event.ClearInput)
        }
        binding.btnPaste.setOnClickListener { pasteFromClipboard() }
        binding.btnMic.setOnClickListener { launchGoogleMic(currentSourceCode()) }
        binding.btnSwapLang.setOnClickListener { viewModel.onEvent(TranslateContract.Event.SwapLanguages) }
        binding.btnSourceLang.setOnClickListener { showLanguagePicker(isSource = true) }
        binding.btnTargetLang.setOnClickListener { showLanguagePicker(isSource = false) }
        binding.btnCopy.setOnClickListener { viewModel.onEvent(TranslateContract.Event.CopyTranslation) }
        binding.btnSpeak.setOnClickListener { viewModel.onEvent(TranslateContract.Event.SpeakTranslation) }
        binding.btnFavourite.setOnClickListener { viewModel.onEvent(TranslateContract.Event.AddToFavourites) }
        binding.btnShare.setOnClickListener { viewModel.onEvent(TranslateContract.Event.ShareTranslation) }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            when (state) {
                is TranslateContract.State.Idle -> renderIdle()
                is TranslateContract.State.Translating -> renderTranslating()
                is TranslateContract.State.Success -> renderSuccess(state)
                is TranslateContract.State.Error -> renderError(state)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is TranslateContract.Effect.ShowToast -> requireContext().toast(effect.message)
                is TranslateContract.Effect.CopyToClipboard -> copyToClipboard(effect.text)
                is TranslateContract.Effect.SpeakText -> ttsManager.speak(effect.text, effect.langCode)
                is TranslateContract.Effect.ShareText -> shareText(effect.text)
                is TranslateContract.Effect.LaunchMic -> launchGoogleMic(effect.sourceCode)
                is TranslateContract.Effect.SetInputText -> {
                    binding.etSourceText.setText(effect.text)
                    binding.etSourceText.setSelection(effect.text.length)
                }
            }
        }
    }

    private fun renderIdle() {
        binding.outputCard.isVisible = false
        binding.tvError.isVisible = false
        binding.progressTranslate.isVisible = false
        binding.chipExpert.isVisible = false
        binding.tvHistoryLabel.isVisible = false
        binding.rvHistory.isVisible = false
    }

    private fun renderTranslating() {
        binding.progressTranslate.isVisible = true
        binding.outputCard.isVisible = false
        binding.tvError.isVisible = false
    }

    private fun renderSuccess(state: TranslateContract.State.Success) {
        binding.progressTranslate.isVisible = false
        binding.tvError.isVisible = false
        binding.outputCard.isVisible = true
        binding.tvTranslatedText.text = state.translatedText
        binding.btnSourceLang.text = state.sourceLang.name
        binding.btnTargetLang.text = state.targetLang.name
        binding.chipExpert.isVisible = state.expert != "DEFAULT"
        binding.chipExpert.text = "Expert: ${state.expert}"

        // Update favourite icon based on whether current translation is already favourited
        val isFavourited = "${state.inputText}|${state.targetLang.code}" in state.favouritedKeys
        binding.btnFavourite.setImageResource(
            if (isFavourited) com.android.zubanx.R.drawable.ic_favorite
            else com.android.zubanx.R.drawable.ic_favorite_border
        )

        historyAdapter.favouritedKeys = state.favouritedKeys
        updateHistory(state.history)
    }

    private fun renderError(state: TranslateContract.State.Error) {
        binding.progressTranslate.isVisible = false
        binding.outputCard.isVisible = false
        binding.tvError.isVisible = true
        binding.tvError.text = state.message
        historyAdapter.favouritedKeys = state.favouritedKeys
        updateHistory(state.history)
    }

    private fun updateHistory(history: List<Translation>) {
        val visible = history.isNotEmpty()
        binding.tvHistoryLabel.isVisible = visible
        binding.rvHistory.isVisible = visible
        historyAdapter.submitList(history)
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(requireContext())?.toString()
        if (!text.isNullOrBlank()) {
            binding.etSourceText.setText(text)
            binding.etSourceText.setSelection(text.length)
        }
    }

    private fun launchGoogleMic(sourceCode: String?) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Now")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
            if (sourceCode != null && sourceCode != "auto") {
                val language = sourceCode + "-" + sourceCode.uppercase(Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sourceCode)
            }
        }
        micLauncher.launch(intent)
    }

    private fun showLanguagePicker(isSource: Boolean) {
        val dialog = BottomSheetDialog(requireContext())
        val pickerBinding = DialogLanguageSelectBinding.inflate(layoutInflater)
        dialog.setContentView(pickerBinding.root)

        val languages = if (isSource) listOf(LanguageItem.DETECT) + LanguageItem.ALL else LanguageItem.ALL
        val adapter = LanguagePickerAdapter { selected ->
            if (isSource) {
                binding.btnSourceLang.text = selected.name
                viewModel.onEvent(TranslateContract.Event.SourceLangSelected(selected))
            } else {
                binding.btnTargetLang.text = selected.name
                viewModel.onEvent(TranslateContract.Event.TargetLangSelected(selected))
            }
            dialog.dismiss()
        }

        pickerBinding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        adapter.submitList(languages)

        pickerBinding.etLangSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().lowercase()
                val filtered = languages.filter {
                    it.name.lowercase().contains(query) || it.code.lowercase().contains(query)
                }
                adapter.submitList(filtered)
            }
        })

        dialog.show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("translation", text))
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share translation"))
    }

    private fun currentSourceCode(): String? {
        val s = viewModel.state.value as? TranslateContract.State.Success
        return s?.sourceLang?.code?.takeIf { it != "auto" }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    // --- Inner adapters ---

    class HistoryAdapter(
        private val onItemClick: (Translation) -> Unit,
        private val onDeleteClick: (Translation) -> Unit,
        private val onSpeakClick: (Translation) -> Unit,
        private val onFavouriteClick: (Translation) -> Unit,
        private val onShareClick: (Translation) -> Unit,
        private val onCopyClick: (Translation) -> Unit
    ) : ListAdapter<Translation, HistoryAdapter.VH>(DIFF) {

        var favouritedKeys: Set<String> = emptySet()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        inner class VH(val b: ItemTranslationHistoryBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemTranslationHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            holder.b.tvHistorySource.text = item.sourceText
            holder.b.tvHistoryTranslated.text = item.translatedText
            holder.b.tvHistoryLangPair.text = "${item.sourceLang.uppercase()} → ${item.targetLang.uppercase()}"
            holder.b.tvHistoryExpert.text = item.expert
            holder.b.root.setOnClickListener { onItemClick(item) }
            holder.b.btnDeleteHistory.setOnClickListener { onDeleteClick(item) }
            holder.b.btnHistorySpeak.setOnClickListener { onSpeakClick(item) }
            holder.b.btnHistoryShare.setOnClickListener { onShareClick(item) }
            holder.b.btnHistoryCopy.setOnClickListener { onCopyClick(item) }

            val key = "${item.sourceText}|${item.targetLang}"
            val isFavourited = key in favouritedKeys
            holder.b.btnHistoryFavourite.setImageResource(
                if (isFavourited) com.android.zubanx.R.drawable.ic_favorite
                else com.android.zubanx.R.drawable.ic_favorite_border
            )
            holder.b.btnHistoryFavourite.setOnClickListener { onFavouriteClick(item) }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<Translation>() {
                override fun areItemsTheSame(a: Translation, b: Translation) = a.id == b.id
                override fun areContentsTheSame(a: Translation, b: Translation) = a == b
            }
        }
    }

    class LanguagePickerAdapter(
        private val onSelected: (LanguageItem) -> Unit
    ) : ListAdapter<LanguageItem, LanguagePickerAdapter.VH>(DIFF) {

        inner class VH(val view: android.widget.TextView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = android.widget.TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val px16 = (16 * resources.displayMetrics.density).toInt()
                val px12 = (12 * resources.displayMetrics.density).toInt()
                setPadding(px16, px12, px16, px12)
                textSize = 15f
                isClickable = true
                isFocusable = true
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            holder.view.text = item.name
            holder.view.setOnClickListener { onSelected(item) }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<LanguageItem>() {
                override fun areItemsTheSame(a: LanguageItem, b: LanguageItem) = a.code == b.code
                override fun areContentsTheSame(a: LanguageItem, b: LanguageItem) = a == b
            }
        }
    }
}
