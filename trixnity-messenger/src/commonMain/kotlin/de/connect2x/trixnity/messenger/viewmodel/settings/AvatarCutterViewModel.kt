package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.GetFileInfo
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


private val log = KotlinLogging.logger { }


interface AvatarCutterViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        file: FileDescriptor,
        onClose: () -> Unit,
    ): AvatarCutterViewModel {
        return AvatarCutterViewModelImpl(viewModelContext, file, onClose)
    }

    companion object : AvatarCutterViewModelFactory
}

interface AvatarCutterViewModel {
    val image: StateFlow<ByteArray?>
    val upload: StateFlow<Boolean>
    val error: StateFlow<String?>
    fun cancel()
    fun accept()
}

open class AvatarCutterViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    file: FileDescriptor,
    private val onClose: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, AvatarCutterViewModel {
    private val getFileInfo = get<GetFileInfo>()

    private val fileInfo = flow { emit(getFileInfo(file)) }
        .shareIn(coroutineScope, started = SharingStarted.Eagerly, replay = 1)
    override val image = fileInfo.map { it?.content?.toByteArray() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

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
            val fileInfo = fileInfo.first()
            if (fileInfo != null) {
                matrixClient.media.prepareUploadThumbnail(
                    fileInfo.content,
                    fileInfo.mimeType,
                )?.let { (cache, _) ->
                    matrixClient.media.uploadMedia(cache).fold(
                        onSuccess = { url ->
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
                        },
                        onFailure = {
                            log.error(it) { "Cannot upload avatar image." }
                            upload.value = false
                            error.value = i18n.profileAvatarError()
                        }
                    )
                }
            } else {
                log.warn { "fileInfo is null" }
            }
        }
    }

}
