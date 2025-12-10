package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module
import java.awt.Desktop
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

private val log = KotlinLogging.logger { }

class UriHandlerImpl(
    config: MatrixMessengerBaseConfiguration,
    private val fileSystem: FileSystem,
    rootPath: RootPath,
    private val closeApp: CloseApp?
) :
    UriHandlerBase(config) {

    private val started = MutableStateFlow(false)
    private val rootPath = rootPath.path
    private val lockFileName = "port.lock"

    /**
     * This need to be called with application start arguments.
     */
    suspend fun start(args: Array<String>) = withContext(Dispatchers.IO) {
        val os = getOs()
        when {
            Desktop.isDesktopSupported() && os == OS.MAC_OS -> {
                args.firstOrNull()?.also { emitUrl(it) }
                Desktop.getDesktop().setOpenURIHandler { event ->
                    urlHandlerFlow.tryEmit(event.uri.toString())
                }
            }

            os == OS.WINDOWS || os == OS.LINUX -> {
                if (started.updateAndGet { true }.not()) return@withContext
                val urlArg = args.firstOrNull()

                val port = readPortFromLockFile()
                if (port == null) {
                    listenForArgs(urlArg)
                } else {
                    sendUrlArg(urlArg.orEmpty(), port)
                }
            }

            else -> log.warn { "this platform is not supported to listen for uris via args" }
        }
    }

    private suspend fun emitUrl(urlArg: String) {
        if (urlArg.isNotBlank())
            try {
                urlHandlerFlow.emit(urlArg)
            } catch (exception: URLParserException) {
                log.error(exception) { "could not parse url from arg $urlArg" }
            }
    }

    private fun readPortFromLockFile(): Int? {
        val lockFile = rootPath.resolve(lockFileName)
        return if (fileSystem.exists(lockFile)) {
            fileSystem.read(lockFile) { readUtf8().toIntOrNull() }
        } else null
    }

    private fun writePortToLockFile(port: Int) {
        log.debug { "write port $port to lock file" }
        val lockFile = rootPath.resolve(lockFileName)
        fileSystem.write(lockFile) { writeUtf8(port.toString()) }
        val randomAccessFile = RandomAccessFile(lockFile.toFile(), "rw")
        val channel = randomAccessFile.getChannel()
        val lock = channel.tryLock(0, Long.MAX_VALUE, true)
        fun releaseFile() {
            randomAccessFile.close()
            fileSystem.delete(lockFile)
        }
        if (lock == null) {
            channel.close()
            releaseFile()
            throw IllegalStateException("could not lock $lockFileName")
        }
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            lock.release()
            channel.close()
            releaseFile()
        })
    }

    private fun listenForArgs(urlArg: String?) {
        thread {
            try {
                runBlocking(Dispatchers.IO) {
                    urlArg?.also { emitUrl(it) }

                    val address = InetAddress.getLoopbackAddress()
                    var port = 2424
                    var server: ServerSocket? = null
                    while (true) {
                        try {
                            if (port < 3000)
                                server = ServerSocket(port, 0, address)
                            break
                        } catch (exception: IOException) {
                            port++
                        }
                    }
                    if (server != null) {
                        writePortToLockFile(port)
                        log.debug { "start listening for url args on port $port" }
                        while (server.isClosed.not()) {
                            try {
                                val socket = server.accept()
                                launch {
                                    try {
                                        log.debug { "try read url arg" }
                                        val inputStream = socket.getInputStream()
                                        val bytes = inputStream.readAllBytes()
                                        inputStream.close()
                                        val url = bytes.decodeToString()
                                        log.debug { "received url arg $url" }
                                        emitUrl(url)
                                    } catch (exception: IOException) {
                                        log.error(exception) { "error reading url args" }
                                    } finally {
                                        socket.close()
                                    }
                                }
                            } catch (exception: Exception) {
                                log.error(exception) { "error reading url args" }
                            }
                        }
                    } else log.error { "could not start server socket to listen for url args" }
                }
            } catch (exception: Exception) {
                log.error(exception) { "error in listenForArgs thread" }
                throw exception
            }
        }
    }

    private suspend fun sendUrlArg(urlArg: String, port: Int) {
        withContext(Dispatchers.IO) {
            val address = InetAddress.getLoopbackAddress()
            val socket =
                try {
                    Socket(address, port)
                } catch (exception: Exception) {
                    log.error(exception) { "could not open client socket on $port to send url arg" }
                    null
                }
            if (socket != null) {
                log.debug { "try send url arg $urlArg using port $port" }
                try {
                    val outputStream = socket.getOutputStream()
                    outputStream.write(urlArg.encodeToByteArray())
                    outputStream.close()
                } catch (exception: Exception) {
                    log.error(exception) { "error sending url arg $urlArg" }
                } finally {
                    socket.close()
                }
                closeApp?.invoke()
            } else {
                listenForArgs(null) // fallback
            }
        }
    }
}

actual fun platformUriHandlerModule(): Module = module {
    single<UriHandler> {
        UriHandlerImpl(get(), get(), get(), getOrNull())
    }
}

val MatrixMessenger.defaultUriHandler: UriHandlerImpl
    get() = checkNotNull(di.get<UriHandler>() as? UriHandlerImpl) {
        "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
    }

val MatrixMultiMessenger.defaultUriHandler: UriHandlerImpl
    get() = checkNotNull(di.get<UriHandler>() as? UriHandlerImpl) {
        "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
    }
