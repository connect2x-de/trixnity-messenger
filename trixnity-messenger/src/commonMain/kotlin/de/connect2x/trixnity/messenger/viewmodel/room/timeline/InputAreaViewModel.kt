package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.byEventId
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.room.message.mentions
import net.folivo.trixnity.client.room.message.replace
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import net.folivo.trixnity.utils.concurrentMutableMap
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.HtmlGenerator.TagRenderer
import org.intellij.markdown.parser.MarkdownParser
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds
import net.folivo.trixnity.core.model.Mention as TrixnityMention

private val log = KotlinLogging.logger { }

private sealed interface SubstringType {
    suspend fun format(matrixClient: MatrixClient, roomId: RoomId): String

    data class Text(val text: String) : SubstringType {
        override suspend fun format(matrixClient: MatrixClient, roomId: RoomId): String =
            this.text
    }

    data class Mention(val mention: TrixnityMention) : SubstringType {
        override suspend fun format(matrixClient: MatrixClient, roomId: RoomId): String =
            when (val mention = this.mention) {
                is TrixnityMention.Event -> {
                    val roomId = mention.roomId ?: roomId
                    val matrixUri = "https://matrix.to/#/${roomId.full}/${mention.eventId.full}"
                    val anchorContent = mention.label ?: matrixUri

                    """<a href="$matrixUri">$anchorContent</a>"""
                }

                is TrixnityMention.Room -> {
                    val alias =
                        matrixClient.room.getState<CanonicalAliasEventContent>(mention.roomId)
                            .first()
                            ?.content?.run { alias ?: aliases?.firstOrNull() }
                    val matrixUri =
                        if (alias != null) "https://matrix.to/#/${alias.full}"
                        else "https://matrix.to/#/${roomId.full}"
                    val anchorContent = mention.label ?: alias?.full ?: mention.roomId.full

                    """<a href="$matrixUri">$anchorContent</a>"""
                }

                is TrixnityMention.RoomAlias -> {
                    val matrixUri = "https://matrix.to/#/${mention.roomAliasId.full}"
                    val anchorContent = mention.label ?: mention.roomAliasId.full

                    """<a href="$matrixUri">$anchorContent</a>"""
                }

                is TrixnityMention.User -> {
                    val userName =
                        matrixClient.user.getById(roomId, mention.userId).first()?.name
                    val matrixUri = "https://matrix.to/#/${mention.userId.full}"
                    val anchorContent = mention.label ?: userName ?: mention.userId.full

                    """<a href="$matrixUri">$anchorContent</a>"""
                }
            }
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
    val textField: TextFieldViewModel
    val isSendEnabled: StateFlow<Boolean>
    val showAttachmentSelectDialog: StateFlow<Boolean>
    val hasShownAttachmentSelectDialog: SharedFlow<Boolean>
    val isReplace: StateFlow<Boolean>
    val isReply: StateFlow<Boolean>
    val repliedElement: StateFlow<TimelineElementHolderViewModel?>
    val listOfMentions: StateFlow<List<UserInfoElement>?>
    val listOfMentionsLoading: StateFlow<Boolean>
    val useMarkdown: StateFlow<Boolean>

    fun selectMention(userId: UserId)
    fun sendMessage()
    fun selectAttachment()
    fun closeAttachmentDialog()
    fun onAttachmentFileSelect(file: FileDescriptor)
    fun replaceMessage(roomId: RoomId, eventId: EventId)
    fun cancelReplace()
    fun replyMessage(roomId: RoomId, eventId: EventId)
    fun cancelReply()
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
    private val timeZone = get<TimeZone>()
    private val initials = get<Initials>()

    override val isAllowedToSendMessages: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<RoomMessageEventContent>(roomId)
            .stateIn(coroutineScope, WhileSubscribed(), false)
    override val textField = TextFieldViewModelImpl(maxLength = 20_000)
    override val isSendEnabled: StateFlow<Boolean> =
        textField.map { it.text.isNotBlank() }.stateIn(coroutineScope, Eagerly, false)
    override val showAttachmentSelectDialog = MutableStateFlow(false)

    private val isTyping = MutableStateFlow(false)
    private val isStillTyping = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val currentReplace: MutableStateFlow<Pair<RoomId, EventId>?> = MutableStateFlow(null)
    override val isReplace: StateFlow<Boolean> =
        currentReplace.map { it != null }.stateIn(coroutineScope, WhileSubscribed(), false)
    private val currentReply = MutableStateFlow<Pair<RoomId, EventId>?>(null)
    override val isReply: StateFlow<Boolean> =
        currentReply.map { it != null }.stateIn(coroutineScope, WhileSubscribed(), false)
    private val _listOfMentionsLoading = MutableStateFlow(false)
    override val listOfMentionsLoading: StateFlow<Boolean> = _listOfMentionsLoading.asStateFlow()

    override val useMarkdown = MutableStateFlow(true)
    private val markdownFlavourDescriptor = CommonMarkFlavourDescriptor()
    private val markdownParser = MarkdownParser(markdownFlavourDescriptor)

    private class HtmlTagRenderer() : TagRenderer {
        override fun openTag(
            node: ASTNode,
            tagName: CharSequence,
            vararg attributes: CharSequence?,
            autoClose: Boolean
        ): CharSequence = when (tagName) {
            // Recommended Tag Whitelist
            // https://spec.matrix.org/v1.13/client-server-api/#mroommessage-msgtypes
            "del", "h1", "h2", "h3", "h4", "h5", "h6",
            "blockquote", "p", "a", "ul", "ol", "sup",
            "sub", "li", "b", "i", "u", "strong", "em",
            "s", "code", "hr", "br", "div", "table",
            "thead", "tbody", "tr", "th", "td", "caption",
            "pre", "span", "img", "details", "summary" ->
                buildString {
                    append("<$tagName")
                    attributes.forEach { attribute ->
                        if (attribute != null) {
                            append(" $attribute")
                        }
                    }

                    if (autoClose) {
                        append(" />")
                    } else {
                        append(">")
                    }
                }

            else -> ""
        }

        override fun closeTag(tagName: CharSequence): CharSequence = if (tagName == "body") "" else "</$tagName>"

        override fun printHtml(html: CharSequence): CharSequence = html
    }

    override val listOfMentions: StateFlow<List<UserInfoElement>?> =
        textField.map { textFieldValue ->
            val userIdLocalPartBeforeCursor = textFieldValue.mentionBeforeCursor()
            if (userIdLocalPartBeforeCursor != null) {
                _listOfMentionsLoading.value = true
                val listOfUsers = listOfUsers(userIdLocalPartBeforeCursor)
                _listOfMentionsLoading.value = false
                listOfUsers
            } else null
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    init {
        coroutineScope.launch {
            isStillTyping.debounce(3.seconds).collect {
                userIsNotTyping()
            }
        }
        coroutineScope.launch {
            textField.collect {
                if (it.text.isEmpty()) userIsNotTyping()
                else typing()
            }
        }
    }

    override val hasShownAttachmentSelectDialog =
        showAttachmentSelectDialog.debounce(200).shareIn(coroutineScope, Eagerly, replay = 1)

    override fun selectMention(userId: UserId) {
        if (listOfMentions.value?.any { it.userId == userId } != true) return
        val textFieldValue = textField.value
        val (text, selection) = textFieldValue
        val userIdLocalPartBeforeCursor = textFieldValue.mentionBeforeCursor()
        if (userIdLocalPartBeforeCursor != null && selection != null) {
            val userIdStart = selection.last - userIdLocalPartBeforeCursor.length - 2 // 1 for @ and 1 for cursor
            textField.update(
                text = buildString {
                    append(text.substring(0..userIdStart))
                    append(userId)
                    append(text.substring(selection.last.coerceAtMost(text.length)))
                },
                selection = IntRange(userIdStart + userId.full.length + 1),
            )
        }
    }

    private fun TextFieldViewModel.State.mentionBeforeCursor() =
        if (text.isNotEmpty() && selection != null && selection.firstIsLast()) {
            text.substring(0..(selection.last - 1).coerceIn(0..text.lastIndex))
                .takeLast(50)
                .takeIf { it.contains('@') }
                ?.substringAfterLast('@')
        } else null

    override fun sendMessage() {
        log.trace { "try to send message" }
        if (isSendEnabled.value == true) {
            val text = textField.value.text
            textField.update("")
            coroutineScope.launch {
                val mentions = MatrixRegex.findMentions(text)
                val mentionedUsers = mentions.values.filterIsInstance<TrixnityMention.User>().map { it.userId }.toSet()
                val formattedMentions =
                    mentions.entries.withIndex()
                        .windowed(
                            size = 2,
                            partialWindows = true
                        ) { mentionWindow ->
                            val first = mentionWindow[0]
                            val second = mentionWindow.getOrNull(1)

                            listOfNotNull(
                                if (first.index == 0) SubstringType.Text(text.substring(0 until first.value.key.first))
                                else null,
                                SubstringType.Mention(first.value.value),
                                if (second == null) SubstringType.Text(text.substring(first.value.key.last + 1 until text.length))
                                else SubstringType.Text(text.substring(first.value.key.last + 1 until second.value.key.start))
                            )
                        }.flatten()
                        .map { substring ->
                            substring.format(matrixClient, roomId)
                        }.joinToString("")
                        .ifBlank { text }

                val formattedBody =
                    when (useMarkdown.value) {
                        true ->
                            HtmlGenerator(
                                formattedMentions,
                                markdownParser.buildMarkdownTreeFromString(formattedMentions),
                                markdownFlavourDescriptor
                            ).generateHtml(HtmlTagRenderer())

                        false -> formattedMentions
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
                repliedEvent?.also {
                    currentReply.value = null
                    onMessageReplyFinished(it.first, it.second)
                }
            }
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

    override fun replaceMessage(roomId: RoomId, eventId: EventId) {
        log.debug { "edit message $eventId" }
        coroutineScope.launch {
            matrixClient.room.getTimelineEvent(roomId, eventId).first()?.content?.getOrNull()
                ?.let { roomEventContent ->
                    when (roomEventContent) {
                        is TextBased -> {
                            currentReplace.value = roomId to eventId
                            textField.update(roomEventContent.bodyWithoutFallback)
                        }

                        else -> log.warn { "cannot edit anything besides TextBased" }
                    }
                } ?: log.warn { "cannot get timeline event content of $eventId" }
        }
    }

    override fun cancelReplace() {
        val currentReplaceValue = currentReplace.value
        if (currentReplaceValue != null) {
            currentReplace.value = null
            textField.update("")
            onMessageReplaceFinished(currentReplaceValue.first, currentReplaceValue.second)
        }
    }

    private val getReceiptsByEventCache = concurrentMutableMap<RoomId, Flow<Map<EventId, Set<UserId>>>>()
    private fun getReceipts(roomId: RoomId): Flow<Map<EventId, Set<UserId>>> =
        flow {
            emitAll(
                getReceiptsByEventCache.read { get(roomId) }
                    ?: getReceiptsByEventCache.write {
                        getOrPut(roomId) {
                            matrixClient.user.getAllReceipts(roomId)
                                .byEventId(userId)
                                .stateIn(coroutineScope, WhileSubscribed(), emptyMap())
                        }
                    }
            )
        }

    private data class TimelineElementHolderViewModelWrapper(
        val roomId: RoomId,
        val eventId: EventId,
        val viewModel: TimelineElementHolderViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private val timelineElementHolderViewModelFactory = get<TimelineElementHolderViewModelFactory>()
    private val repliedElementCache = MutableStateFlow<TimelineElementHolderViewModelWrapper?>(null)
    override val repliedElement: StateFlow<TimelineElementHolderViewModel?> =
        currentReply.map { roomIdAndEventId ->
            if (roomIdAndEventId == null) return@map null
            val (roomId, eventId) = roomIdAndEventId
            val repliedElementCacheValue = repliedElementCache.value
            if (repliedElementCacheValue?.roomId == roomId && repliedElementCacheValue.eventId == eventId)
                return@map repliedElementCacheValue.viewModel
            val timelineEventFlow = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull()
            val timelineEvent = timelineEventFlow.first()
            repliedElementCache.value?.lifecycle?.destroy()
            val lifecycle = LifecycleRegistry()
            lifecycle.start()
            timelineElementHolderViewModelFactory.create(
                viewModelContext = childContextWithOwnLifecycle(lifecycle),
                key = "element",
                timelineEventFlow = timelineEventFlow,
                roomId = roomId,
                eventId = eventId,
                sender = timelineEvent.sender,
                formattedDate = formatDate(
                    Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                        .toLocalDateTime(timeZone)
                ),
                formattedTime = formatTime(
                    Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                        .toLocalDateTime(timeZone)
                ),
                showLoadingIndicatorBefore = flowOf(false),
                showLoadingIndicatorAfter = flowOf(false),
                showUnreadMarker = flowOf(false),
                getReceipts = ::getReceipts,
                onMessageReplace = { _, _ -> },
                onMessageReply = { _, _ -> },
                onMessageReport = { _, _ -> },
                onOpenMention = { _, _ -> },
                onOpenMetadata = {},
                jumpTo = { _, _ -> },
                ignoreReplacedEvents = true,
            ).also {
                repliedElementCache.value = TimelineElementHolderViewModelWrapper(roomId, eventId, it, lifecycle)
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override fun replyMessage(roomId: RoomId, eventId: EventId) {
        log.debug { "reply to message ${eventId}" }
        currentReply.value = roomId to eventId
        textField.update(textField.textValue, textField.selectionValue)
    }

    override fun cancelReply() {
        val currentReplyValue = currentReply.value
        if (currentReplyValue != null) {
            currentReply.value = null
            onMessageReplyFinished(currentReplyValue.first, currentReplyValue.second)
        }
    }

    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    private suspend fun listOfUsers(search: String): List<UserInfoElement> {
        val allUsers = matrixClient.user.getAll(roomId).first() // wait for all users to load
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
                roomUser.toUserInfoElement(coroutineScope, matrixClient, initials, maxMediaSizeInMemory)
            }.toList()
    }

    private suspend fun typing() {
        if (isTyping.value.not()) {
            isTyping.value = true

            try {
                if (messengerSettings[userId].first()?.base?.typingIsPublic == true) {
                    // TODO after 30_000s is set to false on server, but not re-set to true by client again!
                    //  maybe consider using something like `lastTyping: StateFlow<Instant>` instead of 2 fields (isTyping, isStillTyping)
                    matrixClient.api.room.setTyping(roomId, matrixClient.userId, true, 30_000)
                }
            } catch (exc: Exception) {
                log.error(exc) { "Something went wrong while setting typing to true" }
            }
        }
        isStillTyping.emit(Unit)
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

private fun IntRange(value: Int) = IntRange(value, value)
private fun IntRange.firstIsLast() = first == last

class PreviewInputAreaViewModel : InputAreaViewModel {
    override val isAllowedToSendMessages: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val textField = TextFieldViewModelImpl(maxLength = 20_000)
    override val isSendEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showAttachmentSelectDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasShownAttachmentSelectDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReplace: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val repliedElement: StateFlow<TimelineElementHolderViewModel?> = MutableStateFlow(null)
    override val isReply: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val listOfMentions: MutableStateFlow<List<UserInfoElement>?> = MutableStateFlow(null)
    override val listOfMentionsLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val useMarkdown: StateFlow<Boolean> = MutableStateFlow(true)

    override fun selectMention(userId: UserId) {
    }

    override fun sendMessage() {
    }

    override fun selectAttachment() {
    }

    override fun closeAttachmentDialog() {
    }

    override fun onAttachmentFileSelect(file: FileDescriptor) {
    }

    override fun replaceMessage(roomId: RoomId, eventId: EventId) {
    }

    override fun cancelReplace() {
    }

    override fun replyMessage(roomId: RoomId, eventId: EventId) {
    }

    override fun cancelReply() {
    }
}
