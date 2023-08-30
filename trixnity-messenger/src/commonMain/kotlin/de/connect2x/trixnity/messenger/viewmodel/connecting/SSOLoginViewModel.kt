package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging


private val log = KotlinLogging.logger {}

interface SSOLoginViewModelFactory {
    fun newSSOLoginViewModel(
        viewModelContext: ViewModelContext,
        serverUrl: String,
        providerId: String,
        providerName: String,
        onBack: () -> Unit,
    ): SSOLoginViewModel {
        return SSOLoginViewModelImpl(
            viewModelContext,
            serverUrl,
            providerId,
            providerName,
            onBack,
        )
    }
}

interface SSOLoginViewModel {
    val serverUrl: String
    val providerName: String
    val redirectUrl: String
    fun back()
}

open class SSOLoginViewModelImpl(
    viewModelContext: ViewModelContext,
    override val serverUrl: String,
    providerId: String,
    override val providerName: String,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, SSOLoginViewModel {

    // FIXME redirectUrl should also contain unique identifier
    override val redirectUrl = "$serverUrl/_matrix/client/v3/login/sso/redirect/$providerId?redirectUrl=FIXME" // FIXME
    override fun back() {
        onBack()
    }
}

class PreviewSSOLoginViewModel : SSOLoginViewModel {
    override val serverUrl: String = "https://timmy-messenger.de"
    override val providerName: String = "Timmy"
    override val redirectUrl = "$serverUrl/_matrix/client/v3/login/sso/redirect?redirectUrl=FIXME" // FIXME

    override fun back() {
    }

}