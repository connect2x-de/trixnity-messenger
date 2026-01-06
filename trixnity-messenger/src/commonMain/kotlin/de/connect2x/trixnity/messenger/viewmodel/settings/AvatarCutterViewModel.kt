package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.ProcessImageUpload
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.http.ContentType.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.core.component.get
import net.folivo.trixnity.utils.toByteArray


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
    val mimeType: StateFlow<ContentType?>

    /**
     * Original file selected from camera/filepicker
     */
    val file: FileDescriptor

    /**
     * Current file content, can be set via [setImageData]. Will be uploaded by [accept]
     */
    val imageData: StateFlow<ByteArrayFlow?>

    /**
     * File content with automatic transformations applied for display in UI
     */
    val avatarImage: StateFlow<ByteArray?>

    /**
     * Upload in progress
     */
    val upload: StateFlow<Boolean>
    val error: StateFlow<String?>

    val avatarCutterHeading: String
    val maxAvatarSize: Long

    /**
     * Uploads [imageData] and sets it as new avatar
     */
    fun accept()
    fun cancel()

    @Deprecated("Use setImageData instead", replaceWith = ReplaceWith("setImageData"))
    fun setAvatarImage(data: ByteArray?, mimeType: ContentType = Image.PNG) = setImageData(data?.toByteArrayFlow(), mimeType)
    fun setImageData(data: ByteArrayFlow?, mimeType: ContentType)
}

open class AvatarCutterViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val file: FileDescriptor,
    private val onClose: () -> Unit,
    private val roomId: RoomId?
) : MatrixClientViewModelContext by viewModelContext, AvatarCutterViewModel {

    private val i18n = get<I18n>()

    override val upload = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)

    override val avatarCutterHeading =
        if (roomId == null) i18n.yourNewProfileAvatar()
        else i18n.yourNewRoomAvatar()

    private val backCallback = BackCallback {
        cancel()
    }

    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    override val maxAvatarSize: Long = maxMediaSizeInMemory

    override val mimeType = MutableStateFlow<ContentType?>(file.mimeType)
    override val imageData = MutableStateFlow<ByteArrayFlow?>(file.content)
    override val avatarImage: StateFlow<ByteArray?> = imageData.map {
        it?.toByteArray(maxSize = maxMediaSizeInMemory)?.let {
            error.value = null
            get<ProcessImageUpload>().invoke(
                it,
                file.mimeType ?: Image.PNG, // TODO: check if defaulting to PNG isn't causing any issues
            )
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, null)


    init {
        registerBackCallback(backCallback)
    }

    override fun setImageData(data: ByteArrayFlow?, mimeType: ContentType) {
        this.imageData.value = data
        this.mimeType.value = mimeType
    }

    override fun cancel() {
        onClose()
    }

    override fun accept() {
        coroutineScope.launch {
            imageData.value?.let { fileContent ->
                upload.value = true
                val cacheUri = matrixClient.media.prepareUploadMedia(fileContent, mimeType.value)
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

