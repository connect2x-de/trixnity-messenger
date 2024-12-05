package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RepliedTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RepliedTimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.afterNewline
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.room.message.mentions
import net.folivo.trixnity.client.room.message.replace
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

data class Username(
    val userId: UserId,
    val name: String,
    val initials: String,
    val avatar: Flow<ByteArray?>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Username

        if (userId != other.userId) return false
        if (name != other.name) return false
        return initials == other.initials
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + initials.hashCode()
        return result
    }
}

interface InputAreaViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onMessageReplaceFinished: (RoomId, EventId) -> Unit,
        onMessageReplyFinished: (RoomId, EventId) -> Unit,
        onShowAttachmentSendView: (file: FileDescriptor) -> Unit,
        onOpenMention: OpenMentionCallback,
    ): InputAreaViewModel {
        return InputAreaViewModelImpl(
            viewModelContext,
            selectedRoomId,
            onMessageReplaceFinished,
            onMessageReplyFinished,
            onShowAttachmentSendView,
            onOpenMention,
        )
    }

    companion object : InputAreaViewModelFactory
}

interface InputAreaViewModel {
    val isAllowedToSendMessages: StateFlow<Boolean>
    val message: MutableStateFlow<String>
    val isSendEnabled: StateFlow<Boolean>
    val showAttachmentSelectDialog: StateFlow<Boolean>
    val hasShownAttachmentSelectDialog: SharedFlow<Boolean>
    val isEdit: StateFlow<Boolean>
    val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?>
    val isReplyTo: StateFlow<Boolean>
    val listOfMentions: StateFlow<List<Username>>
    val listOfMentionsLoading: StateFlow<Boolean?>
    val useMarkdown: StateFlow<Boolean>

    /**
     * The UI should focus the input area whenever this emits a value.
     */
    val shouldFocus: Flow<Unit>

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
    fun onAttachmentFileSelect(file: FileDescriptor)
    fun editMessage(roomId: RoomId, eventId: EventId)
    fun cancelEdit()
    fun replyToMessage(roomId: RoomId, eventId: EventId)
    fun cancelReplyTo()
}

@OptIn(FlowPreview::class)
open class InputAreaViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onMessageReplaceFinished: (RoomId, EventId) -> Unit,
    private val onMessageReplyFinished: (RoomId, EventId) -> Unit,
    private val onShowAttachmentSendView: (file: FileDescriptor) -> Unit,
    private val onOpenMention: OpenMentionCallback,
) : MatrixClientViewModelContext by viewModelContext, InputAreaViewModel {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val initials = get<Initials>()

    override val isAllowedToSendMessages: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<RoomMessageEventContent>(roomId)
            .stateIn(coroutineScope, WhileSubscribed(), false)
    override val message = MutableStateFlow("")
    override val isSendEnabled: StateFlow<Boolean> =
        message.map { it.isNotBlank() }.stateIn(coroutineScope, WhileSubscribed(), false)
    override val showAttachmentSelectDialog = MutableStateFlow(false)

    private val isTyping = MutableStateFlow(false)
    private val isStillTyping = MutableStateFlow(uuid4())

    private val _shouldFocus: MutableSharedFlow<Unit> = MutableSharedFlow()
    override val shouldFocus: Flow<Unit> = _shouldFocus.asSharedFlow()
    private val currentReplace: MutableStateFlow<Pair<RoomId, EventId>?> = MutableStateFlow(null)
    override val isEdit: StateFlow<Boolean> =
        currentReplace.map { it != null }.stateIn(coroutineScope, WhileSubscribed(), false)
    private val currentReply = MutableStateFlow<Pair<RoomId, EventId>?>(null)
    override val isReplyTo: StateFlow<Boolean> =
        currentReply.map { it != null }.stateIn(coroutineScope, WhileSubscribed(), false)
    override val currentCursorPosition: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val _listOfMentionsLoading = MutableStateFlow<Boolean?>(null)
    override val listOfMentionsLoading: StateFlow<Boolean?> = _listOfMentionsLoading.asStateFlow()

    override val useMarkdown = MutableStateFlow(true)

    private val mentionRegex = "@(\\S*)".toRegex()
    private val mentionInLineRegex = "^.*\\s@(\\S*$)|^@(\\S*$)".toRegex()

    override val listOfMentions: StateFlow<List<Username>> = combine(
        message, currentCursorPosition
    ) { message, currentCursorPosition ->
        if (currentCursorPosition != null) {
            getMentions(message.substring(0, currentCursorPosition.coerceAtMost(message.length)).afterNewline())
        } else {
            message.lines().lastOrNull()?.let { lastLine ->
                getMentions(lastLine)
            } ?: run {
                _listOfMentionsLoading.value = null
                emptyList()
            }
        }
    }
        .stateIn(coroutineScope, WhileSubscribed(), emptyList())

    init {
        coroutineScope.launch {
            isStillTyping.debounce(3.seconds).collect {
                userIsNotTyping()
            }
        }
        coroutineScope.launch {
            message.collect {
                if (it == "") {
                    userIsNotTyping()
                } else {
                    typing()
                }
            }
        }
    }

    override val hasShownAttachmentSelectDialog =
        showAttachmentSelectDialog.debounce(200).shareIn(coroutineScope, Eagerly, replay = 1)

    override fun addNewlineToMessage() {
        message.value += "\n"
        _shouldFocus.tryEmit(Unit)
    }

    override fun addToMessage(additional: String) {
        message.value += additional
        _shouldFocus.tryEmit(Unit)
    }

    override fun selectMention(username: Username) {
        val currentCursorPosition = currentCursorPosition.value
        if (currentCursorPosition != null) {
            val lines = message.value.lines()
            val lineLengthAccumulated = lines
                .runningFold(0) { acc, line -> acc + line.length }
            val lineInWhichCursorIs = lineLengthAccumulated
                .indexOfFirst { it >= currentCursorPosition } - 1 // first index is the initial value 0
            val cursorInLine = currentCursorPosition - lineLengthAccumulated[lineInWhichCursorIs]
            val line = lines[lineInWhichCursorIs]
            val replaceLine = line.substring(0, cursorInLine)
            // search from cursor position to beginning of the word -> if not starting with an `@` ignore
            if (replaceLine.takeLastWhile { it != ' ' }.contains("@")) {
                val beforeReplacement = replaceLine.substringBeforeLast("@")
                val shouldBeReplaced = "@" + replaceLine.substringAfterLast("@")
                val restOfTheLine = line.substring((cursorInLine + 1).coerceAtMost(line.length))
                val replacedLine =
                    listOf(
                        beforeReplacement +
                                shouldBeReplaced.replace(mentionRegex, "${username.userId.full} ") +
                                restOfTheLine
                    )
                message.value =
                    (lines.take(lineInWhichCursorIs) + replacedLine + lines.drop(lines.size - lineInWhichCursorIs + 1))
                        .joinToString("\n") { it }
            }
        } else {
            val lastMention = message.value.substringAfterLast("@")
            val matchResult = mentionRegex.matches("@$lastMention")

            if (matchResult) {
                message.value =
                    message.value.replaceAfterLast(
                        "@",
                        "${username.userId.full} "
                    )
            }
        }
        _shouldFocus.tryEmit(Unit)
    }


    override fun sendMessage() {
        log.debug { "try to send message (enabled: ${isSendEnabled.replayCache.lastOrNull()})" }
        if (isSendEnabled.replayCache.lastOrNull() == true) {
            val text = message.value
            coroutineScope.launch {
                val mentions = MatrixRegex.findMentions(text)
                val mentionLinks = mentions
                    .mapValues { (_, mention) ->
                        // TODO should use matrix: uri instead!
                        val matrixUri: String
                        val anchorContent: String
                        when (mention) {
                            is Mention.Event -> {
                                val roomId = mention.roomId ?: roomId
                                matrixUri = "https://matrix.to/#/${roomId.full}/${mention.eventId.full}"
                                anchorContent = mention.label ?: matrixUri
                            }

                            is Mention.Room -> {
                                val alias =
                                    matrixClient.room.getState<CanonicalAliasEventContent>(mention.roomId).first()
                                        ?.content?.run { alias ?: aliases?.firstOrNull() }
                                matrixUri =
                                    if (alias != null) "https://matrix.to/#/${alias.full}"
                                    else "https://matrix.to/#/${roomId.full}"
                                anchorContent = mention.label ?: alias?.full ?: mention.roomId.full
                            }

                            is Mention.RoomAlias -> {
                                matrixUri = "https://matrix.to/#/${mention.roomAliasId.full}"
                                anchorContent = mention.label ?: mention.roomAliasId.full
                            }

                            is Mention.User -> {
                                val userName = matrixClient.user.getById(roomId, mention.userId).first()?.name
                                matrixUri = "https://matrix.to/#/${mention.userId.full}"
                                anchorContent = mention.label ?: userName ?: mention.userId.full
                            }
                        }
                        """<a href="$matrixUri">$anchorContent</a>"""
                    }
                val mentionedUsers = mentions.values.filterIsInstance<Mention.User>().map { it.userId }.toSet()
                val formattedBody = mentionLinks.entries.fold(text) { currentText, (range, newValue) ->
                    currentText.replaceRange(range, newValue)
                }.let {
                    if (useMarkdown.value) {
                        val flavour = CommonMarkFlavourDescriptor()
                        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(it)
                        val html = HtmlGenerator(it, parsedTree, flavour).generateHtml()

                        html.removePrefix("<body>").removeSuffix("</body>")
                    } else it
                }

                val replacedEvent = currentReplace.value
                val repliedEvent = currentReply.value
                log.debug { "send message" }
                matrixClient.room.sendMessage(roomId) {
                    when {
                        replacedEvent != null -> replace(replacedEvent.second)
                        repliedEvent != null -> {
                            val event =
                                matrixClient.room.getTimelineEvent(repliedEvent.first, repliedEvent.second)
                                    .filterNotNull()
                                    .first()
                            reply(event)
                        }
                    }
                    mentions(mentionedUsers)
                    text(body = text, format = "org.matrix.custom.html", formattedBody = formattedBody)
                }
                currentReplace.value = null
                replacedEvent?.also { onMessageReplaceFinished(it.first, it.second) }
                repliedEvent?.also { onMessageReplyFinished(it.first, it.second) }
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

    override fun onAttachmentFileSelect(file: FileDescriptor) {
        log.debug { "selected as attachment: ${file.fileName} of size: ${file.fileSize}" }
        onShowAttachmentSendView(file)
    }

    override fun editMessage(roomId: RoomId, eventId: EventId) {
        log.debug { "edit message $eventId" }
        coroutineScope.launch {
            matrixClient.room.getTimelineEvent(roomId, eventId).firstOrNull()?.content?.getOrNull()
                ?.let { roomEventContent ->
                    when (roomEventContent) {
                        is TextBased -> {
                            currentReplace.value = roomId to eventId
                            message.value = roomEventContent.bodyWithoutFallback
                            _shouldFocus.tryEmit(Unit)
                        }

                        else -> log.warn { "cannot edit anything besides TextBased" }
                    }
                } ?: log.warn { "cannot get timeline event $eventId" }
        }
    }

    override fun cancelEdit() {
        val currentReplaceValue = currentReplace.value
        if (currentReplaceValue != null) {
            currentReplace.value = null
            message.value = ""
            onMessageReplaceFinished(currentReplaceValue.first, currentReplaceValue.second)
        }
    }

    private data class TimelineElementViewModelWrapper(
        val viewModel: TimelineElementViewModel<*>,
        val lifecycle: LifecycleRegistry,
    )

    private val repliedTimelineElementHolderViewModelFactory = get<RepliedTimelineElementHolderViewModelFactory>()
    private val repliedElementCache = MutableStateFlow<TimelineElementViewModelWrapper?>(null)
    override val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?> =
        currentReply.map { roomIdAndEventId ->
            if (roomIdAndEventId == null) return@map null
            val (roomId, eventId) = roomIdAndEventId
            repliedElementCache.value?.lifecycle?.destroy()
            val eventContent = matrixClient.room.getTimelineEvent(roomId, eventId).first()?.event?.content
            if (eventContent !is MessageEventContent) return@map null
            val repliedEventId = eventContent.relatesTo?.replyTo?.eventId
            if (repliedEventId == null) return@map null
            val lifecycle = LifecycleRegistry()
            lifecycle.start()
            repliedTimelineElementHolderViewModelFactory.create(
                childContextWithOwnLifecycle(lifecycle),
                matrixClient.room.getTimelineEvent(roomId, repliedEventId),
                roomId,
                repliedEventId,
                onOpenMention,
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override fun replyToMessage(roomId: RoomId, eventId: EventId) {
        log.debug { "reply to message ${eventId}" }
        currentReply.value = roomId to eventId
        _shouldFocus.tryEmit(Unit)
    }

    override fun cancelReplyTo() {
        val currentReplyValue = currentReply.value
        if (currentReplyValue != null) {
            currentReply.value = null
            onMessageReplyFinished(currentReplyValue.first, currentReplyValue.second)
        }
    }


    private suspend fun getMentions(
        text: String
    ): List<Username> {
        val matchResult = mentionInLineRegex.findAll(text).lastOrNull()
        val groups = matchResult?.groupValues?.drop(1)
        return if (groups?.size == 1 || groups?.size == 2) {
            val search =
                if (groups.size == 1) groups[0]
                else groups.filter { it.isNotEmpty() }.getOrNull(0) // multiline
                    ?: "" // @ with no prefix yet
            _listOfMentionsLoading.value = true
            val listOfUsers = listOfUsers(search)
            _listOfMentionsLoading.value = false
            listOfUsers
        } else {
            _listOfMentionsLoading.value = null
            emptyList()
        }
    }

    private suspend fun listOfUsers(search: String): List<Username> {
        val allUsers = matrixClient.user.getAll(roomId).first() // wait for all users to load
        val maxPreviewSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
        return allUsers
            .entries.asFlow()
            .map { users -> users.value.first() }
            .filterNotNull()
            .filter { roomUser ->
                val userId = roomUser.userId
                userId != matrixClient.userId && (
                        roomUser.name.contains(search, ignoreCase = true) ||
                                userId.localpart.contains(search, ignoreCase = true) ||
                                userId.domain.contains(search, ignoreCase = true)
                        )
            }
            .take(10)
            .map { roomUser ->
                val avatar = flow {
                    emit(
                        roomUser.avatarUrl?.let { url ->
                            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                                onSuccess = {
                                    it.limitedByteArrayOrNull(
                                        maxPreviewSize
                                    ) {
                                        log.error { "User avatar for user ${roomUser.userId} exceeds max preview limit, so it is not displayed" }
                                    }
                                },
                                onFailure = { null }
                            )
                        }
                    )
                }

                Username(roomUser.userId, roomUser.name, initials.compute(roomUser.name), avatar)
            }.toList()
    }

    private suspend fun typing() {
        if (isTyping.value.not()) {
            isTyping.value = true

            try {
                if (messengerSettings[userId].first()?.base?.typingIsPublic == true) {
                    matrixClient.api.room.setTyping(roomId, matrixClient.userId, true, 30_000)
                }
            } catch (exc: Exception) {
                log.error(exc) { "Something went wrong while setting typing to true" }
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
            matrixClient.api.room.setTyping(roomId, matrixClient.userId, typing = false)
        } catch (exc: Exception) {
            log.error(exc) { "Something went wrong while setting typing to false" }
        }
    }
}

class PreviewInputViewModel() : InputAreaViewModel {
    override val isAllowedToSendMessages: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val message: MutableStateFlow<String> = MutableStateFlow("")
    override val isSendEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showAttachmentSelectDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasShownAttachmentSelectDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val shouldFocus: Flow<Unit> = emptyFlow()
    override val isEdit: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?> = MutableStateFlow(null)
    override val isReplyTo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val listOfMentions: MutableStateFlow<List<Username>> = MutableStateFlow(emptyList())
    override val listOfMentionsLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val currentCursorPosition: MutableStateFlow<Int?> = MutableStateFlow(null)
    override val useMarkdown: StateFlow<Boolean> = MutableStateFlow(true)

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

    override fun onAttachmentFileSelect(file: FileDescriptor) {
    }

    override fun editMessage(roomId: RoomId, eventId: EventId) {
    }

    override fun cancelEdit() {
    }

    override fun replyToMessage(roomId: RoomId, eventId: EventId) {
    }

    override fun cancelReplyTo() {
    }
}
