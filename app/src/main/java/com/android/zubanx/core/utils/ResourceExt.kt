package com.android.zubanx.core.utils

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

fun Context.stringRes(@StringRes res: Int, vararg args: Any): String =
    if (args.isEmpty()) getString(res) else getString(res, *args)

fun Context.colorResCompat(@ColorRes res: Int): Int =
    ContextCompat.getColor(this, res)
