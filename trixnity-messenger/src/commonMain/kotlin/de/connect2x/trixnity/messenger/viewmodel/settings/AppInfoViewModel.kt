package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.component.get

interface AppInfoViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseAppInfo: () -> Unit,
    ): AppInfoViewModel {
        return AppInfoViewModelImpl(viewModelContext, onCloseAppInfo)
    }

    companion object : AppInfoViewModelFactory
}

interface AppInfoViewModel {
    val version: String?
    val showPrivacy: MutableStateFlow<Boolean>
    val showImprint: MutableStateFlow<Boolean>
    val showLicenses: MutableStateFlow<Boolean>
    fun close()
}

open class AppInfoViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseAppInfo: () -> Unit,
) : ViewModelContext by viewModelContext, AppInfoViewModel {
    override val version: String? = get<AppVersion>().version
    override val showPrivacy = MutableStateFlow(false)
    override val showImprint = MutableStateFlow(false)
    override val showLicenses = MutableStateFlow(false)

    private val backCallback = BackCallback {
        if (showPrivacy.value || showImprint.value || showLicenses.value){
            showPrivacy.value = false
            showImprint.value = false
            showLicenses.value = false
        }else {
            close()
        }
    }

    init {
        backHandler.register(backCallback)
    }

    override fun close() {
        onCloseAppInfo()
    }
}

class PreviewAppInfoViewModel : AppInfoViewModel {
    override val version: String? = "4.2.0"
    override val showPrivacy: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showImprint: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLicenses: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun close() {
    }
}
