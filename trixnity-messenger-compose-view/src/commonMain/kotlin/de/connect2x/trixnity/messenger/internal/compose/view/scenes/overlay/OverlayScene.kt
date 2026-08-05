package de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene as AndroidxOverlayScene
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface OverlayScene<T : Any> : AndroidxOverlayScene<T> {
    @TrixnityMessengerPrivateApi
    companion object {
        fun overlay() = metadata { put(OverlaySceneMetadataKey, Unit) }
    }

    @TrixnityMessengerPrivateApi data object OverlaySceneMetadataKey : NavMetadataKey<Unit>
}

internal fun <T : Any> OverlayScene(entry: NavEntry<T>, previousEntries: List<NavEntry<T>>): OverlayScene<T> {
    return OverlaySceneImpl(entry = entry, previousEntries = previousEntries)
}

private class OverlaySceneImpl<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : OverlayScene<T> {

    override val key: Any = entry.contentKey
    override val entries = listOf(entry)
    override val overlaidEntries: List<NavEntry<T>> = previousEntries

    override val content: @Composable () -> Unit = { key(entry.contentKey) { entry.Content() } }
}
