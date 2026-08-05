@file:OptIn(ExperimentalDecomposeApi::class)

package de.connect2x.trixnity.messenger.internal.compose.view

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.ClientView
import de.connect2x.trixnity.messenger.compose.view.ClientViewLayout
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel

internal fun ClientView(navigationView: NavigationView): ClientView {
    return ClientViewImpl(navigationView = navigationView)
}

@TrixnityMessengerPrivateApi
private class ClientViewImpl(private val navigationView: NavigationView) : ClientView {
    @Composable
    override fun create(rootViewModel: RootViewModel) {
        ClientViewLayout { navigationView.Content() }
    }
}
