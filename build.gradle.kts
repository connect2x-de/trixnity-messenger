buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${libs.versions.androidGradle.get()}")
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
        // ui
        classpath(libs.gms.google.services)
    }
}

plugins {
    alias(libs.plugins.kotest).apply(false)
    kotlin("plugin.serialization") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.mokkery).apply(false)
    alias(libs.plugins.skie).apply(false)
    alias(libs.plugins.kmmbridge).apply(false)
    `maven-publish`
    alias(libs.plugins.dokka)
    // ui
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
}

allprojects {
    group = "de.connect2x"
    version = withVersionSuffix("2.4.1")

    repositories {
        mavenCentral()
        google()
        mavenLocal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://gitlab.com/api/v4/projects/26519650/packages/maven") // trixnity
        maven("https://gitlab.com/api/v4/projects/58749664/packages/maven") // sysnotify
        maven("https://git.karmakrafts.dev/api/v4/projects/307/packages/maven") // multiplatform jni TODO: remove soon
    }

    if (System.getenv("WITH_LOCK")?.toBoolean() == true) {
        dependencyLocking {
            lockAllConfigurations()
        }

        val dependenciesForAll by tasks.registering(DependencyReportTask::class) {}
    }
}

subprojects {
    if (project.name.startsWith("trixnity-") && !project.name.endsWith("app")) {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "maven-publish")

        val dokkaJar by tasks.registering(Jar::class) {
            dependsOn(tasks.dokkaHtml)
            from(tasks.dokkaHtml.flatMap { it.outputDirectory })
            archiveClassifier.set("javadoc")
            onlyIf { isCI }
        }
        publishing {
            repositories {
                maven {
                    url = uri("${System.getenv("CI_API_V4_URL")}/projects/47538655/packages/maven")
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
                        name.set(project.name)
                        description.set("Multiplatform Kotlin SDK for Matrix messengers")
                        url.set("https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger")
                        licenses {
                            license {
                                name.set("GNU AFFERO GENERAL PUBLIC LICENSE version 3")
                                url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                            }
                        }
                        developers {
                            developer {
                                id.set("michael.thiele")
                                id.set("benkuly")
                            }
                        }
                        scm {
                            url.set("https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger")
                        }
                    }
                    if (isCI) artifact(dokkaJar)
                }
            }
        }
    }
}
