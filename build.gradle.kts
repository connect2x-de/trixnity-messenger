import de.connect2x.conventions.configureJava
import de.connect2x.conventions.isCI
import de.connect2x.conventions.withVersionSuffix
import de.connect2x.conventions.c2xDeveloper
import de.connect2x.conventions.c2xContributor

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotest) apply false
    alias(libs.plugins.mokkery) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.kmmbridge) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.dokka)
    `maven-publish`
    alias(libs.plugins.c2xConventions)
}

allprojects {
    group = "de.connect2x"
    version = withVersionSuffix(rootProject.libs.versions.trixnityMessenger)
    configureJava(rootProject.libs.versions.jvmTarget)

    if (System.getenv("WITH_LOCK")?.toBoolean() == true) {
        dependencyLocking {
            lockAllConfigurations()
        }
        val dependenciesForAll by tasks.registering(DependencyReportTask::class)
    }
}

subprojects {
    if (project.name.startsWith("trixnity-") && !project.name.endsWith("app")) {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "maven-publish")

        val dokkaJar by tasks.registering(Jar::class) {
            dependsOn("dokkaGenerate")
            from(dokka.dokkaPublications.html.flatMap { it.outputDirectory })
            archiveClassifier = "javadoc"
            onlyIf { isCI }
        }

        publishing {
            repositories {
                maven {
                    url =
                        uri("${System.getenv("CI_API_V4_URL")}/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
                    name = "GitLab"
                    credentials(HttpHeaderCredentials::class) {
                        name = "Job-Token"
                        value = System.getenv("CI_JOB_TOKEN")
                    }
                    authentication {
                        create("header", HttpHeaderAuthentication::class)
                    }
                }
            }
            publications.withType<MavenPublication>().configureEach {
                pom {
                    name = project.name
                    description = "Multiplatform Kotlin SDK for Matrix messengers"
                    url = "https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger"
                    licenses {
                        license {
                            name = "GNU AFFERO GENERAL PUBLIC LICENSE version 3"
                            url = "https://www.gnu.org/licenses/agpl-3.0.html"
                        }
                    }
                    developers {
                        c2xDeveloper("benkuly", "Benedict Benken")
                        c2xDeveloper("michael.thiele", "Michael Thiele")
                        c2xDeveloper("janne.koschinski", "Janne Koschinski")
                        c2xDeveloper("KitsuneAlex", "Alexander Hinze")
                        c2xDeveloper("cach30verfl0w", "Cedric Hammes")
                    }
                    contributors {
                        c2xContributor("fhilgers", "Felix Hilgers")
                        c2xContributor("adambrangenberg", "Adam Brangenberg")
                        c2xContributor("jakob.deutschendorf", "Jakob Deutschendorf")
                    }
                    scm {
                        url = this@pom.url
                    }
                }
                if (isCI) artifact(dokkaJar)
            }
        }
    }
}
