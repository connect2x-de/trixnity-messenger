@file:OptIn(MSC4143::class, TrixnityMessengerCallApi::class)

package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.e2ee.E2EEOptions
import de.connect2x.livekitnity.api.e2ee.KeyDerivationAlgorithm
import de.connect2x.livekitnity.api.e2ee.KeyProviderOptions
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.compose.local.LiveKitLocal
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel.LiveKitCallParameters
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class CallRooms(val publishing: Room, val receiving: List<Room>)

@Composable
internal fun rememberCallRooms(
    publishing: LiveKitCallParameters,
    receiving: List<LiveKitCallParameters>,
    onError: () -> Unit,
): CallRooms {
    return CallRooms(
        publishing = rememberCallRoom(publishing, isPublishing = true, onError),
        receiving =
            receiving.map { connection ->
                key(connection.url) { rememberCallRoom(connection, isPublishing = false, onError) }
            },
    )
}

/** Keep each SFU room alive while its role and credentials are unchanged. */
@Composable
private fun rememberCallRoom(connection: LiveKitCallParameters, isPublishing: Boolean, onError: () -> Unit): Room {
    val reportError by rememberUpdatedState(onError)
    return key(isPublishing, connection.url, connection.token, connection.userId, connection.encryptionInfo != null) {
        val encryptionInfo by rememberUpdatedState(connection.encryptionInfo)
        var failed by remember { mutableStateOf(false) }
        val options =
            remember(connection.encryptionInfo) {
                if (connection.encryptionInfo == null) {
                    RoomOptions()
                } else {
                    RoomOptions(
                        e2eeOptions =
                            E2EEOptions(
                                keyProviderOptions =
                                    KeyProviderOptions(
                                        ratchetWindowSize = 0,
                                        enableSharedKey = false,
                                        keyRingSize = 256,
                                        keyDerivationAlgorithm = KeyDerivationAlgorithm.HKDF,
                                    )
                            )
                    )
                }
            }
        val liveKit = LiveKitLocal.current
        val room = remember(liveKit) { liveKit.create(options) }
        val provider =
            remember(room) {
                if (connection.encryptionInfo == null) null
                else
                    MatrixRtcKeyProvider(
                        checkNotNull(room.keyProvider) { "keyProvider is required when encryptionInfo not null" }
                    )
            }

        LaunchedEffect(room, connection.url, connection.token) {
            try {
                encryptionInfo?.also { info -> provider?.update(info, connection.userId) }
                room.connect(url = connection.url, token = connection.token)
                awaitCancellation()
            } catch (_: Throwable) {
                currentCoroutineContext().ensureActive()
                failed = true
                reportError()
            } finally {
                withContext(NonCancellable) { room.disconnect() }
            }
        }

        LaunchedEffect(room) {
            if (provider != null) {
                launch {
                    try {
                        snapshotFlow { encryptionInfo }
                            .filterNotNull()
                            .collect { info -> provider.update(info, connection.userId) }
                    } catch (_: Throwable) {
                        currentCoroutineContext().ensureActive()
                        failed = true
                        reportError()
                    }
                }
            }

            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { room.release() }
            }
        }
        room
    }
}
