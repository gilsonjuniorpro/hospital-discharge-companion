package com.hospitaldischargecompanion.core.ehr

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.medication.MedicationSeedFactory
import com.hospitaldischargecompanion.core.telemonitor.TelemonitorSeedFactory
import org.junit.Test

class FhirBundleSerializerTest {

    @Test
    fun serializesMockFhirBundleWithAllExpectedClinicalResources() {
        val ep = ClinicalSeedFactory.createEpisodes().first { it.id == ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID }
        val meds = MedicationSeedFactory.createMedications().filter { it.episodeId == ep.id }
        val latest = TelemonitorSeedFactory.createCheckIns().filter { it.episodeId == ep.id }.last()

        val bundle = FhirBundleSerializer.buildBundle(ep, meds, latest)
        assertThat(bundle.resourceType).isEqualTo("Bundle")
        assertThat(bundle.patient.resourceType).isEqualTo("Patient")
        assertThat(bundle.encounter.resourceType).isEqualTo("Encounter")
        assertThat(bundle.carePlan.resourceType).isEqualTo("CarePlan")
        assertThat(bundle.medications).hasSize(4)
        assertThat(bundle.latestObservation?.resourceType).isEqualTo("Observation")

        val json = FhirBundleSerializer.toPrettyJson(ep, meds, latest)
        assertThat(json).contains("\"resourceType\": \"Bundle\"")
        assertThat(json).contains("Furosemide 40 mg tablet")
        assertThat(json).contains("BP 142/86 mmHg")
    }
}
