package de.connect2x.trixnity.messenger.internal.compose.view.scenes

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail.NavigationRailScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail.NavigationRailSceneDecoratorStrategy
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail.NavigationRailView
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.singlepane.SinglePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceListRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class NavigationRailSceneDecoratorStrategyTest {
    private val rail =
        object : NavigationRailView {
            @Composable override fun Content(selected: SpaceListRouteMarker?) {}
        }
    private val root = NavEntry("rooms") {}
    private val home = NavEntry("home") {}

    private fun decorate(scene: Scene<String>): Scene<String> =
        with(NavigationRailSceneDecoratorStrategy<String>(rail)) {
            SceneDecoratorStrategyScope<String>().decorateScene(scene)
        }

    @Test
    fun wizardMetadataHidesTheRailThroughNestedStepsAndRestoresItAfterLeaving() {
        val wizard = NavEntry("setup", metadata = NavigationVisibility.Hidden.metadata()) {}
        assertEquals(false, !NavigationVisibility.resolve(listOf(root)).wide)
        assertEquals(true, !NavigationVisibility.resolve(listOf(root, wizard)).wide)
        assertEquals(true, !NavigationVisibility.resolve(listOf(root, wizard, home)).wide)
        assertEquals(false, !NavigationVisibility.resolve(listOf(root, home)).wide)
        val scene = SinglePaneScene(wizard, listOf(root))
        assertSame(scene, decorate(scene))
    }

    @Test
    fun decoratingPreservesTheRegisteredStrategySceneAndBackEntries() {
        val custom =
            object : Scene<String> {
                override val key = "custom"
                override val metadata = mapOf("custom" to "transition")
                override val entries = listOf(root, home)
                override val previousEntries = listOf(root)
                override val content: @Composable () -> Unit = {}
            }
        val decorated = assertIs<NavigationRailScene<String>>(decorate(custom))
        assertSame(custom, decorated.paneScene)
        assertEquals(custom.entries, decorated.entries)
        assertEquals(custom.previousEntries, decorated.previousEntries)
        assertEquals(custom.metadata, decorated.metadata)
    }

    @Test
    fun selectionAndSceneIdentityFollowTheRenderedEntries() {
        val selected = SpaceRoomListRoute(UserId("alice", "example.org"), RoomId("!space:example.org"))
        val space = NavEntry("space", metadata = NavigationRailScene.selection(selected)) {}
        val before = decorate(TwoPaneScene(root, home, listOf(root), null))
        val after =
            assertIs<NavigationRailScene<String>>(decorate(TwoPaneScene(space, home, listOf(root, space), null)))
        assertEquals(selected, after.selected)
        assertEquals(before.key, after.key)
        assertNotEquals(assertIs<NavigationRailScene<String>>(before).paneScene.key, after.paneScene.key)
        assertEquals(null, assertIs<NavigationRailScene<String>>(before).selected)
    }

    @Test
    fun overlaysAreNotDecoratedButFullScreenDestinationsRetainTheReservedRail() {
        val overlay = OverlayScene(home, listOf(root))
        assertSame(overlay, decorate(overlay))
        val single = SinglePaneScene(home, listOf(root))
        assertSame(single, assertIs<NavigationRailScene<String>>(decorate(single)).paneScene)
    }
}
