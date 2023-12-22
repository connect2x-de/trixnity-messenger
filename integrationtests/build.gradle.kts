plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization").version(Versions.kotlin)
}

kotlin {
    jvmToolchain(Versions.kotlinJvmTarget.number)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = Versions.kotlinJvmTarget.toString()
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // testLogging.showStandardStreams = true   // activate when detailed information in tests is required
        }
        tasks.withType<Test>().configureEach {
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {}
        val jvmTest by getting {
            dependencies {
                implementation(project(":trixnity-messenger"))
                implementation("net.folivo:trixnity-client-repository-exposed:${Versions.trixnity}")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinxCoroutines}")
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-common:${Versions.kotest}")
                implementation("io.kotest:kotest-assertions-core:${Versions.kotest}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDatetime}")
                implementation("com.benasher44:uuid:${Versions.uuid}")

                implementation("io.ktor:ktor-client-java:${Versions.ktor}")
                implementation("io.ktor:ktor-client-logging:${Versions.ktor}")
                implementation("org.testcontainers:testcontainers:${Versions.testContainers}")
                implementation("org.testcontainers:junit-jupiter:${Versions.testContainers}")
                implementation("ch.qos.logback:logback-classic:${Versions.logback}")
                implementation("com.squareup.okio:okio-fakefilesystem:${Versions.okio}")
            }
        }
    }
}