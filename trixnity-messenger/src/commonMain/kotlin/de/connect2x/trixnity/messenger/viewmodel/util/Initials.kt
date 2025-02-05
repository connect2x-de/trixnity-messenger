package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.util.takeGraphemes
import de.connect2x.trixnity.messenger.viewmodel.util.RegexUtil.matchBlankSpace


interface Initials {
    fun compute(name: String): String =
        name.split(matchBlankSpace)
            .map {
                it.asSequence()
                    .takeGraphemes(1)
                    .joinToString(separator = "")
                    .uppercase()
            }
            .filter { it.isNotEmpty() }
            .asSequence()
            .take(2)
            .joinToString(separator = "")

    companion object : Initials
}
