import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

interface UITestInfraServiceParams : BuildServiceParameters {
    val projectDir: DirectoryProperty
    val port: Property<Int>
}

@OptIn(ExperimentalAtomicApi::class)
abstract class UITestInfraService : BuildService<UITestInfraServiceParams>, AutoCloseable {

    private var logger: Logger? = null
    private val projectName = "trixnity-messenger-uitest-${parameters.port.get()}"
    private val containerName = "trixnity-messenger-uitest-synapse-${parameters.port.get()}"

    private val isRunning = AtomicBoolean(false)

    fun startInfra(logger: Logger) {
        if (!isRunning.exchange(true)) {
            println("Starting UITest infra service")
            this.logger = logger

            startSynapse()
            waitHealthy()
            createAdmin()
        } else {
            waitHealthy()
        }
    }

    override fun close() {
        println("Stopping UITest infra service")
        stopSynapse()
        isRunning.exchange(false)
    }

    private fun startSynapse() {
        val startDocker =
            ProcessBuilder(
                    "docker",
                    "compose",
                    "-p",
                    projectName,
                    "-f",
                    "${parameters.projectDir.get().asFile}/src/commonTest/resources/localInfra/docker-compose.yml",
                    "up",
                    "-d",
                )
                .apply { environment()["GRADLE_TEST_SYNAPSE_PORT"] = parameters.port.get().toString() }
                .redirectErrorStream(true)
                .start()
        streamLogs(startDocker)
        val exitCodeDocker = startDocker.waitFor()
        if (exitCodeDocker != 0) {
            error("Could not create Synapse docker container, exit code $exitCodeDocker")
        }
    }

    private fun waitHealthy() {
        repeat(30) {
            val startDocker =
                ProcessBuilder("docker", "inspect", "--format='{{.State.Health.Status}}'", containerName)
                    .redirectErrorStream(true)
                    .start()

            val output = startDocker.inputStream.bufferedReader().readText().trim().replace("'", "")
            val exitCode = startDocker.waitFor()

            if (exitCode != 0) {
                error("Could not inspect docker container '$containerName', exit code $exitCode")
            }

            when (output) {
                "healthy" -> {
                    logger?.info("Docker container '$containerName' is healthy now")
                    return
                }
                "unhealthy" -> error("Container '$containerName' became unhealthy")
            }
            logger?.warn("Docker container '$containerName' is not healthy yet ($output)")
            Thread.sleep(1000)
        }
        error("Timed out waiting for container '$containerName' to become healthy")
    }

    private fun createAdmin() {
        val addUsers =
            ProcessBuilder(
                    "docker",
                    "compose",
                    "-p",
                    projectName,
                    "-f",
                    "${parameters.projectDir.get().asFile}/src/commonTest/resources/localInfra/docker-compose.yml",
                    "exec",
                    "synapse",
                    "register_new_matrix_user",
                    "-a",
                    "-u",
                    "admin",
                    "-p",
                    "admin",
                    "-c",
                    "/data/homeserver.yaml",
                )
                .apply { environment()["GRADLE_TEST_SYNAPSE_PORT"] = parameters.port.get().toString() }
                .redirectErrorStream(true)
                .start()
        streamLogs(addUsers)
        val exitCodeAddUsers = addUsers.waitFor()
        if (exitCodeAddUsers != 0) {
            error("Admin user could not be created.")
        }
    }

    private fun stopSynapse() {
        val stopDocker =
            ProcessBuilder(
                    "docker",
                    "compose",
                    "-p",
                    projectName,
                    "-f",
                    "${parameters.projectDir.get().asFile}/src/commonTest/resources/localInfra/docker-compose.yml",
                    "down",
                    "-v",
                )
                .apply { environment()["GRADLE_TEST_SYNAPSE_PORT"] = parameters.port.get().toString() }
                .redirectErrorStream(true)
                .start()
        streamLogs(stopDocker)
        val exitCodeDocker = stopDocker.waitFor()
        if (exitCodeDocker != 0) {
            logger?.warn("Could not shut down Synapse docker container.")
        }
    }

    private fun streamLogs(process: Process) {
        Thread {
                process.inputStream.bufferedReader().useLines {
                    it.forEach { line -> logger?.lifecycle("[infra] $line") }
                }
            }
            .apply {
                isDaemon = true
                start()
            }
    }
}
