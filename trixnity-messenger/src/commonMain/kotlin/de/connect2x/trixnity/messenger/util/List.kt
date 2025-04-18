package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Adds an item to a [List] at the correct position according to its [Comparable.compareTo] method.
 * Assumes the [List] is already sorted.
 *
 * @return A [List] containing the item
 */
fun <T : Comparable<T>> List<T>.plusSorted(element: T): List<T> {
    val insertionPoint = this.binarySearch(element)
    val indexToInsert = if (insertionPoint >= 0) { insertionPoint } else { -insertionPoint - 1 }
    return this.subList(0, indexToInsert) + element + this.subList(indexToInsert, this.size)
}

/**
 * Adds an item to a [List] at the correct position according to its [Comparable.compareTo] method.
 * Assumes the [List] is already sorted.
 * Replaces the current value of the [MutableStateFlow] with the new [List].
 */
fun <T : Comparable<T>> MutableStateFlow<List<T>>.plusSorted(element: T) {
    this.value = this.value.plusSorted(element)
}
