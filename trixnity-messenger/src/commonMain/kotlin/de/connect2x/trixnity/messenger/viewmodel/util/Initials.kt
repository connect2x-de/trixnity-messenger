package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.util.GraphemeIterableProvider
import de.connect2x.trixnity.messenger.util.PlatformGraphemeIterableProvider
import de.connect2x.trixnity.messenger.util.isEmoji
import de.connect2x.trixnity.messenger.viewmodel.util.RegexUtil.matchBlankSpace

interface Initials {
    fun compute(name: String): String

    @Deprecated(
        message = "This cannot be overridden by DI, please use platformStringModule instead",
        level = DeprecationLevel.WARNING
    )
    companion object : Initials by InitialsImpl(PlatformGraphemeIterableProvider)
}

class InitialsImpl(
    private val graphemeIterableProvider: GraphemeIterableProvider
) : Initials {
    override fun compute(name: String): String {
        val graphs = name.split(matchBlankSpace)
            .mapNotNull { graphemeIterableProvider(it).firstOrNull() }
            .map { it.uppercase() }

        if (graphs.isEmpty()) return ""
        val isFirstEmoji = graphs.first().isEmoji()
        val isSecondEmoji = graphs.getOrNull(1)?.isEmoji() == true
        return graphs.take(if (isFirstEmoji && isSecondEmoji) 1 else 2).joinToString(separator = "")
    }
}
