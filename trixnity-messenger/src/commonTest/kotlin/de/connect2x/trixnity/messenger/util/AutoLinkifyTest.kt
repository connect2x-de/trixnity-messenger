package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.util.html.AutoLinkifyVisitor
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.util.html.MatrixMentionVisitor
import io.ktor.http.*
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.UserId
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoLinkifyTest {
    private fun parseLinks(content: String): HtmlNode.HtmlElement =
        AutoLinkifyVisitor.process(
            HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.TextContent(content)
                )
            )
        )

    @Test
    fun linkifiesLinksInText() {
        assertEquals(
            expected = HtmlNode.HtmlElement("#root", emptyMap(), listOf(
                HtmlNode.HtmlElement("span", emptyMap(), listOf(
                    HtmlNode.TextContent("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "),
                    HtmlNode.HtmlElement("a", mapOf(
                        "href" to "https://example.com/",
                    ), listOf(
                        HtmlNode.TextContent("https://example.com/"),
                    )),
                    HtmlNode.TextContent(" Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."),
                )),
            )),
            actual = parseLinks("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. https://example.com/ Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."),
        )
    }
    // Mentions
    @Test
    fun shouldReplaceBasicUserMention() {
        assertEquals(
            expected = HtmlNode.HtmlElement("#root", emptyMap(), listOf(
                HtmlNode.HtmlElement("span", emptyMap(), listOf(
                    HtmlNode.TextContent("Hallo "),
                    HtmlNode.HtmlElement("a", mapOf(
                        "href" to "matrix:u/user:acme.com",
                    ), listOf(
                        HtmlNode.TextContent("@user:acme.com"),
                    )),
                    HtmlNode.TextContent("! How are you?"),
                )),
            )),
            actual = parseLinks("Hallo @user:acme.com! How are you?"),
        )
    }

    @Test
    fun shouldReplaceBasicRoomAliasMention() {
        assertEquals(
            expected = HtmlNode.HtmlElement("#root", emptyMap(), listOf(
                HtmlNode.HtmlElement("span", emptyMap(), listOf(
                    HtmlNode.TextContent("Hallo "),
                    HtmlNode.HtmlElement("a", mapOf(
                        "href" to "matrix:r/awesome-room:acme.com",
                    ), listOf(
                        HtmlNode.TextContent("#awesome-room:acme.com"),
                    )),
                    HtmlNode.TextContent("! How are you?"),
                )),
            )),
            actual = parseLinks("Hallo #awesome-room:acme.com! How are you?"),
        )
    }

    @Test
    fun shouldReplaceUriUserMention() {
        assertEquals(
            expected = HtmlNode.HtmlElement("#root", emptyMap(), listOf(
                HtmlNode.HtmlElement("span", emptyMap(), listOf(
                    HtmlNode.TextContent("Hallo "),
                    HtmlNode.HtmlElement("a", mapOf(
                        "href" to "matrix:u/user:acme.com?action=chat",
                    ), listOf(
                        HtmlNode.TextContent("@user:acme.com"),
                    )),
                    HtmlNode.TextContent("! How are you?"),
                )),
            )),
            actual = parseLinks("Hallo matrix:u/user:acme.com?action=chat! How are you?"),
        )
    }

    @Test
    fun shouldReplaceLinkUserMention() {
        assertEquals(
            expected = HtmlNode.HtmlElement("#root", emptyMap(), listOf(
                HtmlNode.HtmlElement("span", emptyMap(), listOf(
                    HtmlNode.TextContent("Hallo "),
                    HtmlNode.HtmlElement("a", mapOf(
                        "href" to "https://matrix.to/#/%40alice%3Aexample.org",
                    ), listOf(
                        HtmlNode.TextContent("@alice:example.org"),
                    )),
                    HtmlNode.TextContent("! How are you?"),
                )),
            )),
            actual = parseLinks("Hallo https://matrix.to/#/%40alice%3Aexample.org! How are you?"),
        )
    }

    @Test
    fun shouldReplaceMultipleMentions() {
        assertEquals(
            expected = HtmlNode.HtmlElement("#root", emptyMap(), listOf(
                HtmlNode.HtmlElement("span", emptyMap(), listOf(
                    HtmlNode.TextContent("Hallo "),
                    HtmlNode.HtmlElement("a", mapOf(
                        "href" to "matrix:u/user:acme.com",
                    ), listOf(
                        HtmlNode.TextContent("@user:acme.com"),
                    )),
                    HtmlNode.TextContent("! How are you? Want to meet up with "),
                    HtmlNode.HtmlElement("a", mapOf(
                        "href" to "matrix:u/user2:acme.com",
                    ), listOf(
                        HtmlNode.TextContent("@user2:acme.com"),
                    )),
                    HtmlNode.TextContent("?"),
                )),
            )),
            actual = parseLinks("Hallo @user:acme.com! How are you? Want to meet up with @user2:acme.com?"),
        )
    }

    // Links
    @Test
    fun shouldFormatLinkExcludingWhitespaceInTheEnd() {
        listOf("\r", "\n", "\t", " `").forEach {
            assertEquals(
                expected = HtmlNode.HtmlElement(
                    "#root", emptyMap(), listOf(
                        HtmlNode.HtmlElement(
                            "span", emptyMap(), listOf(
                                HtmlNode.TextContent("Hello, take a look at "),
                                HtmlNode.HtmlElement(
                                    "a", mapOf(
                                        "href" to "https://tammy.connect2x.de/en-us/",
                                    ), listOf(
                                        HtmlNode.TextContent("https://tammy.connect2x.de/en-us/"),
                                    )
                                ),
                                HtmlNode.TextContent("${it}cool isn't it?"),
                            )
                        ),
                    )
                ),
                actual = parseLinks("Hello, take a look at https://tammy.connect2x.de/en-us/${it}cool isn't it?"),
            )
        }
    }
    @Test
    fun shouldFormatRegularLink() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://matrix.org",
                                ), listOf(
                                    HtmlNode.TextContent("https://matrix.org"),
                                )
                            ),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("https://matrix.org"),
        )
    }

    @Test
    fun shouldFormatUrlWithEscapedAmpersand() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://duckduckgo.com/?q=html+escaping+ampersand&amp;ia=web",
                                ), listOf(
                                    HtmlNode.TextContent("https://duckduckgo.com/?q=html+escaping+ampersand&amp;ia=web"),
                                )
                            ),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("https://duckduckgo.com/?q=html+escaping+ampersand&amp;ia=web"),
        )
    }

    @Test
    fun shouldFormatUrlWithSemicolon() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://exampleformytest.com/bla;blubb",
                                ), listOf(
                                    HtmlNode.TextContent("https://exampleformytest.com/bla;blubb"),
                                )
                            ),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("https://exampleformytest.com/bla;blubb"),
        )
    }

    @Test
    fun shouldFormatUrlWithComma() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://osmand.net/map/?pin=50.89774,13.69089#19/51.05483/13.74711",
                                ), listOf(
                                    HtmlNode.TextContent("https://osmand.net/map/?pin=50.89774,13.69089#19/51.05483/13.74711"),
                                )
                            ),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("https://osmand.net/map/?pin=50.89774,13.69089#19/51.05483/13.74711"),
        )
    }

    @Test
    fun shouldIgnoreExclamationMarkAtEndOfLink() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.TextContent("I think you could really like "),
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://graphemica.com/",
                                ), listOf(
                                    HtmlNode.TextContent("https://graphemica.com/"),
                                )
                            ),
                            HtmlNode.TextContent("!"),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("I think you could really like https://graphemica.com/!"),
        )
    }

    @Test
    fun shouldIgnoreParenthesis() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.TextContent("The website Graphemica ("),
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://graphemica.com/",
                                ), listOf(
                                    HtmlNode.TextContent("https://graphemica.com/"),
                                )
                            ),
                            HtmlNode.TextContent(") can display any sort of symbol."),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("The website Graphemica (https://graphemica.com/) can display any sort of symbol."),
        )
    }

    @Test
    fun shouldIgnoreColonAtTheEndOfLink() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.TextContent("The best thing about "),
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://graphemica.com/",
                                ), listOf(
                                    HtmlNode.TextContent("https://graphemica.com/"),
                                )
                            ),
                            HtmlNode.TextContent(": It has support for unicode characters."),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("The best thing about https://graphemica.com/: It has support for unicode characters."),
        )
    }

    @Test
    fun shouldIgnoreQuestionMarkAtTheEndOfLink() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.TextContent("Do you know "),
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://graphemica.com/",
                                ), listOf(
                                    HtmlNode.TextContent("https://graphemica.com/"),
                                )
                            ),
                            HtmlNode.TextContent("?"),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("Do you know https://graphemica.com/?"),
        )
    }

    @Test
    fun shouldIgnorePeriodAtTheEndOfLink() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.TextContent("I thought about "),
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://graphemica.com",
                                ), listOf(
                                    HtmlNode.TextContent("https://graphemica.com"),
                                )
                            ),
                            HtmlNode.TextContent("."),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("I thought about https://graphemica.com."),
        )
    }

    @Test
    fun shouldAllowUnicodeSymbols() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://graphemica.com/»",
                                ), listOf(
                                    HtmlNode.TextContent("https://graphemica.com/»"),
                                )
                            ),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("https://graphemica.com/»"),
        )
    }

    @Test
    fun shouldAllowParenthesisInUrls() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.TextContent("Have you heard about "),
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://en.wikipedia.org/wiki/Matrix_(protocol)",
                                ), listOf(
                                    HtmlNode.TextContent("https://en.wikipedia.org/wiki/Matrix_(protocol)"),
                                )
                            ),
                            HtmlNode.TextContent(" yet?"),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("Have you heard about https://en.wikipedia.org/wiki/Matrix_(protocol) yet?"),
        )
    }

    @Test
    fun shouldRemovePunctuationAroundUrlsWithParenthesises() {
        assertEquals(
            expected = HtmlNode.HtmlElement(
                "#root", emptyMap(), listOf(
                    HtmlNode.HtmlElement(
                        "span", emptyMap(), listOf(
                            HtmlNode.TextContent("Have you heard about Matrix ("),
                            HtmlNode.HtmlElement(
                                "a", mapOf(
                                    "href" to "https://en.wikipedia.org/wiki/Matrix_(protocol)",
                                ), listOf(
                                    HtmlNode.TextContent("https://en.wikipedia.org/wiki/Matrix_(protocol)"),
                                )
                            ),
                            HtmlNode.TextContent(") yet?"),
                        ),
                    ),
                ),
            ),
            actual = parseLinks("Have you heard about Matrix (https://en.wikipedia.org/wiki/Matrix_(protocol)) yet?"),
        )
    }
}
