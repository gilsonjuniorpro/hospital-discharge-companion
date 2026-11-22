package com.hospitaldischargecompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EhrSyncAndDemoControlsInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun ehrSyncPreviewsFhirBundleAndReconciles409Conflict() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("ehr", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_EHR_SYNC)

            composeRule.onNodeWithText("Inspect FHIR Bundle").performClick()
            composeRule.waitForText("Mock FHIR R4 Bundle Payload")
            composeRule.onNodeWithText("\"resourceType\": \"Bundle\"", substring = true).assertIsDisplayed()
            composeRule.onNodeWithText("Close").performClick()

            composeRule.onNodeWithText("Conflict (409)").performClick()
            composeRule.onNodeWithText("Push Vitals & Adherence").performClick()

            composeRule.waitForText("Keep Local Home Data")
            composeRule.scrollToTextIn(HdcTestTags.SCREEN_EHR_SYNC, "Keep Local Home Data")
            composeRule.onAllNodesWithText("Keep Local Home Data").onFirst().performClick()
        }
    }

    @Test
    fun demoControlsAppliesScenarioTogglesThemeAndDisplaysDisclaimer() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("demo")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_DEMO_CONTROLS)

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_DEMO_CONTROLS, "Full Adherence")
            composeRule.onNodeWithText("Full Adherence").performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_DEMO_CONTROLS, "Theme Mode")
            composeRule.onNodeWithText("Dark").performClick()
            composeRule.onNodeWithText("Light").performClick()
            composeRule.onNodeWithText("System").performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_DEMO_CONTROLS, "Educational demo only", substring = true)
            composeRule.onNodeWithText("Educational demo only", substring = true).assertIsDisplayed()

            composeRule.onNodeWithTag(HdcTestTags.SCREEN_DEMO_CONTROLS).performScrollToIndex(0)
        }
    }
}
