package de.connect2x.trixnity.messenger.internal.compose.view.scenes

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata

internal enum class NavigationVisibility(val compact: Boolean, val wide: Boolean) {
    Hidden(compact = false, wide = false),
    WideOnly(compact = false, wide = true),
    Always(compact = true, wide = true);

    fun metadata() = metadata { put(Key, this@NavigationVisibility) }

    companion object {
        private data object Key : NavMetadataKey<NavigationVisibility>

        fun resolve(entries: List<NavEntry<*>>): NavigationVisibility {
            val policies = entries.mapNotNull { it.metadata[Key] }
            return when {
                Hidden in policies -> Hidden
                Always in policies -> Always
                else -> WideOnly
            }
        }
    }
}
