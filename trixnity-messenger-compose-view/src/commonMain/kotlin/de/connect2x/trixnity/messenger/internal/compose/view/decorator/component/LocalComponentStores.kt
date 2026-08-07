package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import androidx.compose.runtime.staticCompositionLocalOf
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
val LocalComponentStores =
    staticCompositionLocalOf<ComponentStores> { error("CompositionLocal LocalComponentStores not present") }
