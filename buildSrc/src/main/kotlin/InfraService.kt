import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

interface InfraServiceParams : BuildServiceParameters {
    val projectDir: DirectoryProperty
}

abstract class InfraService : BuildService<InfraServiceParams>, AutoCloseable {

    private var logger: Logger? = null

    fun startInfra(logger: Logger) {
        println("Starting infraService")
        this.logger = logger
        val dir = parameters.projectDir.get().asFile

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
            error("Command failed with exit code $exitCodeDocker")
        }

        val addUsers = ProcessBuilder(
            "bash",
            "$dir/src/commonTest/resources/localInfra/addUsers.sh"
        )
            .redirectErrorStream(true)
            .start()
        streamLogs(addUsers)
        val exitCodeAddUsers = addUsers.waitFor()
        if (exitCodeAddUsers != 0) {
            error("Command failed with exit code $exitCodeAddUsers")
        }
    }

    override fun close() {
        println("Stopping infrastructure")
        val dir = parameters.projectDir.get().asFile

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
            error("Command failed with exit code $exitCodeDocker")
        }
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
