import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

object Versions {
    const val trixnityMessenger = "2.0.0"

    val kotlinJvmTarget = JavaVersion.VERSION_11
    const val trixnity = "4.2.7" // https://gitlab.com/trixnity/trixnity/-/releases
    const val kotlin = "1.9.23" // https://kotlinlang.org/
    const val ksp = "1.9.23-1.0.19" // https://github.com/google/ksp/releases
    const val kotlinWrappers = "1.0.0-pre.718" // https://github.com/JetBrains/kotlin-wrappers/releases
    const val kotlinxCoroutines = "1.8.0" // https://github.com/Kotlin/kotlinx.coroutines/releases
    const val kotlinxSerialization = "1.6.3" // https://github.com/Kotlin/kotlinx.serialization/releases
    const val decompose = "2.2.2" // https://github.com/arkivanov/Decompose/releases
    const val kotlinxDatetime = "0.5.0" // https://github.com/Kotlin/kotlinx-datetime/releases
    const val koin = "3.5.3" // https://github.com/InsertKoinIO/koin/tags
    const val ktor = "2.3.9" // https://github.com/ktorio/ktor/releases
    const val mocKmp = "1.17.0" // https://github.com/Kodein-Framework/MocKMP/releases
    const val kotest = "5.8.1" // https://github.com/kotest/kotest/releases
    const val jna = "5.14.0" // https://github.com/java-native-access/jna/tags
    const val okio = "3.9.0" // https://square.github.io/okio/changelog
    const val uuid = "0.8.4" // https://github.com/benasher44/uuid/releases
    const val korge = "4.0.10" // https://github.com/korlibs/korge/releases
    const val dokka = "1.9.20" // https://github.com/Kotlin/dokka/releases"
    const val jsJoda = "2.18.3" // https://www.npmjs.com/package/@js-joda/timezone
    const val skie = "0.6.2" // https://github.com/touchlab/SKIE/releases
    const val kmmBridge = "0.5.2" // https://github.com/touchlab/KMMBridge/releases

    // integration tests
    const val testContainers = "1.19.7" // https://github.com/testcontainers/testcontainers-java/releases

    const val kotlinLogging = "6.0.3" // https://github.com/MicroUtils/kotlin-logging/releases
    const val logback = "1.5.3" // https://github.com/qos-ch/logback/tags

    const val crypto = "1.1.0-alpha06" // https://developer.android.com/jetpack/androidx/releases/security
    const val activity = "1.8.2" // https://developer.android.com/jetpack/androidx/releases/activity
    const val androidGradle = "8.2.2" // https://developer.android.com/reference/tools/gradle-api
}

val JavaVersion.number: Int
    get() = JavaLanguageVersion.of(this.majorVersion).asInt()
