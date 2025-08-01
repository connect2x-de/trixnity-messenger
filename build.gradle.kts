import de.connect2x.conventions.authenticatedPackageRegistry
import de.connect2x.conventions.c2xOrganization
import de.connect2x.conventions.defaultDependencyLocking
import de.connect2x.conventions.isCI
import de.connect2x.conventions.withVersionSuffix

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform) apply false
    alias(sharedLibs.plugins.kotlin.jvm) apply false
    alias(sharedLibs.plugins.kotlin.serialization) apply false
    alias(sharedLibs.plugins.kotlin.parcelize) apply false
    alias(sharedLibs.plugins.kotlinx.kover) apply false
    alias(sharedLibs.plugins.android.library) apply false
    alias(sharedLibs.plugins.android.application) apply false
    alias(sharedLibs.plugins.aboutLibraries.plugin) apply false
    alias(sharedLibs.plugins.kotest) apply false
    alias(sharedLibs.plugins.mokkery) apply false
    alias(sharedLibs.plugins.skie) apply false
    alias(sharedLibs.plugins.kmmBridge) apply false
    alias(sharedLibs.plugins.compose.multiplatform) apply false
    alias(sharedLibs.plugins.dokka)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.seskar) apply false
    `maven-publish`
    alias(libs.plugins.c2xConventions)
}

allprojects {
    group = "de.connect2x"
    version = withVersionSuffix(rootProject.libs.versions.trixnityMessenger)
    if (System.getenv("WITH_LOCK")?.toBoolean() == true) {
        defaultDependencyLocking()
    }
}

subprojects {
    val isTrixnityProject = project.name.startsWith("trixnity-") && !project.name.endsWith("app")
    val isJsWrapper = project.name.startsWith("wrappers-")
    if (isTrixnityProject || isJsWrapper) {
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
