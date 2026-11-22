package com.hospitaldischargecompanion.core.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringResourceParityTest {

    private fun extractKeys(relativeResPath: String): Set<String> {
        val candidates = listOf(
            File("src/main/res/$relativeResPath"),
            File("app/src/main/res/$relativeResPath")
        )
        val xmlFile = candidates.firstOrNull { it.exists() }
            ?: error("Could not locate resource file $relativeResPath from ${File(".").absolutePath}")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until nodes.length) {
            val name = nodes.item(i).attributes.getNamedItem("name")?.nodeValue
            if (!name.isNullOrBlank()) keys.add(name)
        }
        return keys
    }

    @Test
    fun allFourLocalesContainExactSameStringResourceKeys() {
        val enKeys = extractKeys("values/strings.xml")
        assertThat(enKeys).isNotEmpty()

        val locales = listOf(
            "values-pt-rBR/strings.xml",
            "values-es/strings.xml",
            "values-fr/strings.xml"
        )
        locales.forEach { path ->
            val localeKeys = extractKeys(path)
            assertThat(localeKeys).containsExactlyElementsIn(enKeys)
        }
    }
}
