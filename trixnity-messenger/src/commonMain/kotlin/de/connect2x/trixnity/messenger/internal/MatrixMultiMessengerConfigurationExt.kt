package de.connect2x.trixnity.messenger.internal

import com.arkivanov.decompose.InternalDecomposeApi
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration

@OptIn(InternalDecomposeApi::class)
@TrixnityMessengerPrivateApi
fun MatrixMultiMessengerConfiguration.nav3ViewModelOptIn() {
    modulesFactories += ::nav3OptInModuleFactory

    messengerConfiguration { modulesFactories += ::nav3OptInModuleFactory }
}
