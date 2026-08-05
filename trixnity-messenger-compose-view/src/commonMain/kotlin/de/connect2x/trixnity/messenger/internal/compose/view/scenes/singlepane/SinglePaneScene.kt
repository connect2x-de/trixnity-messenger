package de.connect2x.trixnity.messenger.internal.compose.view.scenes.singlepane

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane

@TrixnityMessengerPrivateApi interface SinglePaneScene<T : Any> : Scene<T>

internal fun <T : Any> SinglePaneScene(entry: NavEntry<T>, previousEntries: List<NavEntry<T>>): SinglePaneScene<T> {
    return SinglePaneSceneImpl(entry = entry, previousEntries = previousEntries)
}

private class SinglePaneSceneImpl<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : SinglePaneScene<T> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<T>> = listOf(entry)
    override val content: @Composable () -> Unit = {
        CompositionLocalProvider(IsSinglePane provides true) { key(entry.contentKey) { entry.Content() } }
    }
}
