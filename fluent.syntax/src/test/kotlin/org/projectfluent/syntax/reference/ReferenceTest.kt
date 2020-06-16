package org.projectfluent.syntax.reference

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.projectfluent.syntax.parser.FluentParser
import java.io.File
import java.nio.file.Paths

internal open class ReferenceTest {
    open val fixture = "reference_fixtures"
    open val DISABLED = arrayOf(
        "leading_dots.ftl",
        ""
    )
    @TestFactory
    open fun references(): Iterable<DynamicTest> {
        val reference_tests: MutableList<DynamicTest> = mutableListOf()
        val referencedir = Paths.get("src", "test", "resources", fixture)
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

internal class StructureTest : ReferenceTest() {
    override val fixture = "structure_fixtures"
    override val DISABLED = arrayOf(
        ""
    )
}
