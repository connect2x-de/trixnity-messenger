package de.connect2x.messenger.compose.view

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import de.connect2x.trixnity.messenger.util.defaultUrlHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private val log = KotlinLogging.logger {}

fun startMultiMessenger(
    args: Array<String>,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) = runBlocking(Dispatchers.Default) {
    log.info { "Starting client" }
    log.info { "command line args: ${args.joinToString { it }}" }
    log.info { "JVM version: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})" }

    val lifecycle = LifecycleRegistry()
    val matrixMultiMessenger = MatrixMultiMessenger.create(
        configuration = configuration
    )

    // to set the lang to EN:   matrixMessenger.di.get<MatrixMessengerSettingsHolder>().update { it.copy(preferredLang = "en") }

    val urlHandler = matrixMultiMessenger.defaultUrlHandler
    urlHandler.start(args)

    messengerApp(matrixMultiMessenger, lifecycle, urlHandler)
}
