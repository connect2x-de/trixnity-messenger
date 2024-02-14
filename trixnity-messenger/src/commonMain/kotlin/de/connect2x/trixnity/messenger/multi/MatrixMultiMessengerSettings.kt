package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.SettingsHolder
import kotlinx.serialization.Serializable
import org.koin.core.module.Module
import kotlin.jvm.JvmInline

@Serializable
data class MatrixMultiMessengerSettings(
    val profiles: Map<String, MatrixMultiMessengerProfileSettings> = mapOf(),
    val activeProfile: String? = null,
    val forgetActiveProfileOnStart: Boolean = false,
)

@Serializable
data class MatrixMultiMessengerProfileSettings(
    val displayName: String? = null,
)

interface MatrixMultiMessengerSettingsHolder : SettingsHolder<MatrixMultiMessengerSettings>

@JvmInline
value class MatrixMultiMessengerSettingsHolderImpl(private val delegate: SettingsHolder<MatrixMultiMessengerSettings>) :
    SettingsHolder<MatrixMultiMessengerSettings> by delegate, MatrixMultiMessengerSettingsHolder

expect fun platformMatrixMultiMessengerSettingsHolderModule(): Module