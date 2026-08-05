package de.connect2x.trixnity.messenger.internal.sort

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

@TrixnityMessengerPrivateApi
inline fun <reified T : Any> Scope.getSorted(): List<T> {
    return getSorter<T>().sort(getAll<T>())
}

@TrixnityMessengerPrivateApi
inline fun <reified T : Any> Scope.getSorter(): Sorter<T> {
    return getOrNull<Sorter<T>>(named<T>()) ?: getTopologicalSorter()
}

@TrixnityMessengerPrivateApi
fun <T : Any> Scope.getTopologicalSorter(): TopologicalSorter<T> {
    return TopologicalSorter(metadata = getAll<SortedMetadata<*>>())
}
