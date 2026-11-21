package com.hospitaldischargecompanion.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HdcTestTagsUniquenessTest {

    @Test
    fun allFourteenScreenTagsAreUniqueAndWellFormed() {
        val tags = HdcTestTags.allScreenTags()
        assertEquals(14, tags.size)
        assertEquals(tags.size, tags.toSet().size)
        tags.forEach { tag ->
            assertTrue("Tag must start with screen_: $tag", tag.startsWith("screen_"))
            assertTrue("Tag must be lowercase snake_case: $tag", tag.matches(Regex("^[a-z0-9_]+$")))
        }
    }
}
