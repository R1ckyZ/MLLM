package com.example.mllm

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

const val LANGUAGE_CODE_EN = "en"
const val LANGUAGE_CODE_ZH = "zh"

fun resolveLocale(languageCode: String): Locale {
    val resolvedCode = languageCode.ifBlank { Locale.getDefault().language }
    if (resolvedCode.contains('-')) {
        return Locale.forLanguageTag(resolvedCode)
    }
    return when (resolvedCode) {
        LANGUAGE_CODE_EN -> Locale.ENGLISH
        LANGUAGE_CODE_ZH -> Locale.SIMPLIFIED_CHINESE
        else -> Locale(resolvedCode)
    }
}

fun resolveLanguageTag(languageCode: String): String {
    val resolvedCode = languageCode.ifBlank { Locale.getDefault().language }
    return when (resolvedCode) {
        LANGUAGE_CODE_EN -> "en"
        LANGUAGE_CODE_ZH -> "zh-CN"
        else -> resolvedCode
    }
}

fun createLocalizedContext(
    base: Context,
    languageCode: String,
): Context {
    val locale = resolveLocale(languageCode)
    val config = Configuration(base.resources.configuration)
    config.setLocale(locale)
    return base.createConfigurationContext(config)
}
