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
class FamilyCaregiverInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun familyHubDisplaysConsentBadgeEscalatesAndOpensInviteDialog() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent("family", "ep-hf-001")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_FAMILY)
            composeRule.onNodeWithText("Family Caregiver Hub").assertIsDisplayed()
            composeRule.onNodeWithText("Consent Verified").assertIsDisplayed()

            composeRule.onNodeWithText("Escalate to Nurse").performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_FAMILY, "Confirm")
            composeRule.onAllNodesWithText("Confirm").onFirst().performClick()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_FAMILY, "Invite Caregiver")
            composeRule.onNodeWithText("Invite Caregiver").performClick()
            composeRule.waitForText("Invite Family Caregiver")
            composeRule.onNodeWithText("Invite Family Caregiver").assertIsDisplayed()
            composeRule.onNodeWithText("Cancel").performClick()
        }
    }
}
