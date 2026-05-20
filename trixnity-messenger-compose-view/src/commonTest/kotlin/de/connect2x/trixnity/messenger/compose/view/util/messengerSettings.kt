package de.connect2x.trixnity.messenger.compose.view.util

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettings
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive

fun createTestMatrixMessengerSettingsHolder(): MatrixMessengerSettingsHolder {
    val settingsHolder: MutableStateFlow<MatrixMessengerSettings?> =
        MutableStateFlow(MatrixMessengerSettings(mapOf("preferredLang" to JsonPrimitive("en"))))
    val dummyStorage =
        object : SettingsStorage {
            override suspend fun read(): String? = null

            override suspend fun write(settings: String) {}
        }
    val delegate = MatrixMessengerSettingsHolderImpl(dummyStorage, settingsHolder)
    return object : MatrixMessengerSettingsHolder by delegate {
        override fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?> = flow {
            val hasNoEntry = delegate[userId].first() == null
            if (hasNoEntry) delegate.update<MatrixMessengerAccountSettingsBase>(userId) { it }
            emitAll(delegate[userId])
        }
    }
}

fun createTestMatrixMultiMessengerSettingsHolder(): MatrixMultiMessengerSettingsHolder {
    val settingsHolder: MutableStateFlow<MatrixMultiMessengerSettings?> =
        MutableStateFlow(MatrixMultiMessengerSettings(mapOf()))
    val dummyStorage =
        object : SettingsStorage {
            override suspend fun read(): String? = null

            override suspend fun write(settings: String) {}
        }
    return MatrixMultiMessengerSettingsHolderImpl(dummyStorage, settingsHolder)
}
