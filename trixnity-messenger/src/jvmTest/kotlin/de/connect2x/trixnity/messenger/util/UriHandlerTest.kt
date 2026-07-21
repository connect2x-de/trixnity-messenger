package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class UriHandlerTest {

    private val config1 = MatrixMessengerConfiguration()
    private val config2 = MatrixMessengerConfiguration()

    private var fakeFileSystem: FakeFileSystem = FakeFileSystem()
    private val rootPath = RootPath("/directory".toPath())

    private val closeApp = CloseApp {}

    private var server: AutoCloseable? = null

    private val di =
        koinApplication {
                modules(
                    module {
                        single<FileLocker> {
                            object : FileLocker {
                                override fun lockFile(file: File): Boolean = true

                                override fun release() {}
                            }
                        }
                    }
                )
            }
            .koin

    init {
        fakeFileSystem.createDirectories(rootPath.path)
        config1.appName = "App1"
        config2.appName = "App2"
    }

    @AfterTest
    fun cleanup() {
        server?.close()
        server = null

        fakeFileSystem.delete(rootPath.path.resolve("port.lock"))
    }

    @Test
    fun `uriHandler writes port to port lock file when opening server`() = runTest {
        val handler1 = cut()

        server = handler1.listenForArgs(null)

        eventually(2.seconds) {
            runCatching { fakeFileSystem.read(rootPath.path.resolve("port.lock")) { readUtf8() } }.getOrNull() shouldBe
                "2424"
        }
    }

    @Test
    fun `uriHandler accepts uri from app with same name`() = runTest {
        val handler1 = cut(config1)
        var receivedUri = ""
        backgroundScope.launch {
            handler1.collect {
                receivedUri = it
                println(it)
            }
        }
        server = handler1.listenForArgs(null)

        val handler2 = cut(config1)

        handler2.sendUrlArg(config1.appUri + "abc", 2424)

        eventually(2.seconds) { receivedUri shouldBe config1.appUri + "abc" }
    }

    @Test
    fun `uriHandler does not accept uri from app with different name and new server is started with different port`() =
        runTest {
            val handler1 = cut(config1)
            var receivedUri = ""
            backgroundScope.launch { handler1.collect { receivedUri = it } }
            server = handler1.listenForArgs(null)

            val handler2 = cut(config2)

            handler2.sendUrlArg(config1.appUri + "abc", 2424)

            eventually(2.seconds) { receivedUri shouldBe "" }

            eventually(2.seconds) {
                runCatching { fakeFileSystem.read(rootPath.path.resolve("port.lock")) { readUtf8() } }
                    .getOrNull() shouldBe "2425"
            }
        }

    @Test
    fun `uriHandler does not accept uri from instance that does not send verification string`() = runTest {
        val handler1 = cut(config1)
        var receivedUri = ""
        backgroundScope.launch {
            handler1.collect {
                receivedUri = it
                println(it)
            }
        }
        server = handler1.listenForArgs(null)

        delay(1.seconds)

        val socket = Socket(InetAddress.getLoopbackAddress(), 2424)
        socket.use { it.getOutputStream().write((config1.appUri + "abc").encodeToByteArray()) }

        eventually(2.seconds) { receivedUri shouldBe "" }
    }

    @Test
    fun `uriHandler starts server after sending and no verification string was send back`() = runTest {
        val address = InetAddress.getLoopbackAddress()
        var port = 2424
        var serverSocket = ServerSocket(port, 0, address)
        server = serverSocket

        backgroundScope.launch {
            val socket = serverSocket.accept()
            socket.use { it.getInputStream().readAllBytes() }
        }

        val handler2 = cut(config2)

        handler2.sendUrlArg(config1.appUri + "abc", 2424)

        eventually(2.seconds) {
            runCatching { fakeFileSystem.read(rootPath.path.resolve("port.lock")) { readUtf8() } }
                .getOrNull() shouldNotBe "2424"
        }
    }

    private fun cut(config: MatrixMessengerConfiguration = config1): UriHandlerImpl {
        return UriHandlerImpl(config, fakeFileSystem, rootPath, closeApp, di)
    }
}
