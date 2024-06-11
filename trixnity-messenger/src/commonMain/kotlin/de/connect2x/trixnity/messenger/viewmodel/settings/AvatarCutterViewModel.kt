package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent


private val log = KotlinLogging.logger { }


interface AvatarCutterViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        file: FileDescriptor,
        roomId: RoomId? = null,
        onClose: () -> Unit,
    ): AvatarCutterViewModel {
        return AvatarCutterViewModelImpl(viewModelContext, onClose = onClose, file = file, roomId = roomId)
    }

    companion object : AvatarCutterViewModelFactory
}

interface AvatarCutterViewModel {
    val upload: StateFlow<Boolean>
    val error: StateFlow<String?>
    val file: FileDescriptor
    fun cancel()
    fun accept()
}

open class AvatarCutterViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val file: FileDescriptor,
    private val onClose: () -> Unit,
    private val roomId: RoomId?
) : MatrixClientViewModelContext by viewModelContext, AvatarCutterViewModel {

    override val upload = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)

    private val backCallback = BackCallback {
        cancel()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun cancel() {
        onClose()
    }

    override fun accept() {
        coroutineScope.launch {
            upload.value = true
            val preparedUpload = matrixClient.media.prepareUploadThumbnail(
                file.content,
                file.mimeType,
            )

            if (preparedUpload == null) {
                log.error { "Failed to prepare upload thumbnail." }
                upload.value = false
                error.value = i18n.profileAvatarError()
                return@launch
            }

            val (cache, _) = preparedUpload

            matrixClient.media.uploadMedia(cache).fold(
                onSuccess = { url ->
                    if (roomId == null) {
                        setUserAvatar(url)
                    } else {
                        setRoomAvatar(url, roomId)
                    }
                },
                onFailure = {
                    log.error(it) { "Cannot upload avatar image." }
                    upload.value = false
                    error.value = i18n.profileAvatarError()
                }
            )
        }
    }

    private suspend fun setUserAvatar(url: String) {
        matrixClient.setAvatarUrl(url).fold(
            onSuccess = {
                upload.value = false
                onClose()
            },
            onFailure = {
                log.error(it) { "Cannot set user avatar." }
                upload.value = false
                error.value = i18n.profileAvatarError()
            }
        )
    }

    private suspend fun setRoomAvatar(url: String, roomId: RoomId) {
        matrixClient.api.room.sendStateEvent(
            roomId,
            eventContent = AvatarEventContent(url = url)
        ).fold(
            onSuccess = {
                upload.value = false
                onClose()
            },
            onFailure = {
                log.error(it) { "Cannot set room avatar." }
                upload.value = false
                error.value = i18n.profileAvatarError()
            }
        )
    }
}
