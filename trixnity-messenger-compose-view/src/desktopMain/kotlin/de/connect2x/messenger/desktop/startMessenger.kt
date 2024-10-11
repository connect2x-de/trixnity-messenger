package de.connect2x.messenger.desktop

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import de.connect2x.trixnity.messenger.util.defaultUrlHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private val log = KotlinLogging.logger {}

fun startMessenger(
    appName: String,
    version: String,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
    args: Array<String>,
) = runBlocking(Dispatchers.Default) {
    log.info { "Starting $appName client (version=${version})" }
    log.info { "command line args: ${args.joinToString { it }}" }
    log.info { "JVM version: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})" }

    val lifecycle = LifecycleRegistry()
    val matrixMultiMessenger = MatrixMultiMessenger.create(
        configuration = configuration
    )

// to set the lang to EN:   matrixMessenger.di.get<MatrixMessengerSettingsHolder>().update { it.copy(preferredLang = "en") }

    val urlHandler = matrixMultiMessenger.defaultUrlHandler
    urlHandler.start(args)

    val isDebug = args
        .find { arg -> arg.startsWith("debug=") }
        ?.split("=")?.last()?.toBoolean()
        ?: false

    messengerApp(matrixMultiMessenger, lifecycle, urlHandler, isDebug)
}
