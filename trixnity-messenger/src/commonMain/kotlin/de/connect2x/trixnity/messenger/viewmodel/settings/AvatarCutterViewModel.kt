package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.checkFileSizeExceedsLimit
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
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
    val upload: StateFlow<Boolean>
    val error: StateFlow<String?>
    val fileDescriptor: FileDescriptor
    fun cancel()
    fun accept()
}

open class AvatarCutterViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val file: FileDescriptor,
    private val onClose: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, AvatarCutterViewModel {

    override val upload = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)
    override val fileDescriptor: FileDescriptor = file

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
            matrixClient.media.prepareUploadThumbnail(
                file.content,
                file.mimeType,
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
        }
    }
}
