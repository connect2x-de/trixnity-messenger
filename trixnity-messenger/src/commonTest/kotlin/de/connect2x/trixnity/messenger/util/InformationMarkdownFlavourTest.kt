package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.configureTestLogging
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class InformationMarkdownFlavourTest {

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `a backslash at the end of a line leads to newline tag`() {
        val markdownFlavourDescriptor = InformationMarkdownFlavourImpl()
        val markdownParser = MarkdownParser(markdownFlavourDescriptor)

        val input = """test\
            |test""".trimMargin()
        val output = HtmlGenerator(
            input,
            markdownParser.buildMarkdownTreeFromString(input),
            markdownFlavourDescriptor
        ).generateHtml(TestHtmlTagRenderer())

        assertEquals("<p>test<br />\ntest</p>", output)
    }
}
