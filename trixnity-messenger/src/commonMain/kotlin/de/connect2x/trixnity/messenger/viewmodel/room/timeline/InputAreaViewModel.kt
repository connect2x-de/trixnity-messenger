package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.canSendMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.replace
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event.MessageEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.*
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

interface InputAreaViewModelFactory {
    fun newInputAreaViewModel(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onMessageEditFinished: (EventId) -> Unit,
        onMessageReplyToFinished: (MessageEvent<*>) -> Unit,
        onShowAttachmentSendView: (file: String) -> Unit,
    ): InputAreaViewModel {
        return InputAreaViewModelImpl(
            viewModelContext,
            selectedRoomId,
            onMessageEditFinished,
            onMessageReplyToFinished,
            onShowAttachmentSendView,
        )
    }
}

interface InputAreaViewModel {
    val isAllowedToSendMessages: StateFlow<Boolean>
    val message: MutableStateFlow<String>
    val isSendEnabled: StateFlow<Boolean>
    val showAttachmentSelectDialog: StateFlow<Boolean>
    val hasShownAttachmentSelectDialog: SharedFlow<Boolean>
    val isEdit: StateFlow<Boolean>
    val replyToViewModel: StateFlow<ReplyToViewModel?>
    val isReplyTo: StateFlow<Boolean>
    val listOfPossibleUsers: StateFlow<List<String>>

    /**
     * The UI should focus the input area whenever this value changes to a non-null value.
     */
    val shouldFocus: StateFlow<String?>

    fun addNewlineToMessage()
    fun addToMessage(additional: String)
    fun sendMessage()
    fun selectAttachment()
    fun closeAttachmentDialog()
    fun onAttachmentFileSelect(file: String)
    fun editMessage(eventId: EventId)
    fun cancelEdit()
    fun replyToMessage(event: MessageEvent<*>)
    fun cancelReplyTo()
}

@OptIn(FlowPreview::class)
open class InputAreaViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onMessageEditFinished: (EventId) -> Unit,
    private val onMessageReplyFinished: (MessageEvent<*>) -> Unit,
    private val onShowAttachmentSendView: (file: String) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, InputAreaViewModel {

    override val isAllowedToSendMessages: StateFlow<Boolean> =
        canSendMessages(matrixClient, selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val message = MutableStateFlow("")
    override val isSendEnabled: StateFlow<Boolean> =
        message.map { it.isNotBlank() }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val showAttachmentSelectDialog = MutableStateFlow(false)

    private val isTyping = MutableStateFlow(false)
    private val isStillTyping = MutableStateFlow(uuid4())

    private val _shouldFocus: MutableStateFlow<String?> = MutableStateFlow(null)
    override val shouldFocus: StateFlow<String?> = _shouldFocus.asStateFlow()
    private val editMode: MutableStateFlow<EventId?> = MutableStateFlow(null)
    override val isEdit: StateFlow<Boolean> =
        editMode.map { it != null }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val replyToViewModel: MutableStateFlow<ReplyToViewModel?> = MutableStateFlow(null)
    override val isReplyTo: StateFlow<Boolean> =
        replyToViewModel.map { it != null }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val listOfPossibleUsers: StateFlow<List<String>> = message.map { message ->
        val nameRegex = "^.*\\s@(\\S*$)|^@(\\S*$)".toRegex()
        message.split("\\R".toRegex()).lastOrNull()?.let { lastLine ->
            val matchResult = nameRegex.find(lastLine)
            val groups = matchResult?.groupValues?.filterNot { it == lastLine }
            log.trace { "$groups" }
            if (groups?.size == 1 || groups?.size == 2) {
                val namePrefix =
                    if (groups.size ==1) groups[0]
                    else groups.filter { it.isNotEmpty() }.getOrNull(0) // multiline
                        ?: "" // @ with no prefix yet
                listOfUsers(namePrefix)
            } else {
                emptyList()
            }
        } ?: emptyList()
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        coroutineScope.launch {
            isStillTyping.debounce(3.seconds).collect {
                userIsNotTyping()
            }
        }
        coroutineScope.launch {
            message.drop(1).collect {
                if (it == "") {
                    userIsNotTyping()
                } else {
                    typing()
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    override val hasShownAttachmentSelectDialog =
        showAttachmentSelectDialog.debounce(200).shareIn(coroutineScope, SharingStarted.Eagerly, replay = 1)

    override fun addNewlineToMessage() {
        message.value += "\n"
        _shouldFocus.value = uuid4().toString()
    }

    override fun addToMessage(additional: String) {
        message.value += additional
        _shouldFocus.value = uuid4().toString()
    }

    override fun sendMessage() {
        log.debug { "try to send message (enabled: ${isSendEnabled.replayCache.lastOrNull()})" }
        if (isSendEnabled.replayCache.lastOrNull() == true) {
            val text = message.value
            coroutineScope.launch {
                val editedEvent = editMode.value
                if (editedEvent != null) {
                    log.debug { "send message (edit)" }
                    matrixClient.room.sendMessage(selectedRoomId) {
                        replace(editedEvent) // FIXME replace a reply event will destroy the replied part
                        text(text)
                    }
                    editMode.value = null
                    _shouldFocus.value = null
                    onMessageEditFinished(editedEvent)
                } else {
                    val viewModel = replyToViewModel.value
                    if (viewModel != null) {
                        log.debug { "send message (reply)" }
                        val event = viewModel.event
                        matrixClient.room.sendMessage(selectedRoomId) {
                            reply(event)
                            text(text)
                        }
                        replyToViewModel.value = null
                        _shouldFocus.value = null
                        onMessageReplyFinished(event)
                    } else {
                        log.debug { "send message" }
                        matrixClient.room.sendMessage(selectedRoomId) {
                            text(text)
                        }
                    }
                }
            }
            message.value = ""
        }
    }

    override fun selectAttachment() {
        showAttachmentSelectDialog.value = true
    }

    override fun closeAttachmentDialog() {
        showAttachmentSelectDialog.value = false
    }

    override fun onAttachmentFileSelect(file: String) {
        log.debug { "selected $file as attachment" }
        onShowAttachmentSendView(file)
    }

    override fun editMessage(eventId: EventId) {
        log.debug { "edit message $eventId" }
        coroutineScope.launch {
            matrixClient.room.getTimelineEvent(selectedRoomId, eventId).firstOrNull()?.content?.getOrNull()
                ?.let { roomEventContent ->
                    when (roomEventContent) {
                        is TextMessageEventContent -> {
                            editMode.value = eventId
                            message.value = roomEventContent.bodyWithoutFallback
                            _shouldFocus.value = eventId.full
                        }

                        is NoticeMessageEventContent -> {
                            editMode.value = eventId
                            message.value = roomEventContent.bodyWithoutFallback
                            _shouldFocus.value = eventId.full
                        }

                        is EmoteMessageEventContent -> {
                            editMode.value = eventId
                            message.value = roomEventContent.bodyWithoutFallback
                            _shouldFocus.value = eventId.full
                        }

                        else -> log.warn { "cannot edit anything besides TextMessageEventContent" }
                    }
                } ?: log.warn { "cannot get timeline event $eventId" }
        }
    }

    override fun cancelEdit() {
        val editedValue = editMode.value
        if (editedValue != null) {
            editMode.value = null
            message.value = ""
            _shouldFocus.value = null
            onMessageEditFinished(editedValue)
        }
    }

    override fun replyToMessage(event: MessageEvent<*>) {
        log.debug { "reply to message ${event.id}" }
        replyToViewModel.value = get<ReplyToViewModelFactory>()
            .newReplyToViewModel(this@InputAreaViewModelImpl, selectedRoomId, event, ::cancelReplyTo)
        _shouldFocus.value = event.id.full
    }

    override fun cancelReplyTo() {
        val repliedToEvent = replyToViewModel.value?.event
        replyToViewModel.value = null
        _shouldFocus.value = null
        repliedToEvent?.let { onMessageReplyFinished(it) }
    }

    private suspend fun listOfUsers(startsWith: String): List<String> {
        return matrixClient.user.getAll(selectedRoomId).filterNotNull()
            .map { it.values }
            .map { userFlowList ->
                userFlowList
                    .filter { roomUserFlow ->
                        val roomUser = roomUserFlow.first()
                        roomUser?.userId != matrixClient.userId &&
                                roomUser?.name?.startsWith(startsWith, ignoreCase = true) ?: false
                    }
                    .take(10)
                    .map { it.first()?.name ?: "" }
            }.first()
    }

    private suspend fun typing() {
        if (isTyping.value.not()) {
            isTyping.value = true
            try {
                matrixClient.api.rooms.setTyping(selectedRoomId, matrixClient.userId, true, 30_000)
            } catch (exc: Exception) {
                // ignore
            }
        }
        isStillTyping.value = uuid4()
    }

    private suspend fun userIsNotTyping() {
        try {
            if (isTyping.value.not()) {
                return
            }
            isTyping.value = false
            matrixClient.api.rooms.setTyping(selectedRoomId, matrixClient.userId, typing = false)
        } catch (exc: Exception) {
            // ignore
        }
    }
}

class PreviewInputViewModel : InputAreaViewModel {
    override val isAllowedToSendMessages: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val message: MutableStateFlow<String> = MutableStateFlow("")
    override val isSendEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showAttachmentSelectDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasShownAttachmentSelectDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val shouldFocus: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isEdit: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReplyTo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val replyToViewModel: MutableStateFlow<ReplyToViewModel?> = MutableStateFlow(null)
    override val listOfPossibleUsers: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    override fun addNewlineToMessage() {
    }

    override fun addToMessage(additional: String) {
    }

    override fun sendMessage() {
    }

    override fun selectAttachment() {
    }

    override fun closeAttachmentDialog() {
    }

    override fun onAttachmentFileSelect(file: String) {
    }

    override fun editMessage(eventId: EventId) {
    }

    override fun cancelEdit() {
    }

    override fun replyToMessage(event: MessageEvent<*>) {
    }

    override fun cancelReplyTo() {
    }
}