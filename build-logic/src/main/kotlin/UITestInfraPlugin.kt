import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class UITestInfraPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val serviceProvider: Provider<UITestInfraService> =
            project.gradle.sharedServices.registerIfAbsent("uiTestInfra", UITestInfraService::class.java) {
                parameters.projectDir.set(project.layout.projectDirectory)
            }

        // Auto-attach to any task with "UITest" or "uiTest" in its name
        project.tasks.configureEach {
            if (name.contains("UITest", ignoreCase = true)) {
                usesService(serviceProvider)
                doFirst { serviceProvider.get().startInfra(logger) }
            }
        }
    }
}
