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
class TelemonitoringAndAlertsInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun abnormalBpCheckInTriggersAlertAndOpensEscalationLadder() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("telemonitoring", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_TELEMONITORING)

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_TELEMONITORING, "Simulate High BP")
            composeRule.onNodeWithText("Simulate High BP").performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_TELEMONITORING, "Submit Check-In")
            composeRule.onNodeWithText("Submit Check-In").performClick()

            composeRule.onNodeWithTag(HdcTestTags.SCREEN_TELEMONITORING).performScrollToIndex(0)
            composeRule.waitForText("Clinical Alert(s) Triggered", substring = true)
            composeRule.onNodeWithText("Clinical Alert(s) Triggered", substring = true).performClick()

            composeRule.waitForTag(HdcTestTags.SCREEN_ALERTS)
            composeRule.onNodeWithText("Active").assertIsDisplayed()
            composeRule.onNodeWithText("Escalated").assertIsDisplayed()
            composeRule.onAllNodesWithText("Family").onFirst().assertIsDisplayed()
            composeRule.onAllNodesWithText("Nurse").onFirst().assertIsDisplayed()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_ALERTS, "Escalate")
            composeRule.onAllNodesWithText("Escalate").onFirst().performClick()
        }
    }
}
