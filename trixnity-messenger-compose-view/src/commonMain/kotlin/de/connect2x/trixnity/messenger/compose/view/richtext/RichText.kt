package de.connect2x.trixnity.messenger.compose.view.richtext

internal sealed interface RichText {
    data class Block(
        val tag: String,
        val attributes: Map<String, String>,
        val children: List<RichText>,
    ) : RichText

    data class InlineSpan(val children: List<Inline>) : RichText

    sealed interface Inline {
        data class Block(
            val tag: String,
            val attributes: Map<String, String>,
            val children: List<Inline>,
            val rawContent: String? = null,
        ) : Inline

        data class Text(
            val content: String,
            val rawContent: String? = null,
        ) : Inline
    }

    companion object {
        val inline = listOf(
            "a", "sup", "sub", "b", "i", "u", "strong", "em", "s", "code", "br", "span", "del"
        )
    }
}
