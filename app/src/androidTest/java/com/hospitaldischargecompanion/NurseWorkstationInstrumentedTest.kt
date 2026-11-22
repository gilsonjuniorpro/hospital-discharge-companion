package com.hospitaldischargecompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NurseWorkstationInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun nurseWorkstationExportsHandoffFiltersCensusAndOpensNoteTemplates() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("nurse")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_NURSE)
            composeRule.onNodeWithText("Nurse Triage Hub").assertIsDisplayed()
            composeRule.onNodeWithText("Shift Handoff Summary").assertIsDisplayed()

            composeRule.onNodeWithText("Export Handoff").performClick()
            composeRule.waitForText("Nursing Shift Handoff Report")
            composeRule.onNodeWithText("Close").performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_NURSE, "Acknowledge")
            composeRule.onAllNodesWithText("Acknowledge").onFirst().performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_NURSE, "Active Discharge Census")
            composeRule.onAllNodesWithText("High Risk").onFirst().performClick()
            composeRule.waitForText("João Silva", substring = true)

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_NURSE, "Note Templates")
            composeRule.onAllNodesWithText("Note Templates").onFirst().performClick()
            composeRule.waitForText("Document Nursing Clinical Note")
            composeRule.onNodeWithText("Cancel").performClick()
        }
    }
}
