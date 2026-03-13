import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively

interface UITestInfraServiceParams : BuildServiceParameters {
    val projectDir: DirectoryProperty
}

abstract class UITestInfraService : BuildService<UITestInfraServiceParams>, AutoCloseable {

    private var logger: Logger? = null

    fun startInfra(logger: Logger) {
        println("Starting UITest infra service")
        this.logger = logger
        val dir = parameters.projectDir.get().asFile

        startSynapse(dir)
        createAdmin(dir)
        deleteOldScreenshots(dir)
    }

    override fun close() {
        println("Stopping UITest infra service")
        val dir = parameters.projectDir.get().asFile

        stopSynapse(dir)
        cleanupDb(dir)
    }

    private fun startSynapse(dir: File) {
        val startDocker = ProcessBuilder(
            "docker",
            "compose",
            "-f",
            "$dir/src/commonTest/resources/localInfra/docker-compose.yml",
            "up",
            "-d"
        )
            .redirectErrorStream(true)
            .start()
        streamLogs(startDocker)
        val exitCodeDocker = startDocker.waitFor()
        if (exitCodeDocker != 0) {
            error("Could not create Synapse docker container, exit code $exitCodeDocker")
        }
    }

    private fun createAdmin(dir: File) {
        val addUsersBuilder = ProcessBuilder(
            "bash",
            "$dir/src/commonTest/resources/localInfra/createAdmin.sh"
        )
            .redirectErrorStream(true)
        addUsersBuilder.directory(File("$dir/src/commonTest/resources/localInfra"))
        val addUsers = addUsersBuilder.start()
        streamLogs(addUsers)
        val exitCodeAddUsers = addUsers.waitFor()
        if (exitCodeAddUsers != 0) {
            logger?.warn("Admin user was not created (maybe already exists?).")
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun deleteOldScreenshots(dir: File) {
        Path("$dir/screenshots").deleteRecursively()
    }

    private fun stopSynapse(dir: File) {
        val stopDocker = ProcessBuilder(
            "docker",
            "compose",
            "-f",
            "$dir/src/commonTest/resources/localInfra/docker-compose.yml",
            "down"
        )
            .redirectErrorStream(true)
            .start()
        streamLogs(stopDocker)
        val exitCodeDocker = stopDocker.waitFor()
        if (exitCodeDocker != 0) {
            logger?.warn("Could not shut down Synapse docker container.")
        }
    }

    private fun cleanupDb(dir: File) {
        Path("$dir/src/commonTest/resources/localInfra/data/homeserver.db").deleteExisting()
    }

    private fun streamLogs(process: Process) {
        Thread {
            process.inputStream.bufferedReader().useLines {
                it.forEach { line -> logger?.lifecycle("[infra] $line") }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }
}
