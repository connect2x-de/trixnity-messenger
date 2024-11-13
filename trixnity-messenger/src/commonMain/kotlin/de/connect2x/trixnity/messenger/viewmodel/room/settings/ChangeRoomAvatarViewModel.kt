package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.MaxByteFlowSizeException
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface ChangeRoomAvatarViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onOpenAvatarCutter: (UserId, RoomId, FileDescriptor) -> Unit,
    ): ChangeRoomAvatarViewModel {
        return ChangeAvatarViewModelImpl(
            viewModelContext,
            selectedRoomId,
            onOpenAvatarCutter,
        )
    }

    companion object : ChangeRoomAvatarViewModelFactory
}

interface ChangeRoomAvatarViewModel {
    val canChangeRoomAvatar: StateFlow<Boolean>
    val avatar: StateFlow<ByteArray?>
    val initials: StateFlow<String>
    val openImageSelector: MutableStateFlow<Boolean>
    fun openAvatarCutter(file: FileDescriptor)
}

class ChangeAvatarViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onOpenAvatarCutter: (UserId, RoomId, FileDescriptor) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ChangeRoomAvatarViewModel {

    private val initialsComputation = get<Initials>()
    private val roomNameComputation = get<RoomName>()

    override val canChangeRoomAvatar: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<AvatarEventContent>(selectedRoomId)
            .stateIn(coroutineScope, Eagerly, false) // Needs to be Eagerly for some use cases.

    private val maxPreviewSize = get<MatrixMessengerConfiguration>().filePreviewMaxSize
    override val avatar = matrixClient.room.getById(selectedRoomId).map { room ->
        room?.avatarUrl?.let { avatar ->
            matrixClient.media.getThumbnail(
                avatar,
                avatarSize().toLong(),
                avatarSize().toLong(),
            ).fold(
                onSuccess = {
                    try {
                        it.limitSize(maxPreviewSize).toByteArray()
                    }
                    catch (_ : MaxByteFlowSizeException) {
                        log.error{"Room avatar for room $selectedRoomId exceeds max preview size, so it is not displayed"}
                        null
                    }
                },
                onFailure = {
                    log.error(it) { "Cannot load user avatar." }
                    null
                }
            )
        }
    }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val initials =
        roomNameComputation.getRoomName(selectedRoomId, matrixClient)
            .map { initialsComputation.compute(it) }
            .stateIn(coroutineScope, WhileSubscribed(), "")

    override val openImageSelector: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun openAvatarCutter(file: FileDescriptor) {
        if (canChangeRoomAvatar.value) {
            onOpenAvatarCutter(userId, selectedRoomId, file)
        } else {
            log.warn { "Don't have enough rights to change avatar" }
        }
    }
}

class PreviewChangeAvatarViewModel : ChangeRoomAvatarViewModel {
    override val canChangeRoomAvatar: StateFlow<Boolean> = MutableStateFlow(false)
    override val avatar: StateFlow<ByteArray?> = MutableStateFlow(null)
    override val initials: StateFlow<String> = MutableStateFlow("T")
    override val openImageSelector: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun openAvatarCutter(file: FileDescriptor) {}
}
