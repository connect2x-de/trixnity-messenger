package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.util.RegexUtil.matchBlankSpace


interface Initials {
    fun compute(name: String): String =
        name.split(matchBlankSpace)
            .asSequence()
            .map {
                it.mapIndexed { i, char ->
                    if (i == 0) char.uppercaseChar()
                    else if (i == 1 && char.isLowSurrogate()) char
                    else null
                }
                    .filterNotNull()
                    .joinToString(separator = "")
            }
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString(separator = "")

    companion object : Initials
}
