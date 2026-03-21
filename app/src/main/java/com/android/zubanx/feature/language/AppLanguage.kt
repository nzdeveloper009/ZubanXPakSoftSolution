package com.android.zubanx.feature.language

import androidx.annotation.DrawableRes
import com.android.zubanx.R

data class AppLanguage(
    val code: String,
    val displayName: String,
    @DrawableRes val flagDrawable: Int
) {
    companion object {
        val ALL = listOf(
            AppLanguage("en",  "English",    R.drawable.flag_us),
            AppLanguage("es",  "Spanish",    R.drawable.flag_es),
            AppLanguage("fil", "Filipino",   R.drawable.flag_ph),
            AppLanguage("hi",  "Hindi",      R.drawable.flag_in),
            AppLanguage("de",  "German",     R.drawable.flag_de),
            AppLanguage("fr",  "French",     R.drawable.flag_fr),
            AppLanguage("it",  "Italian",    R.drawable.flag_it),
            AppLanguage("ko",  "Korean",     R.drawable.flag_kr),
            AppLanguage("pt",  "Portuguese", R.drawable.flag_pt),
            AppLanguage("vi",  "Vietnamese", R.drawable.flag_vn),
            AppLanguage("ar",  "Arabic",     R.drawable.flag_sa),
            AppLanguage("ru",  "Russian",    R.drawable.flag_ru),
            AppLanguage("bn",  "Bengali",    R.drawable.flag_bd),
            AppLanguage("zh",  "Chinese",    R.drawable.flag_cn),
            AppLanguage("ur",  "Urdu",       R.drawable.flag_pk),
            AppLanguage("id",  "Indonesian", R.drawable.flag_id),
            AppLanguage("tr",  "Turkish",    R.drawable.flag_tr),
        )

        fun fromCode(code: String) = ALL.firstOrNull { it.code == code } ?: ALL.first()
    }
}
