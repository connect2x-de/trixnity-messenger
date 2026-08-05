package de.connect2x.trixnity.messenger.internal.sort

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import io.ktor.util.reflect.instanceOf

@TrixnityMessengerPrivateApi interface TopologicalSorter<T : Any> : Sorter<T>

internal fun <T : Any> TopologicalSorter(metadata: List<SortedMetadata<*>>): TopologicalSorter<T> {
    return TopologicalSorterImpl(metadata)
}

private class TopologicalSorterImpl<T : Any>(val metadata: List<SortedMetadata<*>>) : TopologicalSorter<T> {
    override fun sort(items: Collection<T>): List<T> {
        val entries = items.associate { item ->
            val metadata = metadata.first { item.instanceOf(it.clazz) }

            Pair(first = metadata.clazz, second = Pair(first = item, second = metadata))
        }

        return topologicalSort(
                keys = entries.keys,
                before = { entries.getValue(it).second.before },
                after = { entries.getValue(it).second.after },
            )
            .map { entries.getValue(it).first }
    }
}

private fun <K> topologicalSort(
    keys: Set<K>,
    before: (K) -> Iterable<K> = { emptyList() },
    after: (K) -> Iterable<K> = { emptyList() },
): List<K> {
    val outgoingEdges = keys.associateWith { mutableSetOf<K>() }
    val incomingCount = keys.associateWith { Count() }

    fun addEdge(from: K, to: K) {
        if (from !in keys || to !in keys) return

        require(from != to) { "self reference is not allowed" }

        if (outgoingEdges.getValue(from).add(to)) {
            incomingCount.getValue(to).increment()
        }
    }

    keys.forEach { key ->
        before(key).forEach { addEdge(key, it) }
        after(key).forEach { addEdge(it, key) }
    }

    val readyKeys = ArrayDeque(incomingCount.filterValues { it.count == 0 }.keys)

    val sortedKeys = mutableListOf<K>()

    while (readyKeys.isNotEmpty()) {
        val key = readyKeys.removeFirst()
        sortedKeys += key

        outgoingEdges.getValue(key).forEach {
            if (incomingCount.getValue(it).decrement() == 0) {
                readyKeys.addLast(it)
            }
        }
    }

    require(sortedKeys.size == keys.size) {
        val cyclicKeys = incomingCount.filterValues { it.count > 0 }.keys

        "dependency cycle: $cyclicKeys"
    }

    return sortedKeys
}

private class Count {

    private var _count = 0

    val count: Int
        get() = _count

    fun increment(): Int {
        _count += 1
        return _count
    }

    fun decrement(): Int {
        _count -= 1
        return _count
    }
}
