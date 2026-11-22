package com.hospitaldischargecompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhysicianWorkstationInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun physicianWorkstationDisplaysClinicalSheetAndSignsOffRevisionV2() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("physician")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_PHYSICIAN)
            composeRule.onNodeWithText("Physician Escalations").assertIsDisplayed()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_PHYSICIAN, "Discharge Medication Summary")
            composeRule.onNodeWithText("Discharge Medication Summary").assertIsDisplayed()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_PHYSICIAN, "Earlier Follow-Up")
            composeRule.onNodeWithText("Earlier Follow-Up").performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_PHYSICIAN, "Sign Off New Care Plan Revision")
            composeRule.onNodeWithText("Sign Off New Care Plan Revision").performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_PHYSICIAN, "Care Plan Revision v2")
            composeRule.waitForText("Care Plan Revision v2")
            composeRule.onNodeWithText("Care Plan Revision v2").assertIsDisplayed()
        }
    }
}
