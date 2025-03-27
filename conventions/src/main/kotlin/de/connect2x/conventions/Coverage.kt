package de.connect2x.conventions

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

fun Project.registerCoverageTask() {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    tasks.register("testCoverage") {
        val reportTask = tasks.named("koverXmlReportJvm")
        dependsOn(reportTask)
        doLast {
            val regex = """<counter type="INSTRUCTION" missed="(\d+)" covered="(\d+)"/>""".toRegex()
            for (file in reportTask.get().outputs.files) {
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
}
