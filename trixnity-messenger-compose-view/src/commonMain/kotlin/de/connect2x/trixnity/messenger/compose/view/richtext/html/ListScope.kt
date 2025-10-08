package de.connect2x.trixnity.messenger.compose.view.richtext.html

internal sealed interface ListScope {
    fun render(): String

    data class OrderedList(
        var index: Int = 1,
        val reversed: Boolean = false,
        val type: OrderedListStyle = OrderedListStyle.NUMBERS,
    ) : ListScope {
        override fun render(): String = when (type) {
            OrderedListStyle.LOWERCASE_LETTERS -> letter(index).lowercase()
            OrderedListStyle.UPPERCASE_LETTERS -> letter(index)
            OrderedListStyle.LOWERCASE_ROMAN -> romanNumeral(index).lowercase()
            OrderedListStyle.UPPERCASE_ROMAN -> romanNumeral(index)
            OrderedListStyle.NUMBERS -> "${index}."
        }.also { index++ }
    }

    enum class OrderedListStyle {
        LOWERCASE_LETTERS,
        UPPERCASE_LETTERS,
        LOWERCASE_ROMAN,
        UPPERCASE_ROMAN,
        NUMBERS;

        companion object {
            fun of(value: String?): OrderedListStyle? = when (value) {
                "a" -> LOWERCASE_LETTERS
                "A" -> UPPERCASE_LETTERS
                "i" -> LOWERCASE_ROMAN
                "I" -> UPPERCASE_ROMAN
                "1" -> NUMBERS
                else -> null
            }
        }
    }

    data class UnorderedList(
        val type: UnorderedListStyle = UnorderedListStyle.CIRCLE,
    ) : ListScope {
        override fun render(): String = type.symbol
    }

    enum class UnorderedListStyle(val symbol: String) {
        CIRCLE("•"),
        DISC("◦"),
        SQUARE("▪");

        companion object {
            fun of(value: String?): UnorderedListStyle? = when (value?.lowercase()) {
                "disc" -> DISC
                "square" -> SQUARE
                "circle" -> CIRCLE
                else -> null
            }

            fun next(style: UnorderedListStyle) = when (style) {
                CIRCLE -> DISC
                DISC -> SQUARE
                SQUARE -> CIRCLE
            }
        }
    }
}

private val alphabet = ('A'..'Z').toList()

private fun letter(number: Int): String {
    var counter = number
    return buildString {
        while (counter > 26) {
            append(alphabet[counter / 26 - 1])
            counter = counter % 26
        }
        append(alphabet[counter - 1])
        append('.')
    }
}

private val romanSymbols = listOf(
    1000 to "M",
    900 to "CM",
    500 to "D",
    400 to "CD",
    100 to "C",
    90 to "XC",
    50 to "L",
    40 to "XL",
    10 to "X",
    9 to "IX",
    5 to "V",
    4 to "IV",
    1 to "I"
)

private fun romanNumeral(number: Int): String {
    if (number < 1 || number > 3999) { return "" }

    var remaining = number
    return buildString {
        for ((value, symbol) in romanSymbols) {
            while (remaining >= value) {
                append(symbol)
                remaining -= value
            }
        }
        append('.')
    }
}
