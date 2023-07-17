package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.canSendMessages
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.afterNewline
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.replace
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event.MessageEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.*
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

data class Username(
    val matrixId: String,
    val name: String,
    val initials: String,
    val avatar: Flow<ByteArray?>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Username

        if (matrixId != other.matrixId) return false
        if (name != other.name) return false
        return initials == other.initials
    }

    override fun hashCode(): Int {
        var result = matrixId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + initials.hashCode()
        return result
    }
}

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
    val listOfMentions: StateFlow<List<Username>>
    val listOfMentionsLoading: StateFlow<Boolean?>

    /**
     * The UI should focus the input area whenever this value changes to a non-null value.
     */
    val shouldFocus: StateFlow<String?>

    /**
     * The UI should set this value, so that mentions, etc. can be computed everywhere. When left to `null`, only the
     * end of the last input line is considered for mentions, @see [listOfMentions].
     */
    val currentCursorPosition: MutableStateFlow<Int?>

    fun addNewlineToMessage()
    fun addToMessage(additional: String)
    fun selectMention(username: Username)
    fun sendMessage()
    fun selectAttachment()
    fun closeAttachmentDialog()
    fun onAttachmentFileSelect(file: String)
    fun editMessage(eventId: EventId)
    fun cancelEdit()
    fun replyToMessage(event: MessageEvent<*>)
    fun cancelReplyTo()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
open class InputAreaViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onMessageEditFinished: (EventId) -> Unit,
    private val onMessageReplyFinished: (MessageEvent<*>) -> Unit,
    private val onShowAttachmentSendView: (file: String) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, InputAreaViewModel {

    private val initials = get<Initials>()

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
    override val currentCursorPosition: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val _listOfMentionsLoading = MutableStateFlow<Boolean?>(null)
    override val listOfMentionsLoading: StateFlow<Boolean?> = _listOfMentionsLoading.asStateFlow()
    override val listOfMentions: StateFlow<List<Username>> = combine(
        message, currentCursorPosition
    ) { message, currentCursorPosition ->
        val nameRegex = "^.*\\s@(\\S*$)|^@(\\S*$)".toRegex()
        if (currentCursorPosition != null) {
            getMentions(
                nameRegex,
                message.substring(0, currentCursorPosition.coerceAtMost(message.length)).afterNewline()
            )
        } else {
            message.lines().lastOrNull()?.let { lastLine ->
                getMentions(nameRegex, lastLine)
            } ?: run {
                _listOfMentionsLoading.value = null
                emptyList()
            }
        }
    }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    private suspend fun getMentions(
        nameRegex: Regex,
        line: String
    ): List<Username> {
        val matchResult = nameRegex.find(line)
        val groups = matchResult?.groupValues?.filterNot { it == line }
        return if (groups?.size == 1 || groups?.size == 2) {
            val namePrefix =
                if (groups.size == 1) groups[0]
                else groups.filter { it.isNotEmpty() }.getOrNull(0) // multiline
                    ?: "" // @ with no prefix yet
            _listOfMentionsLoading.value = true
            val listOfUsers = listOfUsers(namePrefix)
            _listOfMentionsLoading.value = false
            listOfUsers
        } else {
            _listOfMentionsLoading.value = null
            emptyList()
        }
    }

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

    override fun selectMention(username: Username) {
        val currentCursorPosition = currentCursorPosition.value
        val lines = message.value.lines()
        if (currentCursorPosition != null) {
            val lineLengthAccumulated = lines
                .runningFold(0) { acc, line -> acc + line.length }
            val lineInWhichCursorIs = lineLengthAccumulated
                .indexOfFirst { it >= currentCursorPosition } - 1 // first index is the initial value 0
            val cursorInLine = currentCursorPosition - lineLengthAccumulated[lineInWhichCursorIs]
            val line = lines[lineInWhichCursorIs]
            val replaceLine = line.substring(0, cursorInLine)
            val beforeLastAt = replaceLine.substringBeforeLast("@")
            val afterLastAt = "@" + replaceLine.substringAfterLast("@")
            val matchResult = "^.*@(\\S*)(.*)$".toRegex().find(afterLastAt)
            val groups = matchResult?.groupValues?.filterNot { it == line }
            if (groups?.size == 2 || groups?.size == 3) {
                val rest = line.substring((cursorInLine + 1).coerceAtMost(line.length))
                val replace =
                    listOf(
                        (if (beforeLastAt != afterLastAt) beforeLastAt else "") +
                        afterLastAt.replace(
                            "@(\\S*)".toRegex(), "@${username.name} "
                        ) + rest
                    )
                message.value =
                    (lines.take(lineInWhichCursorIs) + replace + lines.drop(lines.size - lineInWhichCursorIs + 1))
                        .joinToString("\n") { it }
                _shouldFocus.value = uuid4().toString()
            }
        } else {
            lines.lastOrNull()?.let { lastLine ->
                val matchResult = "^.*\\s@(\\S*$)|^@(\\S*$)".toRegex().find(lastLine)
                val groups = matchResult?.groupValues?.filterNot { it == lastLine }
                if (groups?.size == 1 || groups?.size == 2) {
                    message.value =
                        lines.drop(1).map { "$it\n" }.joinToString { "" } + lastLine.replaceAfterLast(
                            "@",
                            "${username.name} "
                        )
                    _shouldFocus.value = uuid4().toString()
                }
                Unit
            }
        }
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

    private suspend fun listOfUsers(startsWith: String): List<Username> {
        val allUsers = matrixClient.user.getAll(selectedRoomId).first() // wait for all users to load
        return allUsers?.filter { roomUserFlow ->
            val roomUser = roomUserFlow.value.first()
            val userId = roomUser?.userId
            userId != matrixClient.userId && (
                    roomUser?.name?.startsWith(startsWith, ignoreCase = true) ?: false ||
                            userId?.localpart?.startsWith(startsWith, ignoreCase = true) ?: false ||
                            userId?.domain?.startsWith(startsWith, ignoreCase = true) ?: false
                    )
        }
            ?.map { roomUserFlow ->
                roomUserFlow.key.full to roomUserFlow.value
            }
            ?.take(10)
            ?.map { pair ->
                val avatar = pair.second.map {
                    it?.avatarUrl?.let { url ->
                        matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                            onSuccess = { it.toByteArray() },
                            onFailure = { null }
                        )
                    }
                }
                val name = pair.second.first()?.name ?: ""
                Username(pair.first, name, initials.compute(name), avatar)
            }
            ?: emptyList()
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
    override val listOfMentions: MutableStateFlow<List<Username>> = MutableStateFlow(emptyList())
    override val listOfMentionsLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val currentCursorPosition: MutableStateFlow<Int?> = MutableStateFlow(null)

    override fun addNewlineToMessage() {
    }

    override fun addToMessage(additional: String) {
    }

    override fun selectMention(username: Username) {
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