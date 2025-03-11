package de.connect2x.messenger

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.messenger.compose.view.Client
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createRoot
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.create
import de.connect2x.trixnity.messenger.multi.singleMode
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

fun MainViewController() = ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
    log.info { "Starting iOS client" }
    val matrixMessenger = mutableStateOf<MatrixMessenger?>(null)
    val rootViewModel = mutableStateOf<RootViewModel?>(null)

    LaunchedEffect(Unit) {
        val matrixMultiMessenger = MatrixMultiMessenger.create(
            configuration = messengerConfiguration()
        )
        matrixMultiMessenger.singleMode { _matrixMessenger ->
            try {
                val config = _matrixMessenger.di.get<MatrixMessengerConfiguration>()
                log.info { "Creating ${config.appId} iOS MatrixMessenger and RootViewModel ..." }
                matrixMessenger.value = _matrixMessenger
                val lifecycleRegistry = LifecycleRegistry(Lifecycle.State.STARTED)
                rootViewModel.value = _matrixMessenger.createRoot(DefaultComponentContext(lifecycleRegistry))
            } catch (exc: Exception) {
                log.error(exc) { "Error on creating client" }
            }
        }
    }

    matrixMessenger.value?.let { matrixMessenger ->
        rootViewModel.value?.let { rootViewModel ->
            CompositionLocalProvider(
                Platform provides PlatformType.ANDROID, // TODO iOS
                DI provides matrixMessenger.di,
            ) {
                MessengerTheme {
                    Client(rootViewModel)
                }
            }
        } ?: LoadingSpinner()
    } ?: LoadingSpinner()
}
