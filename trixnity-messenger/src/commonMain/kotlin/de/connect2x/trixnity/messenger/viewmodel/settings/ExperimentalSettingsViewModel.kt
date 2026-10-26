package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerExperimentalSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.inject

interface ExperimentalSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseExperimentalSettings: () -> Unit,
    ): ExperimentalSettingsViewModel {
        return ExperimentalSettingsViewModelImpl(viewModelContext, onCloseExperimentalSettings)
    }

    companion object : ExperimentalSettingsViewModelFactory
}

interface ExperimentalSettingsViewModel {
    @TrixnityMessengerCallApi val videoCallsEnabled: StateFlow<Boolean>

    @TrixnityMessengerCallApi fun toggleVideoCallsEnabled()

    fun back()
}

class ExperimentalSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseExperimentalSettings: () -> Unit,
) : ViewModelContext by viewModelContext, ExperimentalSettingsViewModel {
    private val settings: MatrixMessengerSettingsHolder by inject()
    private val configuration: MatrixMessengerConfiguration by inject()

    @TrixnityMessengerCallApi
    override val videoCallsEnabled: StateFlow<Boolean> =
        settings
            .map { it.experimental.enableVideoCalls }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.experimental.enableVideoCalls)

    private val backCallback = BackCallback { back() }

    init {
        registerBackCallback(backCallback)
    }

    @TrixnityMessengerCallApi
    override fun toggleVideoCallsEnabled() {
        if (!configuration.features.enableVideoCalls) {
            log.error { "Unable to enable MatrixRTC-based video calls because the developer disabled video calls" }
            return
        }

        coroutineScope.launch {
            settings.update<MatrixMessengerExperimentalSettings> { it.copy(enableVideoCalls = !it.enableVideoCalls) }
        }
    }

    override fun back() = onCloseExperimentalSettings()
}
