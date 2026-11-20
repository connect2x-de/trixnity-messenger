package de.connect2x.trixnity.messenger.internal.compose.view.scenes

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import de.connect2x.trixnity.messenger.internal.compose.view.entries.SpaceExploreEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.SpaceRoomListEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.SpaceSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneSceneStrategy
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneSceneStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpaceSettingsSceneTest {
    private val rooms = NavEntry("rooms", metadata = SpaceRoomListEntry().metadata) {}
    private val explore = NavEntry("explore", metadata = SpaceExploreEntry().metadata) {}
    private val settings = NavEntry("settings", metadata = SpaceSettingsEntry().metadata) {}

    @Test
    fun wideSettingsKeepExploreInTheMiddlePane() {
        val entries = listOf(rooms, explore, settings)
        val scene =
            with(ThreePaneSceneStrategy<String>(WindowSizeClass(1600, 900))) {
                SceneStrategyScope<String>().calculateScene(entries)
            }
        assertNotNull(scene)
        assertEquals(entries, scene.entries)
        assertEquals(listOf(rooms, explore), scene.previousEntries)
    }

    @Test
    fun exploreWithoutSettingsKeepsTheTwoPaneLayout() {
        val entries = listOf(rooms, explore)
        val size = WindowSizeClass(1600, 900)
        assertNull(with(ThreePaneSceneStrategy<String>(size)) { SceneStrategyScope<String>().calculateScene(entries) })
        val scene =
            with(TwoPaneSceneStrategy<String>(size, null)) { SceneStrategyScope<String>().calculateScene(entries) }
        assertNotNull(scene)
        assertEquals(entries, scene.entries)
    }
}
