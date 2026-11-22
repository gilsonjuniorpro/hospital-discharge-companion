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
class MedicationsInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun medicationsDisplaysReconciliationFiltersAndSkipReasonPresets() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("medications", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_MEDICATIONS)
            composeRule.onNodeWithText("Adherence", substring = true).assertIsDisplayed()
            composeRule.onNodeWithText("Clinical Interaction & Therapy Warnings").assertIsDisplayed()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_MEDICATIONS, "Reconciliation:", substring = true)
            composeRule.onNodeWithText("Reconciliation:", substring = true).assertIsDisplayed()

            composeRule.onNodeWithText("Changed").performClick()
            composeRule.waitForText("Furosemide", substring = true)
            composeRule.onNodeWithText("Furosemide", substring = true).assertIsDisplayed()

            composeRule.onNodeWithText("All").performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_MEDICATIONS, "Take")
            composeRule.onAllNodesWithText("Take").onFirst().performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_MEDICATIONS, "Skip")
            composeRule.onAllNodesWithText("Skip").onFirst().performClick()
            composeRule.waitForText("Why was this dose skipped?")
            composeRule.onNodeWithText("Dizziness / lightheaded").assertIsDisplayed()
            composeRule.onNodeWithText("Cancel").performClick()
        }
    }
}
