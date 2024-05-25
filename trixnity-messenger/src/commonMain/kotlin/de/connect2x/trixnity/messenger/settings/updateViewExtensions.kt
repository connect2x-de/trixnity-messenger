package de.connect2x.trixnity.messenger.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountPlatformSettings
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerPlatformSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettings
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import net.folivo.trixnity.core.model.UserId
import kotlin.jvm.JvmName

suspend fun <T : SettingsView<MatrixMessengerAccountSettings>> MatrixMessengerSettingsHolder.updateView(
    userId: UserId,
    serializer: KSerializer<T>,
    updater: (T) -> T,
) = update(userId) {
    set(updater(it.get(serializer)), serializer)
}

suspend inline fun <reified T : SettingsView<MatrixMessengerAccountSettings>> MatrixMessengerSettingsHolder.updateView(
    userId: UserId,
    noinline updater: (T) -> T,
) = updateView(userId, serializer(), updater)

suspend inline fun <reified T : SettingsView<MatrixMessengerSettings>> MatrixMessengerSettingsHolder.updateView(
    noinline updater: (T) -> T,
) = updateView(serializer(), updater)

suspend inline fun <reified T : SettingsView<MatrixMultiMessengerSettings>> MatrixMultiMessengerSettingsHolder.updateView(
    noinline updater: (T) -> T,
) = updateView(serializer(), updater)

suspend fun <T : SettingsView<MatrixMessengerPlatformSettings>> MatrixMessengerSettingsHolder.updateView(
    serializer: KSerializer<T>,
    updater: (T) -> T,
) = updateView<MatrixMessengerSettingsBase> {
    val newSettings = MutableSettingsImpl(it.platform)
    with(newSettings) {
        set(updater(it.platform.get(serializer)), serializer)
    }
    it.copy(platform = MatrixMessengerPlatformSettings(newSettings))
}

@JvmName("updatePlatformView")
suspend inline fun <reified T : SettingsView<MatrixMessengerPlatformSettings>> MatrixMessengerSettingsHolder.updateView(
    noinline updater: (T) -> T,
) = updateView(serializer(), updater)

@JvmName("updatePlatformView")
suspend fun <T : SettingsView<MatrixMessengerAccountPlatformSettings>> MatrixMessengerSettingsHolder.updateView(
    userId: UserId,
    serializer: KSerializer<T>,
    updater: (T) -> T,
) = updateView<MatrixMessengerAccountSettingsBase>(userId) {
    val newSettings = MutableSettingsImpl(it.platform)
    with(newSettings) {
        set(updater(it.platform.get(serializer)), serializer)
    }
    it.copy(platform = MatrixMessengerAccountPlatformSettings(newSettings))
}

@JvmName("updatePlatformView")
suspend inline fun <reified T : SettingsView<MatrixMessengerAccountPlatformSettings>> MatrixMessengerSettingsHolder.updateView(
    userId: UserId,
    noinline updater: (T) -> T,
) = updateView(userId, serializer(), updater)
