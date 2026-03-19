package com.android.zubanx.core.utils

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// NOTE: These are simplified shims. Unlike the deprecated lifecycle-aware variants,
// they do NOT suspend until a specific lifecycle state — they launch immediately.
// For lifecycle-aware flow collection use collectFlow() (FragmentExt) or
// repeatOnLifecycle() directly. These exist only as call-site convenience wrappers.
fun LifecycleCoroutineScope.launchWhenStarted(block: suspend () -> Unit): Job =
    launch { block() }

fun LifecycleCoroutineScope.launchWhenResumed(block: suspend () -> Unit): Job =
    launch { block() }
