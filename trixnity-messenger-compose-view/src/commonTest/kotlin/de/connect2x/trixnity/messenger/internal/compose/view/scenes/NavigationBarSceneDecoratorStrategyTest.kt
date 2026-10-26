package de.connect2x.trixnity.messenger.internal.compose.view.scenes

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationbar.NavigationBarScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationbar.NavigationBarSceneDecoratorStrategy
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.singlepane.SinglePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class NavigationBarSceneDecoratorStrategyTest {
    private val home = NavEntry("home", metadata = NavigationVisibility.Always.metadata()) {}
    private val room = NavEntry("room") {}

    private fun decorate(scene: Scene<String>, enabled: Boolean = true): Scene<String> =
        with(NavigationBarSceneDecoratorStrategy<String>(enabled)) {
            SceneDecoratorStrategyScope<String>().decorateScene(scene)
        }

    @Test
    fun onlyVisibleEntriesOptIntoTheBarAndBackEntriesArePreserved() {
        val single = SinglePaneScene(home, emptyList())
        val decorated = assertIs<NavigationBarScene<String>>(decorate(single))
        assertSame(single, decorated.paneScene)
        assertEquals(single.entries, decorated.entries)
        assertEquals(single.previousEntries, decorated.previousEntries)
        assertEquals(single.metadata, decorated.metadata)
        val roomScene = SinglePaneScene(room, listOf(home))
        assertSame(roomScene, decorate(roomScene))
        assertIs<NavigationBarScene<String>>(decorate(TwoPaneScene(home, room, listOf(home), null)))
    }

    @Test
    fun decoratingPreservesCustomSceneMetadata() {
        val scene =
            object : Scene<String> {
                override val key = "custom"
                override val entries = listOf(home)
                override val previousEntries = emptyList<NavEntry<String>>()
                override val metadata = mapOf("custom" to "transition")
                override val content: @Composable () -> Unit = {}
            }
        assertEquals(scene.metadata, decorate(scene).metadata)
    }

    @Test
    fun hiddenPaneTakesPrecedenceOverCompactNavigation() {
        val wizard = NavEntry("setup", metadata = NavigationVisibility.Hidden.metadata()) {}
        val scene = TwoPaneScene(home, wizard, listOf(home), null)
        assertSame(scene, decorate(scene))
    }

    @Test
    fun barDisappearsWhenDisabledOrRailTakesOverAndOverlaysRemainUntouched() {
        val single = SinglePaneScene(home, emptyList())
        assertSame(single, decorate(single, enabled = false))
        assertIs<NavigationBarScene<String>>(decorate(single, enabled = true))
        val overlay = OverlayScene(home, listOf(room))
        assertSame(overlay, decorate(overlay))
    }
}
