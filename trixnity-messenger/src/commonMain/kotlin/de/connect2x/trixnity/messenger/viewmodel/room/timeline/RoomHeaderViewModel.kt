package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomHeaderElement
import de.connect2x.trixnity.messenger.viewmodel.RoomName
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoom
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresence
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.*
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.TypingEventContent
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface RoomHeaderViewModelFactory {
    fun newRoomHeaderViewModel(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        isBackButtonVisible: MutableStateFlow<Boolean>,
        onBack: () -> Unit,
        onVerifyUser: () -> Unit,
        onShowRoomSettings: () -> Unit,
    ): RoomHeaderViewModel {
        return RoomHeaderViewModelImpl(
            viewModelContext,
            selectedRoomId,
            isBackButtonVisible,
            onBack,
            onVerifyUser,
            onShowRoomSettings,
        )
    }
}

interface RoomHeaderViewModel {
    val isBackButtonVisible: StateFlow<Boolean>
    val roomHeaderElement: StateFlow<RoomHeaderElement>
    val usersTyping: StateFlow<String?>
    val userTrustLevel: StateFlow<UserTrustLevel?>
    val canVerifyUser: StateFlow<Boolean>

    fun verifyUser()
    fun showRoomSettings()
    fun goBack()
}

@OptIn(ExperimentalCoroutinesApi::class)
open class RoomHeaderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    override val isBackButtonVisible: MutableStateFlow<Boolean>,
    private val onBack: () -> Unit,
    private val onVerifyUser: () -> Unit,
    private val onShowRoomSettings: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RoomHeaderViewModel {

    override val roomHeaderElement: StateFlow<RoomHeaderElement>
    override val userTrustLevel: StateFlow<UserTrustLevel?>
    override val canVerifyUser: StateFlow<Boolean>

    private val directRoom = get<DirectRoom>()
    private val userPresence = get<UserPresence>()
    private val roomName = get<RoomName>()
    private val initials = get<Initials>()

    override val usersTyping = matrixClient.room.usersTyping.map { map ->
        map[selectedRoomId]?.let { typingInfo(it) }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    init {
        roomHeaderElement =
            combine(
                matrixClient.room.getById(selectedRoomId),
                roomName.getRoomNameElement(selectedRoomId, matrixClient),
                userPresence.presentEventContentFlow(matrixClient, selectedRoomId)
            ) { room, roomNameElement, userPresence ->
                val roomImage = room?.avatarUrl?.let { avatarUrl ->
                    matrixClient.media.getThumbnail(
                        avatarUrl,
                        avatarSize().toLong(),
                        avatarSize().toLong()
                    ).fold(
                        onSuccess = { it },
                        onFailure = {
                            log.error(it) { "Cannot load avatar image for room '${roomNameElement.roomName}'." }
                            null
                        }
                    )
                }?.toByteArray()
                RoomHeaderElement(
                    roomNameElement.roomName,
                    initials.compute(roomNameElement.roomName),
                    roomImage,
                    userPresence?.presence,
                )
            }.stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                RoomHeaderElement(
                    "",
                    initials.compute(selectedRoomId.full),
                    null,
                    Presence.OFFLINE
                )
            )

        userTrustLevel =
            directRoom.getUser(matrixClient, selectedRoomId).flatMapLatest {
                it?.let { userId ->
                    matrixClient.key.getTrustLevel(userId)
                } ?: flowOf(null)
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

        canVerifyUser =
            combine(
                directRoom.getUser(matrixClient, selectedRoomId),
                userTrustLevel
            ) { otherUser, userTrustLevel ->
                val otherUserVerified =
                    userTrustLevel is UserTrustLevel.CrossSigned && userTrustLevel.verified
                otherUser != null && otherUserVerified.not()
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    }

    override fun verifyUser() {
        onVerifyUser()
    }

    override fun showRoomSettings() {
        onShowRoomSettings()
    }

    override fun goBack() {
        onBack()
    }

    private suspend fun typingInfo(eventContent: TypingEventContent): String? {
        val usersTyping = eventContent.users.filterNot { it == matrixClient.userId }
        return when {
            usersTyping.isEmpty() -> null
            usersTyping.size == 1 -> {
                val username = usersTyping[0].let {
                    matrixClient.user.getById(selectedRoomId, it).first()?.name
                        ?: it.full
                }
                val isDirect =
                    matrixClient.room.getById(selectedRoomId).first()?.isDirect ?: false
                if (isDirect)
                    i18n.roomHeaderTypingSingleDirect()
                else
                    i18n.roomHeaderTypingSingle(username)
            }

            usersTyping.size < 4 -> {
                val usernames = usersTyping.map {
                    matrixClient.user.getById(selectedRoomId, it).first()?.name
                        ?: it.full
                }

                i18n.roomHeaderTypingMultiple(
                    i18n.commonAnd(
                        usernames.take(usernames.size - 1).joinToString(),
                        usernames.last()
                    )
                )
            }

            else -> {
                val usernames = usersTyping.map {
                    matrixClient.user.getById(selectedRoomId, it).first()?.name
                        ?: it.full
                }
                i18n.roomHeaderTypingMultipleMore(usernames.take(2).joinToString())
            }
        }
    }
}

class PreviewRoomHeaderViewModel : RoomHeaderViewModel {
    override val isBackButtonVisible: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val roomHeaderElement: MutableStateFlow<RoomHeaderElement> = MutableStateFlow(
        RoomHeaderElement(
            roomName = "Dev Channel",
            roomImageInitials = "DC",
            roomImage = null,
            presence = null
        )
    )
    override val usersTyping: MutableStateFlow<String?> = MutableStateFlow("is typing...")
    override val userTrustLevel: MutableStateFlow<UserTrustLevel?> = MutableStateFlow(null)
    override val canVerifyUser: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun verifyUser() {
    }

    override fun showRoomSettings() {
    }

    override fun goBack() {
    }

}
