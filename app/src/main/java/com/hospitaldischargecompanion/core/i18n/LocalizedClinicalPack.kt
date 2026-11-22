package com.hospitaldischargecompanion.core.i18n

import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.EpisodeCaseType

data class LocalizedClinicalOverlay(
    val diagnosisTitle: String,
    val diagnosisSummary: String,
    val redFlags: List<String>
)

/**
 * Provides localized seed clinical content packs for pt-BR, es, and fr while keeping
 * English as the canonical default source in ClinicalSeedFactory.
 */
object LocalizedClinicalPack {
    fun overlayFor(caseType: EpisodeCaseType, locale: AppLocale): LocalizedClinicalOverlay? = when (locale) {
        AppLocale.PT_BR -> when (caseType) {
            EpisodeCaseType.HEART_FAILURE -> LocalizedClinicalOverlay(
                diagnosisTitle = "Insuficiência Cardíaca (ICFEr) Descompensada",
                diagnosisSummary = "Alta após 4 dias de diurese venosa por sobrecarga volêmica. Meta de peso seco 76,5 kg; monitorar peso diário, PA e adesão ao diurético.",
                redFlags = listOf(
                    "Ganho de peso de 2 kg ou mais em 2 dias",
                    "Pressão sistólica acima de 160 mmHg ou abaixo de 90 mmHg",
                    "Piora da falta de ar em repouso ou necessidade de travesseiros extras",
                    "Dor no peito, desmaio ou SpO2 abaixo de 92%"
                )
            )
            EpisodeCaseType.POST_SURGERY -> LocalizedClinicalOverlay(
                diagnosisTitle = "Pós-Operatório de Colecistectomia Videolaparoscópica",
                diagnosisSummary = "Cirurgia sem intercorrências com 4 portais. Monitorar incisões, controle da dor, deambulação precoce e dieta leve.",
                redFlags = listOf(
                    "Febre acima de 38,0 °C ou calafrios",
                    "Vermelhidão progressiva, calor ou secreção purulenta na incisão",
                    "Dor abdominal intensa (7/10 ou mais) sem melhora",
                    "Náuseas persistentes, vômitos ou icterícia"
                )
            )
            EpisodeCaseType.PNEUMONIA -> LocalizedClinicalOverlay(
                diagnosisTitle = "Pneumonia Adquirida na Comunidade (Recuperação)",
                diagnosisSummary = "Transição de antibiótico venoso para amoxicilina-clavulanato oral. Monitorar SpO2, temperatura, hidratação e exercícios respiratórios.",
                redFlags = listOf(
                    "Saturação de oxigênio (SpO2) abaixo de 93%",
                    "Retorno da febre acima de 38,0 °C ou piora da tosse",
                    "Confusão mental nova, fadiga extrema ou respiração acelerada",
                    "Incapacidade de manter hidratação ou antibiótico oral"
                )
            )
        }

        AppLocale.ES -> when (caseType) {
            EpisodeCaseType.HEART_FAILURE -> LocalizedClinicalOverlay(
                diagnosisTitle = "Insuficiencia Cardíaca (ICFEr) Descompensada",
                diagnosisSummary = "Alta tras 4 días de diuresis IV por sobrecarga de volumen. Peso seco objetivo 76,5 kg; control diario de peso, PA y diurético.",
                redFlags = listOf(
                    "Aumento de peso de 2 kg o más en 2 días",
                    "Presión sistólica mayor a 160 mmHg o menor a 90 mmHg",
                    "Empeoramiento de la falta de aire en reposo o al acostarse",
                    "Dolor torácico, desmayo o SpO2 inferior al 92%"
                )
            )
            EpisodeCaseType.POST_SURGERY -> LocalizedClinicalOverlay(
                diagnosisTitle = "Postoperatorio de Colecistectomía Laparoscópica",
                diagnosisSummary = "Cirugía sin complicaciones con 4 incisiones. Vigilar heridas quirúrgicas, dolor, caminata suave y dieta baja en grasas.",
                redFlags = listOf(
                    "Fiebre superior a 38,0 °C o escalofríos",
                    "Enrojecimiento progresivo, calor o secreción en las incisiones",
                    "Dolor abdominal intenso (7/10 o superior)",
                    "Náuseas persistentes, vómitos o coloración amarillenta"
                )
            )
            EpisodeCaseType.PNEUMONIA -> LocalizedClinicalOverlay(
                diagnosisTitle = "Neumonía Adquirida en la Comunidad (Recuperación)",
                diagnosisSummary = "Paso a antibiótico oral (amoxicilina-clavulánico). Vigilar SpO2, temperatura, hidratación y ejercicios respiratorios.",
                redFlags = listOf(
                    "Saturación de oxígeno (SpO2) inferior al 93%",
                    "Fiebre recurrente superior a 38,0 °C o empeoramiento de la tos",
                    "Confusión nueva, fatiga intensa o respiración rápida",
                    "Intolerancia a líquidos o antibióticos orales"
                )
            )
        }

        AppLocale.FR -> when (caseType) {
            EpisodeCaseType.HEART_FAILURE -> LocalizedClinicalOverlay(
                diagnosisTitle = "Insuffisance Cardiaque (FEVG réduite) Décompensée",
                diagnosisSummary = "Sortie après 4 jours de diurétiques IV. Poids sec cible 76,5 kg ; suivi quotidien du poids, de la TA et du traitement diurétique.",
                redFlags = listOf(
                    "Prise de poids de 2 kg ou plus en 2 jours",
                    "Tension systolique supérieure à 160 mmHg ou inférieure à 90 mmHg",
                    "Essoufflement aggravé au repos ou besoin d\'oreillers supplémentaires",
                    "Douleur thoracique, malaise ou SpO2 inférieure à 92 %"
                )
            )
            EpisodeCaseType.POST_SURGERY -> LocalizedClinicalOverlay(
                diagnosisTitle = "Suivi Post-Opératoire de Cholécystectomie Cœlioscopique",
                diagnosisSummary = "Intervention sans complication (4 orifices). Surveiller les cicatrices, la douleur, la marche précoce et l\'alimentation légère.",
                redFlags = listOf(
                    "Fièvre supérieure à 38,0 °C ou frissons",
                    "Rougeur étendue, chaleur ou écoulement purulent sur une cicatrice",
                    "Douleur abdominale intense (7/10 ou plus)",
                    "Nausées persistantes, vomissements ou ictère"
                )
            )
            EpisodeCaseType.PNEUMONIA -> LocalizedClinicalOverlay(
                diagnosisTitle = "Pneumonie Communautaire (Convalescence)",
                diagnosisSummary = "Relais antibiotique oral par amoxicilline-acide clavulanique. Suivre la SpO2, la température, l\'hydratation et la kinésithérapie respiratoire.",
                redFlags = listOf(
                    "Saturation en oxygène (SpO2) inférieure à 93 %",
                    "Réapparition de fièvre > 38,0 °C ou aggravation de la toux",
                    "Confusion nouvelle, fatigue majeure ou essoufflement rapide",
                    "Impossibilité de boire ou de prendre l\'antibiotique oral"
                )
            )
        }

        AppLocale.EN, AppLocale.SYSTEM -> null
    }

    fun applyTo(episode: DischargeEpisode, locale: AppLocale): DischargeEpisode {
        val overlay = overlayFor(episode.caseType, locale) ?: return episode
        return episode.copy(
            diagnosisTitle = overlay.diagnosisTitle,
            diagnosisSummary = overlay.diagnosisSummary,
            redFlags = overlay.redFlags
        )
    }
}
