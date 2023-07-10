package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.getLicenses
import de.connect2x.trixnity.messenger.getVersion
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow

interface AppInfoViewModelFactory {
    fun newAppInfoViewModel(
        viewModelContext: ViewModelContext,
        onCloseAppInfo: () -> Unit,
    ): AppInfoViewModel {
        return AppInfoViewModelImpl(viewModelContext, onCloseAppInfo)
    }
}

interface AppInfoViewModel {
    val showPrivacy: MutableStateFlow<Boolean>
    val showImprint: MutableStateFlow<Boolean>
    val showLicenses: MutableStateFlow<Boolean>
    val version: String
    val licenses: String
    fun close()
}

open class AppInfoViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseAppInfo: () -> Unit,
) : ViewModelContext by viewModelContext, AppInfoViewModel {
    override val showPrivacy = MutableStateFlow(false)
    override val showImprint = MutableStateFlow(false)
    override val showLicenses = MutableStateFlow(false)

    override val version = getVersion()
    override val licenses = getLicenses()

    private val backCallback = BackCallback {
        close()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun close() {
        onCloseAppInfo()
    }
}

class PreviewAppInfoViewModel : AppInfoViewModel {
    override val showPrivacy: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showImprint: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLicenses: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val version: String = "0.49.11"
    override val licenses: String = "Licenses"
    override fun close() {
    }

}
