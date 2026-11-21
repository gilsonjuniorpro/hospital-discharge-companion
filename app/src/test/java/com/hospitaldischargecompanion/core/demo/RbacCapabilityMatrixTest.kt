package com.hospitaldischargecompanion.core.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RbacCapabilityMatrixTest {

    @Test
    fun roleCountMatchesFourDemoPersonas() {
        assertEquals(
            setOf(UserRole.PATIENT, UserRole.FAMILY, UserRole.NURSE, UserRole.PHYSICIAN),
            UserRole.entries.toSet()
        )
    }

    @Test
    fun patientHasNonClinicianReadAndSelfCareCapabilitiesOnly() {
        assertFalse(UserRole.PATIENT.isClinician)
        assertFalse(UserRole.PATIENT.canModifyCarePlan)
        assertFalse(UserRole.PATIENT.canReopenTasks)
        assertFalse(UserRole.PATIENT.canWriteClinicalNotes)
    }

    @Test
    fun familyCaregiverCannotPerformClinicianActions() {
        assertFalse(UserRole.FAMILY.isClinician)
        assertFalse(UserRole.FAMILY.canModifyCarePlan)
        assertFalse(UserRole.FAMILY.canReopenTasks)
        assertFalse(UserRole.FAMILY.canWriteClinicalNotes)
    }

    @Test
    fun nurseCanReopenTasksAndWriteNotesButCannotSignCarePlanAmendments() {
        assertTrue(UserRole.NURSE.isClinician)
        assertFalse(UserRole.NURSE.canModifyCarePlan)
        assertTrue(UserRole.NURSE.canReopenTasks)
        assertTrue(UserRole.NURSE.canWriteClinicalNotes)
    }

    @Test
    fun physicianHasFullClinicianAndCarePlanAmendmentCapabilities() {
        assertTrue(UserRole.PHYSICIAN.isClinician)
        assertTrue(UserRole.PHYSICIAN.canModifyCarePlan)
        assertTrue(UserRole.PHYSICIAN.canReopenTasks)
        assertTrue(UserRole.PHYSICIAN.canWriteClinicalNotes)
    }
}
