package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

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
    val file: FileDescriptor
    val upload: StateFlow<Boolean>
    val error: StateFlow<String?>
    val avatarCutterHeading: String
    val maxAvatarSize: Long
    val avatarImage: StateFlow<ByteArray?>
    fun cancel()
    fun accept()
    fun setAvatarImage(data: ByteArray?)
}

open class AvatarCutterViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val file: FileDescriptor,
    private val onClose: () -> Unit,
    private val roomId: RoomId?
) : MatrixClientViewModelContext by viewModelContext, AvatarCutterViewModel {

    private val i18n = get<I18n>()
    private val messengerConfiguration = get<MatrixMessengerConfiguration>()

    override val upload = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)

    override val avatarCutterHeading =
        if (roomId == null) i18n.yourNewProfileAvatar()
        else i18n.yourNewRoomAvatar()

    private val backCallback = BackCallback {
        cancel()
    }

    override val maxAvatarSize: Long = messengerConfiguration.avatarMaxSize

    private val _avatarImage: MutableStateFlow<ByteArray?> = MutableStateFlow(null)

    init {
        backHandler.register(backCallback)
        coroutineScope.launch {
            val fileSize = file.fileSize
            if (fileSize == null || fileSize <= maxAvatarSize) {
                _avatarImage.value = file.content.limitedByteArrayOrNull(maxAvatarSize) {
                    log.warn { "Uploaded avatar file exceeds avatar size limits, so it's not shown" }
                    error.value = i18n.avatarSizeMaxSizeError(maxAvatarSize)
                }
            } else {
                log.warn { "Uploaded avatar file exceeds avatar size limits, so it's not shown" }
                error.value = i18n.avatarSizeMaxSizeError(maxAvatarSize)
            }
        }
    }

    override val avatarImage: StateFlow<ByteArray?> = _avatarImage.asStateFlow()

    override fun setAvatarImage(data: ByteArray?) {
        _avatarImage.value = data
    }

    override fun cancel() {
        onClose()
    }

    override fun accept() {
        coroutineScope.launch {
            upload.value = true
            val cacheUri = matrixClient.media.prepareUploadMedia(
                file.content,
                file.mimeType,
            )
            matrixClient.media.uploadMedia(cacheUri).fold(
                onSuccess = { url ->
                    log.debug { "Successfully uploaded avatar image" }
                    if (roomId == null) setUserAvatar(url)
                    else setRoomAvatar(url, roomId)
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
                log.debug { "Successfully set user avatar" }
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
            eventContent = AvatarEventContent(url = url),
        ).fold(
            onSuccess = {
                upload.value = false
                log.debug { "Successfully set room avatar" }
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

