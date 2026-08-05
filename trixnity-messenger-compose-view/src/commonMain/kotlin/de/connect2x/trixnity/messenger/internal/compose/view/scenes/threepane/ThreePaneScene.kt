package de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane

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
import de.connect2x.trixnity.messenger.compose.view.room.SETTINGS_WEIGHT
import de.connect2x.trixnity.messenger.compose.view.room.TIMELINE_WEIGHT
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedVerticalDivider

@TrixnityMessengerPrivateApi
interface ThreePaneScene<T : Any> : Scene<T> {
    @TrixnityMessengerPrivateApi
    companion object {

        fun left() = metadata { put(ThreePaneSceneLeftMetadataKey, Unit) }

        fun middle() = metadata { put(ThreePaneSceneMiddleMetadataKey, Unit) }

        fun right() = metadata { put(ThreePaneSceneRightMetadataKey, Unit) }
    }

    @TrixnityMessengerPrivateApi data object ThreePaneSceneLeftMetadataKey : NavMetadataKey<Unit>

    @TrixnityMessengerPrivateApi data object ThreePaneSceneMiddleMetadataKey : NavMetadataKey<Unit>

    @TrixnityMessengerPrivateApi data object ThreePaneSceneRightMetadataKey : NavMetadataKey<Unit>
}

internal fun <T : Any> ThreePaneScene(
    leftEntry: NavEntry<T>,
    middleEntry: NavEntry<T>,
    rightEntry: NavEntry<T>,
    previousEntries: List<NavEntry<T>>,
): ThreePaneScene<T> {
    return ThreePaneSceneImpl(
        leftEntry = leftEntry,
        middleEntry = middleEntry,
        rightEntry = rightEntry,
        previousEntries = previousEntries,
    )
}

private data class ThreePaneSceneImpl<T : Any>(
    val leftEntry: NavEntry<T>,
    val middleEntry: NavEntry<T>,
    val rightEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : ThreePaneScene<T> {
    override val key: Any = Triple(leftEntry.contentKey, middleEntry.contentKey, rightEntry.contentKey)

    override val entries: List<NavEntry<T>> = listOf(leftEntry, middleEntry, rightEntry)

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

                Row(
                    modifier = Modifier.weight(ROOM_WEIGHT).fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(TIMELINE_WEIGHT).fillMaxHeight()) {
                        key(middleEntry.contentKey) { middleEntry.Content() }
                    }

                    ThemedVerticalDivider(modifier = Modifier.fillMaxHeight())

                    Box(modifier = Modifier.weight(SETTINGS_WEIGHT).fillMaxHeight()) {
                        key(rightEntry.contentKey) { rightEntry.Content() }
                    }
                }
            }
        }
    }
}
