package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.util.firstGraph
import de.connect2x.trixnity.messenger.util.isEmoji
import de.connect2x.trixnity.messenger.viewmodel.util.RegexUtil.matchBlankSpace

interface Initials {
    fun compute(name: String): String {
        val graphs = name.split(matchBlankSpace)
            .map { it.firstGraph().uppercase() }
            .filter { it.isNotEmpty() }
        if (graphs.isEmpty()) return ""
        val isFirstEmoji = graphs.first().isEmoji()
        val isSecondEmoji = graphs.getOrNull(1)?.isEmoji() == true
        return graphs.take(if (isFirstEmoji && isSecondEmoji) 1 else 2).joinToString(separator = "")
    }

    companion object : Initials
}
