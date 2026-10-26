package de.connect2x.trixnity.messenger.internal.compose.view.scenes

import androidx.navigation3.runtime.NavEntry
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.compose.view.entries.AccountSetupSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.CrossSigningBootstrapExplanationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.CrossSigningBootstrapRecoveryKeyEntry
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapExplanationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRecoveryKeyRoute
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationVisibilityTest {
    private val room = NavEntry("room") {}
    private val home = NavEntry("home", metadata = NavigationVisibility.Always.metadata()) {}
    private val wizard = NavEntry("wizard", metadata = NavigationVisibility.Hidden.metadata()) {}

    @Test
    fun setupStepsHideNavigationWhenDisplayedAlone() {
        val userId = UserId("alice", "example.org")
        val metadata =
            listOf(
                AccountSetupSettingsEntry().metadata(AccountSetupSettingsRoute(userId)),
                CrossSigningBootstrapExplanationEntry().metadata(CrossSigningBootstrapExplanationRoute(userId)),
                CrossSigningBootstrapRecoveryKeyEntry().metadata(CrossSigningBootstrapRecoveryKeyRoute(userId)),
            )
        for (entryMetadata in metadata) {
            val entry = NavEntry("step", metadata = entryMetadata) {}
            assertEquals(NavigationVisibility.Hidden, NavigationVisibility.resolve(listOf(entry)))
        }
    }

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
