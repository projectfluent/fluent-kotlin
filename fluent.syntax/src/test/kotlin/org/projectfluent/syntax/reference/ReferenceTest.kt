package org.projectfluent.syntax.reference

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.projectfluent.syntax.parser.FluentParser
import java.io.File
import java.nio.file.Paths

val DISABLED = arrayOf(
    "leading_dots.ftl",
    ""
)

internal class ReferenceTest {
    @TestFactory
    fun references(): Iterable<DynamicTest> {
        val reference_tests: MutableList<DynamicTest> = mutableListOf()
        val referencedir = Paths.get("src", "test", "resources", "reference_fixtures")
        for (entry in referencedir.toFile().walk()) {
            if (entry.extension == "ftl" && ! DISABLED.contains(entry.name)) {
                val reftest = DynamicTest.dynamicTest(entry.name) {
                    this.compare_reference(entry)
                }
                reference_tests.add(reftest)
            }
        }
        return reference_tests
    }

    fun compare_reference(ftl_file: File) {
        val json_file = File(ftl_file.path.replace(".ftl", ".json"))
        val ref_content = ftl_file.readText()
        val parser = FluentParser()
        val resource = parser.parse(ref_content)
        val ref = Parser.default().parse(json_file.path) as JsonObject
        assertAstEquals(ref, resource)
    }
}
