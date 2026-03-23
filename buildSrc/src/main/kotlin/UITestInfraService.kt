import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
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

        if (synapseNotRunning()) {
            startSynapse(dir)
            createAdmin(dir)
            deleteOldScreenshots(dir)
        }
    }

    override fun close() {
        println("Stopping UITest infra service")
        val dir = parameters.projectDir.get().asFile

        stopSynapse(dir)
        cleanupDb(dir)
    }

    private fun synapseNotRunning(): Boolean {
        val startDocker = ProcessBuilder(
            "docker",
            "ps",
        )
            .redirectErrorStream(true)
            .start()
        val output: String?
        startDocker.inputStream.use { `is` ->
            ByteArrayOutputStream().use { baos ->
                val buffer = ByteArray(1024)
                var length: Int
                while ((`is`.read(buffer).also { length = it }) != -1) {
                    baos.write(buffer, 0, length)
                }
                output = baos.toString(StandardCharsets.UTF_8)
            }
        }
        return (output != null && output.contains("uitest-synapse")).not()
    }

    private fun startSynapse(dir: File) {
        val startDocker = ProcessBuilder(
            "docker",
            "compose",
            "-f",
            "$dir/src/commonTest/resources/localInfra/docker-compose.yml",
            *ciEnv(dir),
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
            *ciEnv(dir),
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

    private fun ciEnv(dir: File): Array<String> {
        return if (System.getenv("CI")?.toBooleanStrictOrNull() == true) {
            listOf("-f", "$dir/src/commonTest/resources/localInfra/docker-compose-ci.yml").toTypedArray()
        } else emptyArray<String>()
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
