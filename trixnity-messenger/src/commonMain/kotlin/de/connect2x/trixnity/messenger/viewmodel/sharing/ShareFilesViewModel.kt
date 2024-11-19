package de.connect2x.trixnity.messenger.viewmodel.sharing

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.getImageDimensions
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.audio
import net.folivo.trixnity.client.room.message.file
import net.folivo.trixnity.client.room.message.image
import net.folivo.trixnity.client.room.message.video
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

interface ShareFilesViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        sharedFiles: List<FileDescriptor>,
        onClose: () -> Unit,
    ): ShareFilesViewModel {
        return ShareFilesViewModelImpl(
            viewModelContext,
            sharedFiles,
            onClose,
        )
    }

    companion object : ShareFilesViewModelFactory
}


interface ShareFilesViewModel {
    val selectedRoomId: StateFlow<RoomId?>
    val sharedFiles: List<FileDescriptor>
    val roomList: RoomListViewModel
    val sending: StateFlow<Boolean>
    fun send()
    fun cancel()
}

private val log = KotlinLogging.logger { }

class ShareFilesViewModelImpl(
    private val viewModelContext: ViewModelContext,
    override val sharedFiles: List<FileDescriptor>,
    private val onClose: () -> Unit,
) : ViewModelContext by viewModelContext, ShareFilesViewModel {
    private val _selectedRoomId = MutableStateFlow<RoomId?>(null)
    override val selectedRoomId: StateFlow<RoomId?> = _selectedRoomId

    private val _sending = MutableStateFlow(false)
    override val sending: StateFlow<Boolean> = _sending

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()

    private val roomListViewModelFactory = get<RoomListViewModelFactory>()
    override val roomList: RoomListViewModel = roomListViewModelFactory.create(
        viewModelContext.childContext("roomlist", LifecycleRegistry()),
        selectedRoomId = selectedRoomId,
        onRoomSelected = { _, roomId -> _selectedRoomId.update { if (it == roomId) null else roomId } },
        onStartCreateNewRoom = { },
        onUserSettingsSelected = { },
        onOpenAppInfo = { },
        onSendLogs = { },
        onOpenAccountsOverview = { },
        onAccountSelected = { },
    )

    private val maxMediaSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    override fun send() {
        val selectedAccount =
            messengerSettings.value.base.selectedAccount ?: return log.warn { "selectedAccount is null" }
        val matrixClient = matrixClients.value[selectedAccount] ?: return log.warn { "matrix client is null" }
        val roomId = this.selectedRoomId.value ?: return log.warn { "selected room is null" }
        coroutineScope.launch {
            _sending.value = true
            for (file in sharedFiles) {
                matrixClient.room.sendMessage(roomId) {
                    when {
                        file.mimeType?.match("image/*") == true -> {
                            log.debug { "send an image to ${roomId.full}" }
                            val (width, height) = getImageDimensions(file.content, maxMediaSize)
                            image(
                                body = file.fileName,
                                fileName = file.fileName,
                                image = file.content,
                                type = file.mimeType,
                                size = file.fileSize,
                                width = width,
                                height = height,
                            )
                        }

                        file.mimeType?.match("video/*") ?: false -> {
                            log.debug { "send a video to ${roomId.full}" }
                            video(
                                body = file.fileName,
                                fileName = file.fileName,
                                video = file.content,
                                type = file.mimeType,
                                size = file.fileSize,
                            )
                        }

                        file.mimeType?.match("audio/*") ?: false -> {
                            log.debug { "send an audio to ${roomId.full}" }
                            audio(
                                body = file.fileName,
                                fileName = file.fileName,
                                audio = file.content,
                                type = file.mimeType,
                                size = file.fileSize,
                            )
                        }

                        else -> {
                            log.debug { "send a file to ${roomId.full}" }
                            file(
                                body = file.fileName,
                                file = file.content,
                                type = file.mimeType,
                                fileName = file.fileName,
                                size = file.fileSize
                            )
                        }
                    }
                }
            }
            _sending.value = false
            onClose()
        }
    }

    override fun cancel() {
        coroutineScope.launch {
            onClose()
        }
    }
}
