import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

object Versions {
    const val trixnityMessenger = "1.0.0"

    val kotlinJvmTarget = JavaVersion.VERSION_11
    const val trixnity = "3.7.3-SNAPSHOT" // https://gitlab.com/trixnity/trixnity/-/releases
    const val kotlin = "1.8.22" // https://kotlinlang.org/
    const val ksp = "1.8.22-1.0.11" // https://github.com/google/ksp/releases
    const val kotlinxCoroutines = "1.7.2" // https://github.com/Kotlin/kotlinx.coroutines/releases
    const val kotlinxSerialization = "1.5.1" // https://github.com/Kotlin/kotlinx.serialization/releases
    const val decompose = "2.0.0" // https://github.com/arkivanov/Decompose/releases
    const val essenty = "1.1.0" // https://github.com/arkivanov/Essenty/releases
    const val kotlinxDatetime = "0.4.0" // https://github.com/Kotlin/kotlinx-datetime/releases
    const val koin = "3.4.2" // https://github.com/InsertKoinIO/koin/tags
    const val ktor = "2.3.2" // https://github.com/ktorio/ktor/releases
    const val mocKmp = "1.14.0" // https://github.com/Kodein-Framework/MocKMP/releases
    const val kotest = "5.6.2" // https://github.com/kotest/kotest/releases
    const val jna = "5.13.0" // https://github.com/java-native-access/jna/tags
    const val okio = "3.4.0" // https://square.github.io/okio/changelog
    const val uuid = "0.7.1" // https://github.com/benasher44/uuid/releases
    const val multiplatformSettings = "1.0.0" // https://github.com/russhwolf/multiplatform-settings/releases
    const val korge = "4.0.8" // https://github.com/korlibs/korge/releases

    // integration tests
    const val testContainers = "1.18.3" // https://github.com/testcontainers/testcontainers-java/releases
    const val h2 = "2.1.214" // https://github.com/h2database/h2database/releases

    const val kotlinLogging = "4.0.1" // https://github.com/MicroUtils/kotlin-logging/releases
    const val logback = "1.4.8" // https://github.com/qos-ch/logback/tags

    const val crypto = "1.1.0-alpha06" // https://developer.android.com/jetpack/androidx/releases/security
    const val activity = "1.7.2" // https://developer.android.com/jetpack/androidx/releases/activity
    const val androidGradle = "7.4.2" // https://developer.android.com/reference/tools/gradle-api
}

val JavaVersion.number: Int
    get() = JavaLanguageVersion.of(this.majorVersion).asInt()
