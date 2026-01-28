import com.vanniktech.maven.publish.MavenPublishBasePlugin
import de.connect2x.conventions.c2xOrganization
import de.connect2x.conventions.defaultDependencyLocking
import de.connect2x.conventions.defaultPublishing
import de.connect2x.conventions.setProjectInfo
import de.connect2x.conventions.withVersionSuffix
import org.jetbrains.dokka.gradle.DokkaPlugin

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform) apply false
    alias(sharedLibs.plugins.kotlin.jvm) apply false
    alias(sharedLibs.plugins.kotlin.serialization) apply false
    alias(sharedLibs.plugins.kotlin.parcelize) apply false
    alias(sharedLibs.plugins.kotlinx.kover) apply false
    alias(sharedLibs.plugins.android.library) apply false
    alias(sharedLibs.plugins.android.application) apply false
    alias(sharedLibs.plugins.aboutLibraries.plugin) apply false
    alias(sharedLibs.plugins.mokkery) apply false
    alias(sharedLibs.plugins.skie) apply false
    alias(sharedLibs.plugins.kmmBridge) apply false
    alias(sharedLibs.plugins.compose.multiplatform) apply false
    alias(sharedLibs.plugins.dokka)
    alias(sharedLibs.plugins.google.services) apply false
    alias(libs.plugins.seskar) apply false
    `maven-publish`
    alias(sharedLibs.plugins.c2xConventions)
    alias(sharedLibs.plugins.mavenPublish) apply false
}

allprojects {
    group = "de.connect2x.trixnity.messenger"
    version = withVersionSuffix(rootProject.libs.versions.trixnityMessenger)
    if (System.getenv("WITH_LOCK")?.toBoolean() == true) {
        defaultDependencyLocking()
    }
}

subprojects {
    val isTrixnityProject = project.name.startsWith("trixnity-") && !project.name.endsWith("app")
    val isJsWrapper = project.name.startsWith("wrappers-")
    if (isTrixnityProject || isJsWrapper) {
        apply<DokkaPlugin>()
        apply<MavenPublishPlugin>()
        apply<MavenPublishBasePlugin>()
        apply<SigningPlugin>()
        defaultPublishing()

        publishing {
            publications.withType<MavenPublication>().configureEach {
                pom {
                    setProjectInfo(
                        name = project.name,
                        description = "Multiplatform Kotlin SDK for Matrix messengers",
                        repository = "connect2x/trixnity-messenger/trixnity-messenger"
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
