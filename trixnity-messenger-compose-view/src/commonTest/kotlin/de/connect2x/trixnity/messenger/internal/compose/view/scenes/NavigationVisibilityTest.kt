package de.connect2x.trixnity.messenger.internal.compose.view.scenes

import androidx.navigation3.runtime.NavEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationVisibilityTest {
    private val room = NavEntry("room") {}
    private val home = NavEntry("home", metadata = NavigationVisibility.Always.metadata()) {}
    private val wizard = NavEntry("wizard", metadata = NavigationVisibility.Hidden.metadata()) {}

    @Test
    fun unmarkedScreensDefaultToNavigationOnWideLayoutsOnly() {
        val visibility = NavigationVisibility.resolve(listOf(room))
        assertEquals(false, visibility.compact)
        assertEquals(true, visibility.wide)
    }

    @Test
    fun homeEnablesNavigationInBothLayoutsAlongsideUnmarkedPanes() {
        val visibility = NavigationVisibility.resolve(listOf(home, room))
        assertEquals(true, visibility.compact)
        assertEquals(true, visibility.wide)
    }

    @Test
    fun hiddenScreensSuppressNavigationInBothLayoutsRegardlessOfEntryOrder() {
        for (entries in listOf(listOf(home, wizard), listOf(wizard, home), listOf(home, wizard, room))) {
            val visibility = NavigationVisibility.resolve(entries)
            assertEquals(false, visibility.compact)
            assertEquals(false, visibility.wide)
        }
        assertEquals(NavigationVisibility.Always, NavigationVisibility.resolve(listOf(home, room)))
    }
}
