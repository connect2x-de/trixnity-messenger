package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.configureTestLogging
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.HtmlGenerator.TagRenderer
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TestHtmlTagRenderer : TagRenderer {
    override fun openTag(
        node: ASTNode,
        tagName: CharSequence,
        vararg attributes: CharSequence?,
        autoClose: Boolean
    ): CharSequence = when (tagName) {
        // Recommended Tag Whitelist
        // https://spec.matrix.org/v1.13/client-server-api/#mroommessage-msgtypes
        "del", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "p", "a", "ul", "ol", "sup",
        "sub", "li", "b", "i", "u", "strong", "em",
        "s", "code", "hr", "br", "div", "table",
        "thead", "tbody", "tr", "th", "td", "caption",
        "pre", "span", "img", "details", "summary" ->
            buildString {
                append("<$tagName")
                attributes.forEach { attribute ->
                    if (attribute != null) {
                        append(" $attribute")
                    }
                }

                if (autoClose) {
                    append(" />")
                } else {
                    append(">")
                }
            }

        else -> ""
    }

    override fun closeTag(tagName: CharSequence): CharSequence = if (tagName == "body") "" else "</$tagName>"

    override fun printHtml(html: CharSequence): CharSequence = html
}

class MatrixMarkdownFlavourTest {
    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `a single linebreak leads to newline tag`() {
        val markdownFlavourDescriptor = MatrixMarkdownFlavourImpl()
        val markdownParser = MarkdownParser(markdownFlavourDescriptor)

        val input = "test\ntest"
        val output = HtmlGenerator(
            input,
            markdownParser.buildMarkdownTreeFromString(input),
            markdownFlavourDescriptor
        ).generateHtml(TestHtmlTagRenderer())

        assertEquals("<p>test\n<br />test</p>", output)
    }

    @Test
    fun `two linebreaks result in new paragraph`() {
        val markdownFlavourDescriptor = MatrixMarkdownFlavourImpl()
        val markdownParser = MarkdownParser(markdownFlavourDescriptor)

        val input = "test\n\ntest"
        val output = HtmlGenerator(
            input,
            markdownParser.buildMarkdownTreeFromString(input),
            markdownFlavourDescriptor
        ).generateHtml(TestHtmlTagRenderer())

        assertEquals("<p>test</p><p>test</p>", output)
    }

    @Test
    fun `three linebreaks result in no additional tags`() {
        val markdownFlavourDescriptor = MatrixMarkdownFlavourImpl()
        val markdownParser = MarkdownParser(markdownFlavourDescriptor)

        val input = "test\n\n\ntest"
        val output = HtmlGenerator(
            input,
            markdownParser.buildMarkdownTreeFromString(input),
            markdownFlavourDescriptor
        ).generateHtml(TestHtmlTagRenderer())

        assertEquals("<p>test</p><p>test</p>", output)
    }
}
