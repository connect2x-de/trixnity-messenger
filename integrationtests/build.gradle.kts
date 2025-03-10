plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization").version(libs.versions.kotlin.get())
    alias(libs.plugins.kotlinx.kover)
}

kotlin {
    val kotlinJvm = libs.versions.kotlinJvmTarget.get()
    jvmToolchain(kotlinJvm.toInt())
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvm
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // testLogging.showStandardStreams = true   // activate when detailed information in tests is required
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {}
        val jvmTest by getting {
            dependencies {
                implementation(projects.trixnityMessenger)
                implementation(libs.trixnity.client)
                implementation(libs.trixnity.client.repository.exposed)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
                implementation(libs.kotest.common)
                implementation(libs.kotest.assertion.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.uuid)
//                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.java)
                implementation(libs.bundles.testcontainers)
                implementation(libs.logback.classic)
                implementation(libs.okio.fakefilesystem)
            }
        }
    }
}

tasks.register("testCoverage") {
    val reportTask = tasks.named("koverXmlReportJvm").get()
    dependsOn(reportTask)
    doLast {
        val regex = """<counter type="INSTRUCTION" missed="(\d+)" covered="(\d+)"/>""".toRegex()
        for (file in reportTask.outputs.files) {
            file.useLines { lines ->
                val coverage = lines.last(regex::containsMatchIn)
                regex.find(coverage)?.let { coverageData ->
                    val covered = coverageData.groupValues[2].toInt()
                    val missed = coverageData.groupValues[1].toInt()
                    println("Total test coverage: ${covered * 100 / (missed + covered)}%")
                }
            }
        }
    }
}
