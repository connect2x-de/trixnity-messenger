package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import io.ktor.http.URLParserException
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.FileSystem
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.module

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.UrlHandlerKt")

/**
 * Handles application startup arguments and system URI protocol activations.
 *
 * This implementation prevents multiple application instances from running concurrently on Windows & Linux, by sending
 * URL arguments to the already opened instance.
 *
 * ### URL-Exchange Socket Protocol
 *
 * The exchange between [sendUrlToSocket] (Client / Secondary instance) and [receiveUrlFromSocket] (Server / Primary
 * instance) follows this sequence:
 * ```
 *    Secondary Instance ([sendUrlArg])                Primary Instance ([listenForArgs])
 *         [sendUrlToSocket]                                [receiveUrlFromSocket]
 *                 │                                                 │
 *                 │  (1. Connect TCP)                               │
 *                 ├────────────────────────────────────────────────►│ (ServerSocket.accept)
 *                 │                                                 │
 *                 │  2. Write Payload                               │
 *                 │     "url\n$appValidationString"                 │
 *                 ├────────────────────────────────────────────────►│ (Read bytes)
 *                 │                                                 │
 *                 │  3. shutdownOutput()                            │
 *                 ├────────────────────────────────────────────────►│
 *                 │                                                 │
 *                 │  4. Write Response                              │
 *                 │     "OK\n$appValidationString"                  │
 *                 │◄────────────────────────────────────────────────┤
 *                 │                                                 │
 *          [Read response]                                          │ [Parse & Validate url-payload]
 *        (Timeout limit: 2s)                                        │ Does appValidationString
 *                 │                                                 │ match local one?
 *                 │                                                 │       │
 *                 │                                                 ▼       ▼
 *                 ▼                                                (Yes)    (No)
 *           [Close Socket]                                          │       │
 *                 │                                                 ▼       ▼
 *                 ▼                                             [emitUrl] [Reject]
 *       Did we get back the exact                                   │       │
 *        "OK\n$appVersionString"                                    ▼       ▼
 *          and no Timeout?                                       [Close Socket]
 *         /             \                                              │
 *      (Yes)            (No / Timeout)                               (Done)
 *       /                 \
 *      ▼                   ▼
 * [Close App]       [Fallback: Become]
 *                   [ Primary Instance ]
 * ```
 */
class UriHandlerImpl(
    config: MatrixMessengerBaseConfiguration,
    private val fileSystem: FileSystem,
    rootPath: RootPath,
    private val closeApp: CloseApp?,
    private val di: Koin,
) : UriHandlerBase(config) {

    private val started = MutableStateFlow(false)
    private val rootPath = rootPath.path
    private val lockFileName = "port.lock"

    private val appValidationString = config.appName
    private val okResponseString = "OK\n$appValidationString"

    /** This need to be called with application start arguments. */
    suspend fun start(args: Array<String>) =
        withContext(Dispatchers.IO) {
            val os = getOs()
            when {
                Desktop.isDesktopSupported() && os == OS.MAC_OS -> {
                    args.firstOrNull()?.also { emitUrl(it) }
                    Desktop.getDesktop().setOpenURIHandler { event -> urlHandlerFlow.tryEmit(event.uri.toString()) }
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
        val fileLocker = di.get<FileLocker>()
        fun releaseFile() {
            fileLocker.release()
            fileSystem.delete(lockFile)
        }
        if (!fileLocker.lockFile(lockFile.toFile())) {
            releaseFile()
            throw IllegalStateException("could not lock $lockFileName")
        }
        Runtime.getRuntime()
            .addShutdownHook(
                thread(start = false) {
                    releaseFile()
                }
            )
    }

    private fun openServer(): Pair<ServerSocket?, Int> {
        val address = InetAddress.getLoopbackAddress()
        var port = 2424
        var server: ServerSocket? = null
        while (true) {
            try {
                if (port < 3000) server = ServerSocket(port, 0, address)
                break
            } catch (_: IOException) {
                port++
            }
        }
        return Pair(server, port)
    }

    internal fun listenForArgs(urlArg: String?): AutoCloseable? {
        val (server, port) = openServer()

        thread {
            try {
                runBlocking(Dispatchers.IO) {
                    urlArg?.also { emitUrl(it) }

                    if (server != null) {
                        writePortToLockFile(port)
                        log.debug { "start listening for url args on port $port" }
                        while (server.isClosed.not()) {

                            val socket = server.accept()
                            launch {
                                val urlResponse = socket.use {
                                    receiveUrlFromSocket(socket)
                                }
                                urlResponse.fold(
                                    {
                                        if (it != null) {
                                            emitUrl(it)
                                        } else {
                                            log.warn { "url args from different app received" }
                                        }
                                    },
                                    {
                                        log.error(it) { "error reading url args" }
                                    },
                                )
                            }
                        }
                    } else log.error { "could not start server socket to listen for url args" }
                }
            } catch (exception: Exception) {
                log.error(exception) { "error in listenForArgs thread" }
                throw exception
            }
        }

        return server
    }

    private fun receiveUrlFromSocket(socket: Socket): Result<String?> {
        return runCatching {
            log.debug { "try read url arg" }

            val inputStream = socket.getInputStream()
            val reply = inputStream.readNBytes(8192).decodeToString().split("\n")

            val url = reply.getOrNull(0)
            val senderAppVersion = reply.getOrNull(1)

            log.debug { "received url arg $url" }

            log.debug { "sending back OK message $okResponseString" }

            val outputStream = socket.getOutputStream()
            outputStream.write((okResponseString).toByteArray(Charsets.UTF_8))

            if (senderAppVersion == appValidationString) url else null
        }
    }

    internal suspend fun sendUrlArg(urlArg: String, port: Int) {
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

                val okResponse = socket.use {
                    sendUrlToSocket(socket, urlArg)
                }

                okResponse.fold(
                    {
                        if (it != okResponseString) {
                            log.debug { "faulty reply received. Opening app instead" }
                            listenForArgs(urlArg) // fallback
                        } else {
                            log.debug { " url successfully send. Invoking close of app" }
                            closeApp?.invoke()
                        }
                    },
                    {
                        if (it is SocketTimeoutException) {
                            log.debug { "no reply received. Opening app instead" }
                            listenForArgs(urlArg) // fallback
                        } else {
                            log.error(it) { "error sending url arg $urlArg. Invoking close of app" }
                            closeApp?.invoke()
                        }
                    },
                )
            } else {
                listenForArgs(urlArg) // fallback
            }
        }
    }

    private fun sendUrlToSocket(socket: Socket, url: String): Result<String> {

        return runCatching {
            val outputStream = socket.getOutputStream()
            outputStream.write((url + "\n" + appValidationString).encodeToByteArray())
            socket.shutdownOutput()

            log.debug { "waiting for url received message" }

            socket.soTimeout = 2000
            val inputStream = socket.getInputStream()
            val okResponse = inputStream.readNBytes(8192).decodeToString()

            log.debug { "received ok response $okResponse" }
            okResponse
        }
    }
}

internal interface FileLocker {
    fun lockFile(file: File): Boolean

    fun release()
}

private class RandomAccessFileFileLocker : FileLocker {
    var randomAccessFile: RandomAccessFile? = null
    var channel: FileChannel? = null
    var lock: FileLock? = null

    override fun lockFile(file: File): Boolean {
        randomAccessFile = RandomAccessFile(file, "rw")
        channel = randomAccessFile?.channel
        lock = channel?.tryLock(0, Long.MAX_VALUE, true)
        return (lock != null)
    }

    override fun release() {
        lock?.release()
        channel?.close()
        randomAccessFile?.close()
    }
}

actual fun platformUriHandlerModule(): Module = module {
    single<FileLocker> { RandomAccessFileFileLocker() }
    single<UriHandler> { UriHandlerImpl(get(), get(), get(), getOrNull(), getKoin()) }
}

val MatrixMessenger.defaultUriHandler: UriHandlerImpl
    get() =
        checkNotNull(di.get<UriHandler>() as? UriHandlerImpl) {
            "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
        }

val MatrixMultiMessenger.defaultUriHandler: UriHandlerImpl
    get() =
        checkNotNull(di.get<UriHandler>() as? UriHandlerImpl) {
            "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
        }
