import de.connect2x.conventions.c2xOrganization
import de.connect2x.conventions.defaultPublishing
import de.connect2x.conventions.setProjectInfo
import de.connect2x.conventions.withVersionSuffix

plugins {
  alias(sharedLibs.plugins.c2xConventions)
  alias(sharedLibs.plugins.kotlin.multiplatform) apply false
  alias(sharedLibs.plugins.compose.compiler) apply false
  alias(sharedLibs.plugins.compose.multiplatform) apply false
  alias(sharedLibs.plugins.android.library) apply false
  alias(sharedLibs.plugins.android.application) apply false
  alias(sharedLibs.plugins.mavenPublish) apply false
  alias(libs.plugins.spotless)
}

registerAggregateTasks(project)

allprojects {
  group = "de.connect2x.livekitnity"
  version = withVersionSuffix(rootProject.libs.versions.livekitnity)

  if (shouldBePublished(project)) {
    apply {
      plugin<com.vanniktech.maven.publish.MavenPublishPlugin>()
      plugin<SigningPlugin>()
    }
    defaultPublishing()

    extensions.configure<PublishingExtension> {
      publications.withType<MavenPublication>().configureEach {
        pom {
          setProjectInfo(
              name = project.name,
              description = "Kotlin Multiplatform SDK for LiveKit",
              repository = "connect2x/LiveKitnity",
          )
          c2xOrganization()
          licenses {
            license {
              name = "GNU AFFERO GENERAL PUBLIC LICENSE version 3"
              url = "https://www.gnu.org/licenses/agpl-3.0.html"
            }
          }
        }
      }
    }
  }
}

spotless {
  kotlinGradle {
    ktfmt().metaStyle()
  }
}

private fun registerAggregateTasks(project: Project) {
  aggregate(project, false, "spotlessApply")
  aggregate(project, false, "spotlessCheck")
  aggregate(project, true, "publishToMavenLocal", ::shouldBePublished)
  aggregate(project, true, "publishAndReleaseToMavenCentral", ::shouldBePublished)
  aggregate(project, true, "publishAllPublicationsToGitLabRepository", ::shouldBePublished)
}

private fun shouldBePublished(project: Project): Boolean {
  return project.name.startsWith("livekitnity-")
}

private fun aggregate(
    project: Project,
    create: Boolean,
    taskName: String,
    filter: (Project) -> Boolean = { true },
) {
  fun doFilter(project: Project): Boolean {
    return project.buildFile.exists() && filter(project)
  }
  fun computeTaskName(project: Project): String {
    return "${project.path}:$taskName"
  }

  val taskNames = subprojects.filter(::doFilter).map(::computeTaskName)

  if (create) {
    tasks.register(taskName)
  }

  project.tasks.named { it == taskName }.configureEach { dependsOn(taskNames) }
}
