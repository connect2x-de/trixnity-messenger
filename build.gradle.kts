import de.connect2x.conventions.authenticatedPackageRegistry
import de.connect2x.conventions.c2xOrganization
import de.connect2x.conventions.configureJava
import de.connect2x.conventions.isCI
import de.connect2x.conventions.withVersionSuffix

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
                authenticatedPackageRegistry()
            }
            publications.withType<MavenPublication>().configureEach {
                pom {
                    name = project.name
                    description = "Multiplatform Kotlin SDK for Matrix messengers"
                    url = "https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger"
                    c2xOrganization()
                    licenses {
                        license {
                            name = "GNU AFFERO GENERAL PUBLIC LICENSE version 3"
                            url = "https://www.gnu.org/licenses/agpl-3.0.html"
                        }
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
