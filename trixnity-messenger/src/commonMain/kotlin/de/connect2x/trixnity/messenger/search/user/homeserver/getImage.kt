package de.connect2x.trixnity.messenger.search.user.homeserver

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

fun getImage(
    avatarUrl: String?,
    matrixClient: MatrixClient,
    maxMediaSizeInMemory: Long,
    coroutineScope: CoroutineScope,
): StateFlow<ByteArray?> {
    return avatarUrl?.let { avatarUrl ->
        flow {
                emit(
                    matrixClient.media
                        .getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong(), maxMediaSizeInMemory)
                        .fold(
                            onSuccess = { it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory) },
                            onFailure = { null },
                        )
                )
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    } ?: MutableStateFlow(null)
}
