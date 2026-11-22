package com.hospitaldischargecompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoordinationModulesInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun messagingFiltersClinicalNotesAndSendsMessage() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("messaging", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_MESSAGING)
            composeRule.onNodeWithText("Clinical Notes Only").performClick()
            composeRule.onNodeWithText("Clinical Notes Only").performClick()

            composeRule.onNodeWithText("Write a message to the care team").performTextInput("Morning weight stable")
            composeRule.onNodeWithContentDescription("Send").performClick()

            composeRule.waitForText("Morning weight stable")
            composeRule.onNodeWithText("Morning weight stable").assertIsDisplayed()
        }
    }

    @Test
    fun appointmentsOpensCustomRescheduleDialogAndSubmitsSlot() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("appointments", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_APPOINTMENTS)
            composeRule.onAllNodesWithText("Reschedule").onFirst().performClick()

            composeRule.waitForText("Request Appointment Reschedule")
            composeRule.onNodeWithText("Tomorrow Morning · 08:30").performClick()
            composeRule.onAllNodesWithText("Reschedule").onLast().performClick()

            composeRule.waitForText("Requested reschedule slot: Tomorrow Morning · 08:30")
            composeRule.onNodeWithText("Requested reschedule slot: Tomorrow Morning · 08:30").assertIsDisplayed()
        }
    }

    @Test
    fun educationTogglesClinicianDetailAndMarksGuideCompleted() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("education", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_EDUCATION)
            composeRule.onNodeWithText("discharge guides", substring = true).assertIsDisplayed()

            composeRule.onNodeWithText("Show Clinician Detail View").performClick()
            composeRule.scrollToTextIn(HdcTestTags.SCREEN_EDUCATION, "Clinical Reference Detail")
            composeRule.onAllNodesWithText("Clinical Reference Detail").onFirst().assertIsDisplayed()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_EDUCATION, "Mark Completed")
            composeRule.onAllNodesWithText("Mark Completed").onFirst().performClick()
        }
    }
}
