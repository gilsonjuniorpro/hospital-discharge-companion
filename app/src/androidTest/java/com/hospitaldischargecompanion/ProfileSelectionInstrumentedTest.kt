package com.hospitaldischargecompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileSelectionInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun profileSelectionDisplaysSeededEpisodesLocalesAndNavigatesToPatientHome() {
        HdcInstrumentedHelpers.resetDemoViaUi(composeRule)

        ActivityScenario.launch<MainActivity>(HdcInstrumentedHelpers.screenIntent()).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_PROFILE)
            composeRule.onNodeWithTag(HdcTestTags.SCREEN_PROFILE).assertIsDisplayed()

            composeRule.onNodeWithText("English").assertIsDisplayed()
            composeRule.onNodeWithText("Português (BR)").assertIsDisplayed()
            composeRule.onNodeWithText("Español").assertIsDisplayed()
            composeRule.onNodeWithText("Français").assertIsDisplayed()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_PROFILE, "João Silva", substring = true)
            composeRule.onNodeWithText("João Silva", substring = true).assertIsDisplayed()
            composeRule.onNodeWithText("Maria Oliveira", substring = true).assertIsDisplayed()

            composeRule.scrollToTextIn(HdcTestTags.SCREEN_PROFILE, "Patient")
            composeRule.onAllNodesWithText("Patient").onFirst().performClick()

            composeRule.waitForTag(HdcTestTags.SCREEN_PATIENT)
            composeRule.onNodeWithText("Patient Recovery").assertIsDisplayed()
        }
    }
}
