package de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.ROOM_LIST_WEIGHT
import de.connect2x.trixnity.messenger.compose.view.ROOM_WEIGHT
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedVerticalDivider

@TrixnityMessengerPrivateApi
interface TwoPaneScene<T : Any> : Scene<T> {
    @TrixnityMessengerPrivateApi
    companion object {

        fun left() = metadata { put(TwoPaneSceneLeftMetadataKey, Unit) }

        fun right() = metadata { put(TwoPaneSceneRightMetadataKey, Unit) }
    }

    @TrixnityMessengerPrivateApi data object TwoPaneSceneLeftMetadataKey : NavMetadataKey<Unit>

    @TrixnityMessengerPrivateApi data object TwoPaneSceneRightMetadataKey : NavMetadataKey<Unit>
}

internal fun <T : Any> TwoPaneScene(
    leftEntry: NavEntry<T>,
    rightEntry: NavEntry<T>?,
    previousEntries: List<NavEntry<T>>,
): TwoPaneScene<T> {
    return TwoPaneSceneImpl(leftEntry = leftEntry, rightEntry = rightEntry, previousEntries = previousEntries)
}

private class TwoPaneSceneImpl<T : Any>(
    val leftEntry: NavEntry<T>,
    val rightEntry: NavEntry<T>?,
    override val previousEntries: List<NavEntry<T>>,
) : TwoPaneScene<T> {

    override val key: Any = Pair(leftEntry.contentKey, rightEntry?.contentKey)

    override val entries: List<NavEntry<T>> = listOfNotNull(leftEntry, rightEntry)

    override val content: @Composable () -> Unit = {
        CompositionLocalProvider(IsSinglePane provides false) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(ROOM_LIST_WEIGHT).fillMaxHeight()) {
                    key(leftEntry.contentKey) { leftEntry.Content() }
                }

                ThemedVerticalDivider(modifier = Modifier.fillMaxHeight())

                Box(modifier = Modifier.weight(ROOM_WEIGHT).fillMaxHeight()) {
                    if (rightEntry != null) {
                        key(rightEntry.contentKey) { rightEntry.Content() }
                    }
                }
            }
        }
    }
}
