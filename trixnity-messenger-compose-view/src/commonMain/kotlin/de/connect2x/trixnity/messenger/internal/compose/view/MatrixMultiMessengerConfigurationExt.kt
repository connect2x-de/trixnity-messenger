package de.connect2x.trixnity.messenger.internal.compose.view

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.nav3ViewModelOptIn
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration

@TrixnityMessengerPrivateApi
fun MatrixMultiMessengerConfiguration.nav3ViewOptIn() {
    nav3ViewModelOptIn()
}
