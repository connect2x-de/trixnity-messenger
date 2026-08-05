package de.connect2x.trixnity.messenger.internal.compose.view.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import de.connect2x.trixnity.messenger.internal.navigation.Route

internal interface EntryProvider {
    fun createEntry(route: Route): NavEntry<Route>
}

internal fun EntryProvider(entries: List<AnyNavigationEntry<*>>): EntryProvider {
    return EntryProviderImpl(entries = entries)
}

private class EntryProviderImpl(entries: List<AnyNavigationEntry<*>>) : EntryProvider {

    private val delegate = entryProvider { entries.forEach(::addEntryProvider) }

    override fun createEntry(route: Route): NavEntry<Route> {
        return delegate.invoke(route)
    }
}

private fun EntryProviderScope<Route>.addEntryProvider(entry: AnyNavigationEntry<*>) {
    addEntryProvider(
        clazz = entry.clazz,
        clazzContentKey = entry::anyClazzContentKey,
        metadata = entry::anyMetadata,
        content = entry::AnyContent,
    )
}
