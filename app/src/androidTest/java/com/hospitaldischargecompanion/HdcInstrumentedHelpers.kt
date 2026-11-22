package com.hospitaldischargecompanion

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.hospitaldischargecompanion.core.ui.HdcTestTags

object HdcInstrumentedHelpers {

    fun screenIntent(slug: String? = null, episodeId: String? = null): Intent {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return Intent(context, MainActivity::class.java).apply {
            if (slug != null) {
                action = Intent.ACTION_VIEW
                val uri = if (episodeId != null) {
                    "hdc://screen/$slug/$episodeId"
                } else {
                    "hdc://screen/$slug"
                }
                data = Uri.parse(uri)
            }
        }
    }

    fun resetDemoViaUi(composeRule: ComposeTestRule) {
        ActivityScenario.launch<MainActivity>(screenIntent("demo")).use {
            composeRule.waitForTag(HdcTestTags.SCREEN_DEMO_CONTROLS)
            composeRule.waitForText("Reset Demo")
            composeRule.onAllNodesWithText("Reset Demo").onFirst().performClick()
            composeRule.waitForText("Demonstration reset", substring = true)
        }
    }
}

fun ComposeTestRule.waitForTag(tag: String, timeoutMs: Long = 10_000L) {
    waitUntil(timeoutMillis = timeoutMs) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeTestRule.waitForText(
    text: String,
    substring: Boolean = false,
    timeoutMs: Long = 10_000L
) {
    waitUntil(timeoutMillis = timeoutMs) {
        onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeTestRule.scrollToTextIn(
    screenTag: String,
    text: String,
    substring: Boolean = false
) {
    onNodeWithTag(screenTag).performScrollToNode(hasText(text, substring = substring))
}
