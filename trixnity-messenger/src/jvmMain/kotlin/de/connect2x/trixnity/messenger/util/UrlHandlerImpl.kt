package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.OS
import de.connect2x.trixnity.messenger.closeApp
import de.connect2x.trixnity.messenger.getAppFolder
import de.connect2x.trixnity.messenger.getOs
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import korlibs.io.async.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val log = KotlinLogging.logger { }

actual class UrlHandler actual constructor(filter: (Url) -> Boolean) : UrlHandlerBase(filter), Flow<Url> {

    private val started = MutableStateFlow(false)
    private val lockFileName = "port.lock"

    /**
     * This need to be called with application start arguments.
     */
    suspend fun start(args: Array<String>) = withContext(Dispatchers.IO) {
        val os = getOs()
        when {
            Desktop.isDesktopSupported() && os == OS.MAC_OS -> {
                Desktop.getDesktop().setOpenURIHandler { event ->
                    val url = Url(event.uri)
                    urlHandlerFlow.tryEmit(url)
                }
            }

            os == OS.WINDOWS || os == OS.LINUX -> {
                if (started.updateAndGet { true }.not()) return@withContext
                val urlArg = args.firstOrNull()

                val port = readPortFromLockFile()
                if (port == null) {
                    listenForArgs(urlArg)
                } else {
                    urlArg?.sendUrlArg(port)
                }
            }

            else -> log.warn("this platform is not supported to listen for uris via args")
        }
    }

    private fun String.toUrl() =
        if (isNotBlank())
            try {
                Url(this)
            } catch (exception: URLParserException) {
                log.error(exception) { "could not parse url from arg $this" }
                null
            }
        else null

    private fun Url.emit() {
        urlHandlerFlow.tryEmit(this)
    }

    private fun readPortFromLockFile(): Int? {
        val lockFile = getAppFolder(null).resolve(lockFileName)
        return if (lockFile.exists()) {
            lockFile.readText().toInt()
        } else null
    }

    private fun writePortToLockFile(port: Int) {
        log.debug("write port $port to lock file")
        val lockFile = getAppFolder(null).resolve(lockFileName)
        lockFile.writeText(port.toString())
        val randomAccessFile = RandomAccessFile(lockFile.toFile(), "rw")
        val channel = randomAccessFile.getChannel()
        val lock = channel.tryLock(0, Long.MAX_VALUE, true)
        if (lock == null) {
            channel.close()
            randomAccessFile.close()
            lockFile.deleteIfExists()
            throw IllegalStateException("could not lock $lockFileName")
        }
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            lock.release()
            channel.close()
            randomAccessFile.close()
            lockFile.deleteIfExists()
        })
    }

    private fun listenForArgs(urlArg: String?) {
        thread {
            try {
                runBlocking(Dispatchers.IO) {
                    urlArg?.toUrl()?.emit()

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
                        log.debug("start listening for url args")
                        while (server.isClosed.not()) {
                            try {
                                val socket = server.accept()
                                launch {
                                    try {
                                        log.debug("try read url arg")
                                        val inputStream = socket.getInputStream()
                                        val bytes = inputStream.readAllBytes()
                                        inputStream.close()
                                        bytes.decodeToString().toUrl()?.emit()
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
                    } else log.error("could not start server socket to listen for url args")
                }
            } catch (exception: Exception) {
                log.error(exception) { "error in listenForArgs thread" }
                throw exception
            }
        }
    }

    private suspend fun String.sendUrlArg(port: Int) {
        val urlArg = this
        withContext(Dispatchers.IO) {
            val address = InetAddress.getLoopbackAddress()
            val socket =
                try {
                    Socket(address, port)
                } catch (exception: Exception) {
                    log.error("could not open client socket on $port to send url arg")
                    null
                }
            if (socket != null) {
                log.debug("try send url arg $urlArg using port $port")
                try {
                    val outputStream = socket.getOutputStream()
                    outputStream.write(urlArg.encodeToByteArray())
                    outputStream.close()
                } catch (exception: Exception) {
                    log.error(exception) { "error sending url arg $urlArg" }
                } finally {
                    socket.close()
                }
                closeApp()
            } else {
                listenForArgs(null) // fallback
            }
        }
    }
}