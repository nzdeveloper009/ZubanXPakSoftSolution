package com.android.zubanx.feature.language

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentLanguageBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private val viewModel: LanguageViewModel by viewModel()
    private lateinit var adapter: LanguageAdapter

    override fun setupViews() {
        adapter = LanguageAdapter { language ->
            viewModel.onEvent(LanguageContract.Event.SelectLanguage(language.code))
        }
        binding.rvLanguages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLanguages.adapter = adapter
        adapter.submitList(AppLanguage.ALL)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_done) {
                viewModel.onEvent(LanguageContract.Event.Confirm)
                true
            } else false
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            adapter.setSelectedCode(state.selectedCode)
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is LanguageContract.Effect.ApplyLocaleAndRestart -> {
                    // recreate() discards back stack and restores from savedInstanceState,
                    // naturally landing on the start destination. attachBaseContext handles
                    // the actual locale application on restart.
                    requireActivity().recreate()
                }
            }
        }
    }
}
