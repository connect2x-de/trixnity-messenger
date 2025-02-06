package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.util.firstGraph
import de.connect2x.trixnity.messenger.viewmodel.util.RegexUtil.matchBlankSpace


interface Initials {
    fun compute(name: String): String =
        name.split(matchBlankSpace)
            .map { it.firstGraph().uppercase() }
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString(separator = "")

    companion object : Initials
}
