package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.UrlRoutingHandler
import de.connect2x.trixnity.messenger.viewmodel.RootRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.RootRouter.Wrapper
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class SSOUrlRoutingHandler(
    val config: MatrixMessengerConfiguration,
    val settings: MatrixMessengerSettingsHolder,
) : UrlRoutingHandler {
    override suspend fun onHandleUrl(
        coroutineScope: CoroutineScope,
        navigate: suspend (List<Config>) -> Wrapper,
        url: Url
    ): Boolean {
        if (url.encodedPath == "/${config.ssoRedirectPath}") {
            coroutineScope.launch {
                val state = settings.value.ssoState
                if (state != null) {
                    val instance = navigate(
                        listOf(
                            Config.AddMatrixAccount,
                            Config.SSOLogin(state.serverUrl, state.providerId, state.providerName, state.state)
                        )
                    )
                    if (instance is Wrapper.SSOLogin) {
                        instance.viewModel.resumeLogin(url)
                    }
                }
            }
            return true
        }
        return false
    }
}
