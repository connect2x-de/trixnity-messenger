package de.connect2x.trixnity.messenger.compose.view.richtext

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.util.html.HtmlVisitor
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InlineImageTest {
    @Test
    fun imagesStayInlineWithSurroundingText() {
        val document = HtmlVisitor.process("before <img src='mxc://example.org/image' alt='cat'> after")
        val span = RichTextVisitor.process(document).children.single() as RichText.InlineSpan
        val context = InlineRichTextContext(mentions = null)
        val rendered = renderInlineContent(span, context, true)
        val text = rendered.text
        assertEquals("before cat after", text.text)
        val annotation = text.getStringAnnotations(INLINE_CONTENT_TAG, 0, text.length).single()
        assertEquals("cat", text.text.substring(annotation.start, annotation.end))
        assertEquals("mxc://example.org/image", rendered.images.getValue(annotation.item).attributes["src"])
    }

    @Test
    fun imagesWithoutAltTextStillHavePlaceholders() {
        val document =
            HtmlVisitor.process("<img src='mxc://example.org/image' alt=' '><img src='mxc://example.org/image'>")
        val span = RichTextVisitor.process(document).children.single() as RichText.InlineSpan
        val rendered = renderInlineContent(span, InlineRichTextContext(mentions = null), true)
        assertEquals("\uFFFC\uFFFC", rendered.text.text)
        assertEquals(2, rendered.images.size)
        assertEquals(2, rendered.text.getStringAnnotations(INLINE_CONTENT_TAG, 0, rendered.text.length).size)
    }

    @Test
    fun imagesUseTextFallbackWhenImageRenderingIsDisabled() {
        val document = HtmlVisitor.process("<img src='mxc://example.org/image' alt='cat'>")
        val span = RichTextVisitor.process(document).children.single() as RichText.InlineSpan
        val rendered = renderInlineContent(span, InlineRichTextContext(mentions = null), true, allowImages = false)
        assertEquals("cat", rendered.text.text)
        assertTrue(rendered.images.isEmpty())
    }

    @Test
    fun imagesInsideMentionsDoNotCreateOverlappingPlaceholders() {
        val href = "https://matrix.to/#/@alice:example.org"
        val document = HtmlVisitor.process("<a href='$href'><img src='mxc://example.org/image' alt='Alice'></a>")
        val span = RichTextVisitor.process(document).children.single() as RichText.InlineSpan
        val mention = TimelineElementMention.User(UserInfoElement(UserId("@alice:example.org"), "Alice", "A"))
        val rendered = renderInlineContent(span, InlineRichTextContext(mentions = mapOf(href to mention)), true)
        assertEquals("Alice", rendered.text.text)
        assertTrue(rendered.images.isEmpty())
        assertEquals(
            href,
            rendered.text.getStringAnnotations(INLINE_CONTENT_TAG, 0, rendered.text.length).single().item,
        )
    }

    @Test
    fun imageAltTextRemainsAvailableWhenThereIsNoDecodedPlaceholder() {
        val alt = "A long accessible description of the image"
        val document = HtmlVisitor.process("before <img src='mxc://example.org/image' alt='$alt'> after")
        val span = RichTextVisitor.process(document).children.single() as RichText.InlineSpan
        val rendered = renderInlineContent(span, InlineRichTextContext(mentions = null), true)
        assertEquals("before $alt after", rendered.text.text)
    }

    @Test
    fun imageAttributesAreRestrictedToTheMatrixAllowlist() {
        val document =
            HtmlVisitor.process(
                "<img src='mxc://example.org/image' width='64' height='32' alt='cat' title='cat_wave' data-mx-emoticon='false' style='width:9999px' onerror='alert(1)' srcset='https://example.org/image 2x'>"
            )
        val span = RichTextVisitor.process(document).children.single() as RichText.InlineSpan
        val rendered = renderInlineContent(span, InlineRichTextContext(mentions = null), true)
        assertEquals(
            mapOf(
                "src" to "mxc://example.org/image",
                "width" to "64",
                "height" to "32",
                "alt" to "cat",
                "title" to "cat_wave",
            ),
            rendered.images.values.single().attributes,
        )
    }

    @Test
    fun externalImagesUseAltText() {
        val document = HtmlVisitor.process("<img src='https://example.org/image' alt='external'> tail")
        val span = RichTextVisitor.process(document).children.single() as RichText.InlineSpan
        val rendered = renderInlineContent(span, InlineRichTextContext(mentions = null), true)
        assertEquals("external tail", rendered.text.text)
        assertTrue(rendered.images.isEmpty())
        assertTrue(rendered.text.getStringAnnotations(INLINE_CONTENT_TAG, 0, rendered.text.length).isEmpty())
    }

    @Test
    fun imagesInsideSpoilersUseAltText() {
        val document =
            HtmlVisitor.process("<span data-mx-spoiler><img src='mxc://example.org/image' alt='secret'></span> tail")
        val span = RichTextVisitor.process(document).children.single() as RichText.InlineSpan
        val rendered = renderInlineContent(span, InlineRichTextContext(mentions = null), true)
        assertEquals("secret tail", rendered.text.text)
        assertTrue(rendered.images.isEmpty())
        assertTrue(rendered.text.getStringAnnotations(INLINE_CONTENT_TAG, 0, rendered.text.length).isEmpty())
    }
}
