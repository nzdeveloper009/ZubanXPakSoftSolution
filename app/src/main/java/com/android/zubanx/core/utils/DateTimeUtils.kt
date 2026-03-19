package com.android.zubanx.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val displayFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun formatTimestamp(timestampMs: Long): String =
        displayFormat.format(Date(timestampMs))

    fun formatDateOnly(timestampMs: Long): String =
        dateOnlyFormat.format(Date(timestampMs))

    fun now(): Long = System.currentTimeMillis()
}
