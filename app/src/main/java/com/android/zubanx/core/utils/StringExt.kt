package com.android.zubanx.core.utils

fun String?.isNotNullOrBlank(): Boolean = !this.isNullOrBlank()

/** Returns "—" if null or blank, otherwise the string itself. */
fun String?.orDash(): String = if (isNullOrBlank()) "—" else this!!

fun String.capitalizeFirst(): String =
    if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
