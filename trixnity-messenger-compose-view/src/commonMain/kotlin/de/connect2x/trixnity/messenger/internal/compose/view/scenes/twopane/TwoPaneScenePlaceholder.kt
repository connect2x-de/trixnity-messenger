package de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.getOrNull

@Composable
@TrixnityMessengerPrivateApi
fun TwoPaneScenePlaceholder() {
    DI.getOrNull<TwoPaneScenePlaceholder>()?.Content()
}

@TrixnityMessengerPrivateApi
interface TwoPaneScenePlaceholder {
    @Composable fun Content()
}
