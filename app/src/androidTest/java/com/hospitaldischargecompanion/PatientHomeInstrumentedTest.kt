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
class PatientHomeInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun patientHomeSwitchesSeededEpisodesAndNavigatesToCarePlan() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("patient", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_PATIENT)
            composeRule.onNodeWithText("Heart Failure (HFrEF) Exacerbation").assertIsDisplayed()
            composeRule.onNodeWithText("Care Circle Code: HDC-HF-402").assertIsDisplayed()

            composeRule.onNodeWithText("Maria Oliveira").performClick()
            composeRule.waitForText("Post-Operative Laparoscopic Cholecystectomy")
            composeRule.onNodeWithText("Post-Operative Laparoscopic Cholecystectomy").assertIsDisplayed()

            composeRule.onNodeWithText("João Silva").performClick()
            composeRule.waitForText("Heart Failure (HFrEF) Exacerbation")

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_PATIENT, "Care Plan")
            composeRule.onAllNodesWithText("Care Plan").onFirst().performClick()

            composeRule.waitForTag(HdcTestTags.SCREEN_CARE_PLAN)
        }
    }
}
