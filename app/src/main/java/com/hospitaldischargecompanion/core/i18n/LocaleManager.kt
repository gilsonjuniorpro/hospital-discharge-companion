package com.hospitaldischargecompanion.core.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.hospitaldischargecompanion.core.demo.AppLocale

object LocaleManager {
    fun applyLocale(locale: AppLocale) {
        val target = if (locale == AppLocale.SYSTEM || locale.languageTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(locale.languageTag)
        }
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.toLanguageTags() != target.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(target)
        }
    }
}
