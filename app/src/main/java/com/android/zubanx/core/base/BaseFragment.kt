package com.android.zubanx.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<T : ViewBinding>(
    private val bindingFactory: (LayoutInflater) -> T
) : Fragment() {

    private var _binding: T? = null
    protected val binding: T
        get() = checkNotNull(_binding) { "ViewBinding accessed outside view lifecycle" }

    /**
     * Set in [setupViews] to intercept back press.
     * Return `true` = consumed (do nothing), `false` = default (pop back stack).
     *
     * To wire this up, register with OnBackPressedDispatcher inside setupViews():
     * ```kotlin
     * requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
     *     if (backPressHandler?.invoke() != true) {
     *         isEnabled = false
     *         requireActivity().onBackPressedDispatcher.onBackPressed()
     *     }
     * }
     * ```
     * The base class does NOT register this automatically to avoid always intercepting
     * back press even when no handler is set.
     */
    var backPressHandler: (() -> Boolean)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = bindingFactory(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Override to initialise views, click listeners, and set [backPressHandler]. */
    open fun setupViews() {}

    /** Override to collect state/effect flows via collectFlow(). */
    open fun observeState() {}
}
