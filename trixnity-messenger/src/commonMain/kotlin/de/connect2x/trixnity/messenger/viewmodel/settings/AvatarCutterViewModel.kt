package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.GetFileInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.utils.toByteArrayFlow
import okio.buffer
import org.koin.core.component.get


private val log = KotlinLogging.logger { }


interface AvatarCutterViewModelFactory {
    fun newAvatarCutterViewModel(
        viewModelContext: MatrixClientViewModelContext,
        file: String,
        onClose: () -> Unit,
    ): AvatarCutterViewModel {
        return AvatarCutterViewModelImpl(viewModelContext, file, onClose)
    }
}

interface AvatarCutterViewModel {
    val image: ByteArray
    val upload: MutableStateFlow<Boolean>
    val error: MutableStateFlow<String?>
    fun cancel()
    fun accept()
}

open class AvatarCutterViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    file: String,
    private val onClose: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, AvatarCutterViewModel {
    private val getFileInfo = get<GetFileInfo>()

    private val fileInfo = getFileInfo(file)
    override val image: ByteArray = fileInfo.source.buffer().readByteArray()

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
            matrixClient.media.prepareUploadThumbnail(
                image.toByteArrayFlow(),
                ContentType.Image.Any
            ) // TODO ByteArrayFlow
                ?.let { (cache, _) ->
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
