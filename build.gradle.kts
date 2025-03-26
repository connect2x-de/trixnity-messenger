plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotest) apply false
    alias(libs.plugins.mokkery) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.kmmbridge) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.dokka)
    `maven-publish`
}

allprojects {
    group = "de.connect2x"
    version = withVersionSuffix(rootProject.libs.versions.trixnityMessenger)
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
            publications.configureEach {
                if (this is MavenPublication) {
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
                            developer {
                                id = "benkuly"
                                name = "Benedict Benken"
                            }
                            developer {
                                id = "michael.thiele"
                                name = "Michael Thiele"
                            }
                            developer {
                                id = "janne.koschinski"
                                name = "Janne Koschinski"
                            }
                            developer {
                                id = "KitsuneAlex"
                                name = "Alexander Hinze"
                            }
                            developer {
                                id = "cach30verfl0w"
                                name = "Cedric Hammes"
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
}
