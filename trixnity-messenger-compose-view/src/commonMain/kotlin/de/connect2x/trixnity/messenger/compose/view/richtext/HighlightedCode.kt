package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.richtext.HighlightedCodeKt")

@Immutable internal data class HighlightedCode(val language: String, val content: AnnotatedString)

@Composable
internal fun rememberHighlightedCode(node: RichText.Block): HighlightedCode? {
    val i18n = DI.current.get<I18nView>()
    val inlineSpan = remember(node) { node.children.singleOrNull() as? RichText.InlineSpan }
    val inlineBlock = remember(inlineSpan) { inlineSpan?.children?.singleOrNull() as? RichText.Inline.Block }
    val language =
        remember(inlineBlock) {
            inlineBlock
                ?.attributes["class"]
                ?.split(" ")
                ?.firstOrNull { it.startsWith("language-") }
                ?.removePrefix("language-")
        }
    val inlineContent =
        remember(inlineBlock) {
            inlineBlock?.rawContent
                ?: inlineBlock?.children?.singleOrNull()?.let {
                    when (it) {
                        is RichText.Inline.Block -> it.rawContent
                        is RichText.Inline.Text -> it.rawContent
                    }
                }
        }

    val highlightedCode = remember { mutableStateOf<AnnotatedString?>(null) }

    val isDarkMode = MaterialTheme.colorScheme.onSurface.luminance() > 0.5

    remember(language, inlineContent) {
        val syntaxLanguage =
            when (language) {
                "c" -> SyntaxLanguage.C
                "cpp" -> SyntaxLanguage.CPP
                "dart" -> SyntaxLanguage.DART
                "java" -> SyntaxLanguage.JAVA
                "kt",
                "kotlin" -> SyntaxLanguage.KOTLIN
                "rs",
                "rust" -> SyntaxLanguage.RUST
                "cs",
                "csharp" -> SyntaxLanguage.CSHARP
                "coffeescript" -> SyntaxLanguage.COFFEESCRIPT
                "js",
                "javascript" -> SyntaxLanguage.JAVASCRIPT
                "pl",
                "perl" -> SyntaxLanguage.PERL
                "py",
                "python" -> SyntaxLanguage.PYTHON
                "rb",
                "ruby" -> SyntaxLanguage.RUBY
                "sh",
                "bash",
                "shell" -> SyntaxLanguage.SHELL
                "swift" -> SyntaxLanguage.SWIFT
                "ts",
                "typescript" -> SyntaxLanguage.TYPESCRIPT
                "go" -> SyntaxLanguage.GO
                "php" -> SyntaxLanguage.PHP
                else -> null
            }

        if (syntaxLanguage != null && inlineContent != null) {
            try {
                val content = inlineContent.trimEnd()
                val highlights =
                    Highlights.Builder()
                        .code(content)
                        .theme(SyntaxThemes.darcula(darkMode = isDarkMode))
                        .language(syntaxLanguage)
                        .build()
                        .getHighlights()
                highlightedCode.value = buildAnnotatedString {
                    append(content)
                    for (highlight in highlights) {
                        addStyle(
                            start = highlight.location.start,
                            end = highlight.location.end,
                            style =
                                when (highlight) {
                                    is BoldHighlight -> SpanStyle(fontWeight = FontWeight.Bold)
                                    is ColorHighlight -> SpanStyle(color = Color(highlight.rgb or 0xFF000000.toInt()))
                                },
                        )
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "unable to syntax highlight code" }
            }
        }
    }

    return HighlightedCode(
        language = language ?: i18n.commonUnknown(),
        content = highlightedCode.value ?: inlineContent?.let(::AnnotatedString) ?: return null,
    )
}
