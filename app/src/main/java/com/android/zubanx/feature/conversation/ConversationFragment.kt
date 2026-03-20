package com.android.zubanx.feature.conversation

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentConversationBinding
import com.android.zubanx.databinding.ItemConversationMessageBinding
import com.android.zubanx.feature.translate.LanguageItem
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConversationFragment : BaseFragment<FragmentConversationBinding>(FragmentConversationBinding::inflate) {

    private val viewModel: ConversationViewModel by viewModel()

    // Track which speaker launched the mic so the result routes correctly
    private var pendingMicSpeaker: ConversationContract.SpeakerSide? = null

    private val micLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                when (pendingMicSpeaker) {
                    ConversationContract.SpeakerSide.A ->
                        viewModel.onEvent(ConversationContract.Event.MicResultA(spoken))
                    ConversationContract.SpeakerSide.B ->
                        viewModel.onEvent(ConversationContract.Event.MicResultB(spoken))
                    null -> Unit
                }
            }
        }
        pendingMicSpeaker = null  // always reset, moved outside the if block
    }

    private val adapter = MessageAdapter()

    override fun setupViews() {
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        binding.btnMicA.setOnClickListener {
            viewModel.onEvent(ConversationContract.Event.LaunchMicA)
        }
        binding.btnMicB.setOnClickListener {
            viewModel.onEvent(ConversationContract.Event.LaunchMicB)
        }
        binding.btnClear.setOnClickListener {
            viewModel.onEvent(ConversationContract.Event.ClearConversation)
        }
        binding.btnLangA.setOnClickListener { showLanguagePicker(ConversationContract.SpeakerSide.A) }
        binding.btnLangB.setOnClickListener { showLanguagePicker(ConversationContract.SpeakerSide.B) }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state is ConversationContract.State.Active) {
                renderActive(state)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is ConversationContract.Effect.ShowToast -> requireContext().toast(effect.message)
                is ConversationContract.Effect.SpeakText -> {
                    // TTS stub — toast until TTS integration is added
                    requireContext().toast("Speaking: ${effect.text}")
                }
                is ConversationContract.Effect.LaunchMic -> launchMic(effect.langCode, effect.speaker)
            }
        }
    }

    private fun renderActive(state: ConversationContract.State.Active) {
        binding.btnLangA.text = state.langA.name
        binding.btnLangB.text = state.langB.name
        binding.progressA.isVisible = state.isTranslatingA
        binding.progressB.isVisible = state.isTranslatingB
        binding.btnMicA.isEnabled = !state.isTranslatingA
        binding.btnMicB.isEnabled = !state.isTranslatingB
        adapter.submitList(state.messages) {
            binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun launchMic(langCode: String, speaker: ConversationContract.SpeakerSide) {
        pendingMicSpeaker = speaker
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
            val localeTag = if ('-' in langCode) langCode else "$langCode-${langCode.uppercase()}"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
        }
        // TODO: Add RECORD_AUDIO runtime permission check before launching
        // (pre-existing gap shared with TranslateFragment — defer to a dedicated permissions plan)
        micLauncher.launch(intent)
    }

    private fun showLanguagePicker(speaker: ConversationContract.SpeakerSide) {
        // Exclude DETECT (auto) — both speakers must have a specific language
        val languages = LanguageItem.ALL.filter { it != LanguageItem.DETECT }
        val names = languages.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select language")
            .setItems(names) { _, which ->
                val selected = languages[which]
                when (speaker) {
                    ConversationContract.SpeakerSide.A ->
                        viewModel.onEvent(ConversationContract.Event.LangASelected(selected))
                    ConversationContract.SpeakerSide.B ->
                        viewModel.onEvent(ConversationContract.Event.LangBSelected(selected))
                }
            }
            .show()
    }

    // --- Adapter ---
    class MessageAdapter : ListAdapter<ConversationContract.ConversationMessage, MessageAdapter.VH>(DIFF) {

        inner class VH(val b: ItemConversationMessageBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemConversationMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val msg = getItem(position)
            holder.b.tvOriginal.text = msg.originalText
            holder.b.tvTranslated.text = msg.translatedText

            // Align Speaker A to left, Speaker B to right
            val gravity = if (msg.speakerSide == ConversationContract.SpeakerSide.A) {
                android.view.Gravity.START
            } else {
                android.view.Gravity.END
            }
            (holder.b.cardOriginal.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let {
                it.gravity = gravity
                holder.b.cardOriginal.layoutParams = it
            }
            (holder.b.cardTranslated.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let {
                it.gravity = gravity
                holder.b.cardTranslated.layoutParams = it
            }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<ConversationContract.ConversationMessage>() {
                override fun areItemsTheSame(a: ConversationContract.ConversationMessage, b: ConversationContract.ConversationMessage) = a.id == b.id
                override fun areContentsTheSame(a: ConversationContract.ConversationMessage, b: ConversationContract.ConversationMessage) = a == b
            }
        }
    }
}
