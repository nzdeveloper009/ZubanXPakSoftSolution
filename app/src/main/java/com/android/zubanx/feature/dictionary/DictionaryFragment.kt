package com.android.zubanx.feature.dictionary

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.navigation.safeNavigate
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.tts.TtsManager
import org.koin.android.ext.android.inject
import com.android.zubanx.databinding.FragmentDictionaryBinding
import com.android.zubanx.databinding.ItemDictionaryHistoryBinding
import com.android.zubanx.domain.model.DictionaryEntry
import org.koin.androidx.viewmodel.ext.android.viewModel

class DictionaryFragment : BaseFragment<FragmentDictionaryBinding>(FragmentDictionaryBinding::inflate) {

    private val viewModel: DictionaryViewModel by viewModel()
    private val ttsManager: TtsManager by inject()

    private val historyAdapter = HistoryAdapter(
        onItemClick = { viewModel.onEvent(DictionaryContract.Event.HistoryItemClicked(it)) },
        onDetailClick = { viewModel.onEvent(DictionaryContract.Event.NavigateToDetail(it)) }
    )

    private val micLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                binding.etSearch.setText(spoken)
                viewModel.onEvent(DictionaryContract.Event.MicResult(spoken))
            }
        }
    }

    override fun setupViews() {
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onEvent(DictionaryContract.Event.QueryChanged(s?.toString() ?: ""))
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                hideKeyboard()
                viewModel.onEvent(DictionaryContract.Event.SearchClicked)
                true
            } else false
        }

        binding.btnMic.setOnClickListener { launchMic() }
        binding.btnSpeak.setOnClickListener {
            val word = binding.tvWord.text?.toString() ?: return@setOnClickListener
            val lang = (viewModel.state.value as? DictionaryContract.State.Success)?.entry?.language ?: "en"
            ttsManager.speak(word, lang)
        }
        binding.btnCopy.setOnClickListener {
            val def = binding.tvDefinition.text?.toString() ?: return@setOnClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("definition", def))
            requireContext().toast(getString(R.string.toast_copied))
        }
        binding.btnViewDetail.setOnClickListener {
            val entry = (viewModel.state.value as? DictionaryContract.State.Success)?.entry ?: return@setOnClickListener
            viewModel.onEvent(DictionaryContract.Event.NavigateToDetail(entry))
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            when (state) {
                is DictionaryContract.State.Idle -> renderIdle(state)
                is DictionaryContract.State.Searching -> renderSearching()
                is DictionaryContract.State.Success -> renderSuccess(state)
                is DictionaryContract.State.Error -> renderError(state)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is DictionaryContract.Effect.ShowToast -> requireContext().toast(effect.message)
                is DictionaryContract.Effect.OpenWordDetail -> {
                    val action = DictionaryFragmentDirections.actionDictionaryToWordDetail(
                        word = effect.entry.word,
                        language = effect.entry.language
                    )
                    safeNavigate(action)
                }
                is DictionaryContract.Effect.LaunchMic -> launchMic()
            }
        }
    }

    private fun renderIdle(state: DictionaryContract.State.Idle) {
        binding.resultCard.isVisible = false
        binding.progressSearch.isVisible = false
        binding.tvError.isVisible = false
        updateHistory(state.history)
    }

    private fun renderSearching() {
        binding.progressSearch.isVisible = true
        binding.resultCard.isVisible = false
        binding.tvError.isVisible = false
    }

    private fun renderSuccess(state: DictionaryContract.State.Success) {
        binding.progressSearch.isVisible = false
        binding.tvError.isVisible = false
        binding.resultCard.isVisible = true

        val entry = state.entry
        binding.tvWord.text = entry.word
        binding.tvPhonetic.text = entry.phonetic ?: ""
        binding.tvPhonetic.isVisible = !entry.phonetic.isNullOrBlank()
        binding.tvDefinition.text = entry.definition

        if (!entry.partOfSpeech.isNullOrBlank()) {
            binding.chipPartOfSpeech.isVisible = true
            binding.chipPartOfSpeech.text = entry.partOfSpeech
        } else {
            binding.chipPartOfSpeech.isVisible = false
        }

        updateHistory(state.history)
    }

    private fun renderError(state: DictionaryContract.State.Error) {
        binding.progressSearch.isVisible = false
        binding.resultCard.isVisible = false
        binding.tvError.isVisible = true
        binding.tvError.text = state.message
        updateHistory(state.history)
    }

    private fun updateHistory(history: List<DictionaryEntry>) {
        val visible = history.isNotEmpty()
        binding.tvHistoryLabel.isVisible = visible
        binding.rvHistory.isVisible = visible
        historyAdapter.submitList(history)
    }

    private fun launchMic() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a word")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-EN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en")
        }
        micLauncher.launch(intent)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    // --- History adapter ---
    class HistoryAdapter(
        private val onItemClick: (DictionaryEntry) -> Unit,
        private val onDetailClick: (DictionaryEntry) -> Unit
    ) : ListAdapter<DictionaryEntry, HistoryAdapter.VH>(DIFF) {

        inner class VH(val b: ItemDictionaryHistoryBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemDictionaryHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            holder.b.tvHistoryWord.text = item.word
            holder.b.tvHistoryPhonetic.text = item.phonetic ?: ""
            holder.b.tvHistoryPhonetic.isVisible = !item.phonetic.isNullOrBlank()
            holder.b.root.setOnClickListener { onItemClick(item) }
            holder.b.btnHistoryDetail.setOnClickListener { onDetailClick(item) }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<DictionaryEntry>() {
                override fun areItemsTheSame(a: DictionaryEntry, b: DictionaryEntry) = a.id == b.id && a.word == b.word
                override fun areContentsTheSame(a: DictionaryEntry, b: DictionaryEntry) = a == b
            }
        }
    }
}
