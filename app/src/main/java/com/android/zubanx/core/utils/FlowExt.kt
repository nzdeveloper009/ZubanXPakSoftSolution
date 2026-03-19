package com.android.zubanx.core.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * Emits the first item in each [windowDuration] window, dropping subsequent items.
 * Useful for preventing double-tap actions.
 */
fun <T> Flow<T>.throttleFirst(windowDuration: Duration): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val now = System.currentTimeMillis()
        if (now - lastEmitTime >= windowDuration.inWholeMilliseconds) {
            lastEmitTime = now
            emit(value)
        }
    }
}
