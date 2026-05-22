package de.connect2x.trixnity.messenger.viewmodel.util

inline fun <T> Iterable<T>.takeWhileInclusive(predicate: (T) -> Boolean): List<T> {
    var shouldContinue = true
    return takeWhile {
        val result = shouldContinue
        shouldContinue = predicate(it)
        result
    }
}

inline fun <T> List<T>.takeLastWhileInclusive(predicate: (T) -> Boolean): List<T> {
    var shouldContinue = true
    return takeLastWhile {
        val result = shouldContinue
        shouldContinue = predicate(it)
        result
    }
}
