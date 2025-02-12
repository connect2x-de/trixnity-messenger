package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.util.firstGraph
import de.connect2x.trixnity.messenger.viewmodel.util.RegexUtil.matchBlankSpace

interface Initials {
    fun compute(name: String): String {
        val graphs = name.split(matchBlankSpace)
            .map { it.firstGraph().uppercase() }
            .filter { it.isNotEmpty() }
        if(graphs.isEmpty()) return ""
        val isFirstEmoji = graphs.first().first().isHighSurrogate()
        return graphs.take(if(isFirstEmoji) 1 else 2).joinToString(separator = "")
    }

    companion object : Initials
}
