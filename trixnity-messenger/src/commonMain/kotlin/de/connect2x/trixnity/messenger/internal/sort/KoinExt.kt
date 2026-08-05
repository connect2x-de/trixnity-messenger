package de.connect2x.trixnity.messenger.internal.sort

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import org.koin.core.Koin
import org.koin.core.qualifier.named

@TrixnityMessengerPrivateApi
inline fun <reified T : Any> Koin.getSorted(): List<T> {
    return getSorter<T>().sort(getAll<T>())
}

@TrixnityMessengerPrivateApi
inline fun <reified T : Any> Koin.getSorter(): Sorter<T> {
    return getOrNull<Sorter<T>>(named<T>()) ?: getTopologicalSorter()
}

@TrixnityMessengerPrivateApi
fun <T : Any> Koin.getTopologicalSorter(): TopologicalSorter<T> {
    return TopologicalSorter(metadata = getAll<SortedMetadata<*>>())
}
