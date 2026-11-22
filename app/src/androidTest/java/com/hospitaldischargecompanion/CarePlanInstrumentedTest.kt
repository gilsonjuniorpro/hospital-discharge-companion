package com.hospitaldischargecompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarePlanInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun carePlanDisplaysProgressFiltersSkipDialogAndRevisionHistory() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("careplan", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_CARE_PLAN)
            composeRule.onNodeWithText("Care Plan Revision v1").assertIsDisplayed()
            composeRule.onNodeWithText("Progress:", substring = true).assertIsDisplayed()
            composeRule.onNodeWithText("Due").assertIsDisplayed()
            composeRule.onNodeWithText("Completed").assertIsDisplayed()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_CARE_PLAN, "Complete")
            composeRule.onAllNodesWithText("Complete").onFirst().performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_CARE_PLAN, "Skip")
            composeRule.onAllNodesWithText("Skip").onFirst().performClick()
            composeRule.waitForText("Skip Task With Reason")
            composeRule.onNodeWithText("Skip Task With Reason").assertIsDisplayed()
            composeRule.onNodeWithText("Cancel").performClick()

            composeRule.onNodeWithContentDescription("Care Plan Revision History").performClick()
            composeRule.waitForText("Care Plan Revision History")
            composeRule.onNodeWithText("Close").performClick()
        }
    }
}
