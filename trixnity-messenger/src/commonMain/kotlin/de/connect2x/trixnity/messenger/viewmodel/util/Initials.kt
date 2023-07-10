package de.connect2x.trixnity.messenger.viewmodel.util


interface Initials {
    fun compute(name: String): String {
        return name.split(' ')
            .map { it.firstOrNull()?.uppercaseChar() ?: "" }.take(2).joinToString("")
    }
}
