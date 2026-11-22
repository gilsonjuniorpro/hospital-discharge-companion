package com.hospitaldischargecompanion.core.i18n

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.discharge.EpisodeCaseType
import org.junit.Test

class LocalizedClinicalPackTest {

    @Test
    fun translatesEverySeededCaseAcrossPortugueseSpanishAndFrench() {
        val episodes = ClinicalSeedFactory.createEpisodes()
        val nonEnglishLocales = listOf(AppLocale.PT_BR, AppLocale.ES, AppLocale.FR)

        nonEnglishLocales.forEach { locale ->
            episodes.forEach { ep ->
                val localized = LocalizedClinicalPack.applyTo(ep, locale)
                assertThat(localized.diagnosisTitle).isNotEqualTo(ep.diagnosisTitle)
                assertThat(localized.diagnosisSummary).isNotEqualTo(ep.diagnosisSummary)
                assertThat(localized.redFlags).hasSize(ep.redFlags.size)
            }
        }

        val englishOriginal = episodes.first { it.caseType == EpisodeCaseType.HEART_FAILURE }
        assertThat(LocalizedClinicalPack.applyTo(englishOriginal, AppLocale.EN)).isEqualTo(englishOriginal)
    }
}
