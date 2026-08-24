package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import org.koin.core.parameter.ParametersHolder

@TrixnityMessengerPrivateApi
interface ViewModelFactoryAdapter<V> {
    fun create(parameters: ParametersHolder): V
}
