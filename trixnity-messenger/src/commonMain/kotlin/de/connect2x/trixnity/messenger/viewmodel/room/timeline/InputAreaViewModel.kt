package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.room.message.MessageBuilder
import de.connect2x.trixnity.client.room.message.mentions
import de.connect2x.trixnity.client.room.message.replace
import de.connect2x.trixnity.client.room.message.reply
import de.connect2x.trixnity.client.room.message.text
import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.client.store.membership
import de.connect2x.trixnity.client.store.originTimestamp
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.PowerLevel
import de.connect2x.trixnity.client.user.canSendEvent
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import de.connect2x.trixnity.core.model.events.m.room.bodyWithoutFallback
import de.connect2x.trixnity.core.util.Reference as TrixnityReference
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.MatrixMarkdownFlavour
import de.connect2x.trixnity.messenger.util.html.toLink
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.toRoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.byEventId
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import de.connect2x.trixnity.utils.concurrentMutableMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.HtmlGenerator.TagRenderer
import org.intellij.markdown.parser.MarkdownParser
import org.koin.core.component.get

private sealed interface SubstringType {
    suspend fun format(matrixClient: MatrixClient, roomId: RoomId): String

    data class Text(val text: String) : SubstringType {
        override suspend fun format(matrixClient: MatrixClient, roomId: RoomId): String = this.text
    }

    data class Reference(val reference: TrixnityReference) : SubstringType {
        override suspend fun format(matrixClient: MatrixClient, roomId: RoomId): String {
            val uri = reference.toLink()
            return when (reference) {
                is TrixnityReference.Event -> {
                    """<a href="$uri">$uri</a>"""
                }

                is TrixnityReference.Room -> {
                    val alias =
                        matrixClient.room.getState<CanonicalAliasEventContent>(reference.roomId).first()?.content?.run {
                            alias ?: aliases?.firstOrNull()
                        }
                    val anchorContent = alias?.full ?: reference.roomId.full
                    """<a href="$uri">$anchorContent</a>"""
                }

                is TrixnityReference.RoomAlias -> {
                    """<a href="$uri">${reference.roomAliasId.full}</a>"""
                }

                is TrixnityReference.User -> {
                    val userName = matrixClient.user.getById(roomId, reference.userId).first()?.name
                    val anchorContent = userName ?: reference.userId.full
                    """<a href="$uri">$anchorContent</a>"""
                }

                is TrixnityReference.Link -> {
                    """<a href="$uri">$uri</a>"""
                }
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
    @Deprecated("use suggestedMentions instead", ReplaceWith("suggestedMentions"))
    val listOfMentions: StateFlow<List<UserInfoElement>?>
    val suggestedMentions: StateFlow<List<SuggestedMention>?>
    @Deprecated("use suggestedMentionsLoading instead", ReplaceWith("suggestedMentionsLoading"))
    val listOfMentionsLoading: StateFlow<Boolean>
    val suggestedMentionsLoading: StateFlow<Boolean>
    val useMarkdown: StateFlow<Boolean>
    val audio: AudioRecordingAreaViewModel

    @Deprecated("use selectionMention with id instead") fun selectMention(userId: UserId) {}

    fun selectMention(id: String)

    fun sendMessage()

    fun selectAttachment()

    fun closeAttachmentDialog()

    fun onAttachmentFileSelect(file: FileDescriptor)

    fun replaceMessage(roomId: RoomId, eventId: EventId)

    fun cancelReplace()

    fun replyMessage(roomId: RoomId, eventId: EventId)

    fun cancelReply()

    sealed interface SuggestedMention {
        val id: String

        data class User(val user: UserInfoElement) : SuggestedMention {
            override val id: String = user.userId.full
        }

        data class AllRoomMembers(val room: RoomInfoElement) : SuggestedMention {
            override val id = "@room"
        }
    }
}

private val roomMentionRegex = "(?<!\\S)@room(?!\\S)".toRegex()

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

    private val enableMessageDrafts = get<MatrixMessengerConfiguration>().features.enableMessageDrafts

    override val isAllowedToSendMessages: StateFlow<Boolean> =
        matrixClient.user
            .canSendEvent<RoomMessageEventContent>(roomId)
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

    // TODO: Move this to an init block
    @Suppress("unused")
    private val completeAudioRecordingOnReplace =
        isReplace.filter { it }.onEach { audio.recorder?.complete() }.launchIn(coroutineScope)

    private val currentReply = MutableStateFlow<Pair<RoomId, EventId>?>(null)
    override val isReply: StateFlow<Boolean> =
        currentReply.map { it != null }.stateIn(coroutineScope, WhileSubscribed(), false)
    private val _listOfMentionsLoading = MutableStateFlow(false)
    override val listOfMentionsLoading: StateFlow<Boolean> = _listOfMentionsLoading.asStateFlow()
    private val _suggestedMentionsLoading = MutableStateFlow(false)
    override val suggestedMentionsLoading: StateFlow<Boolean> = _suggestedMentionsLoading.asStateFlow()

    private val draftMessage: Flow<RoomOutboxMessage<*>?> = matrixClient.room.getDraftMessage(roomId)
    private val draftMutex: Mutex = Mutex()

    override val useMarkdown = MutableStateFlow(true)
    override val audio: AudioRecordingAreaViewModel =
        get<AudioRecordingAreaViewModelFactory>()
            .create(
                viewModelContext = childContext("audioRecordingAreaViewModel"),
                roomId,
                currentReply,
                ::resetCurrentReply,
                draftMutex,
            )
    private val markdownFlavourDescriptor = get<MatrixMarkdownFlavour>()
    private val markdownParser = MarkdownParser(markdownFlavourDescriptor)

    private class HtmlTagRenderer : TagRenderer {
        override fun openTag(
            node: ASTNode,
            tagName: CharSequence,
            vararg attributes: CharSequence?,
            autoClose: Boolean,
        ): CharSequence =
            when (tagName) {
                // Recommended Tag Whitelist
                // https://spec.matrix.org/v1.13/client-server-api/#mroommessage-msgtypes
                "del",
                "h1",
                "h2",
                "h3",
                "h4",
                "h5",
                "h6",
                "blockquote",
                "p",
                "a",
                "ul",
                "ol",
                "sup",
                "sub",
                "li",
                "b",
                "i",
                "u",
                "strong",
                "em",
                "s",
                "code",
                "hr",
                "br",
                "div",
                "table",
                "thead",
                "tbody",
                "tr",
                "th",
                "td",
                "caption",
                "pre",
                "span",
                "img",
                "details",
                "summary" ->
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

    override val suggestedMentions: StateFlow<List<InputAreaViewModel.SuggestedMention>?> =
        flow {
                var lastEmptySearch: String? = null
                fun calculateNextEmptySearch(currentEmptySearch: String?, listSize: Int, search: String): String? {
                    return when {
                        listSize != 0 -> null
                        currentEmptySearch == null || currentEmptySearch.startsWith(search) -> search
                        !search.startsWith(currentEmptySearch) -> null
                        else -> currentEmptySearch
                    }
                }
                emitAll(
                    textField.map { textFieldValue ->
                        val idLocalPartBeforeCursor = textFieldValue.mentionBeforeCursor()
                        if (idLocalPartBeforeCursor != null) {
                            lastEmptySearch?.let {
                                if (idLocalPartBeforeCursor.startsWith(it)) {
                                    return@map emptyList()
                                }
                            }

                            _suggestedMentionsLoading.value = true
                            val listOfMentions = buildList {
                                addAll(listOfUsers(idLocalPartBeforeCursor))
                                getRoomMentionIfAllowed(idLocalPartBeforeCursor)?.let { add(it) }
                            }
                            _suggestedMentionsLoading.value = false

                            lastEmptySearch =
                                calculateNextEmptySearch(lastEmptySearch, listOfMentions.size, idLocalPartBeforeCursor)

                            listOfMentions
                        } else null
                    }
                )
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val listOfMentions: StateFlow<List<UserInfoElement>?> =
        suggestedMentions
            .map { mentions ->
                _listOfMentionsLoading.value = true
                val listOfMentions =
                    mentions?.filterIsInstance<InputAreaViewModel.SuggestedMention.User>()?.map { it.user }
                _listOfMentionsLoading.value = false
                listOfMentions
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    init {
        coroutineScope.launch { isStillTyping.debounce(3.seconds).collect { userIsNotTyping() } }
        coroutineScope.launch { textField.collect { if (it.text.isEmpty()) userIsNotTyping() else typing() } }
        coroutineScope.launch {
            if (enableMessageDrafts) {
                loadDraftIntoTextArea()
                textField.debounce(2.seconds).collect { draftMutex.withLock { saveTextAsDraft() } }
            } else {
                matrixClient.room.deleteDraftMessage(roomId)
            }
        }
        if (enableMessageDrafts) {
            lifecycle.doOnDestroy {
                get<CoroutineScope>().launch {
                    withContext(NonCancellable) { draftMutex.withLock { saveTextAsDraft() } }
                }
            }
        }
    }

    override val hasShownAttachmentSelectDialog =
        showAttachmentSelectDialog.debounce(200).shareIn(coroutineScope, Eagerly, replay = 1)

    private suspend fun loadDraftIntoTextArea() {
        val draftMessage = draftMessage.first()
        when (val content = draftMessage?.content) {
            is TextBased.Text -> {
                when (val relatesTo = content.relatesTo) {
                    is RelatesTo.Reply -> {
                        currentReply.value = draftMessage.roomId to relatesTo.eventId
                        textField.update(content.body)
                    }

                    is RelatesTo.Replace -> {
                        currentReplace.value = draftMessage.roomId to relatesTo.eventId
                        (relatesTo.newContent as? TextBased.Text)?.let { textField.update(it.body) }
                    }

                    else -> {
                        textField.update(content.body)
                    }
                }
            }

            is RoomMessageEventContent.FileBased.Audio -> {
                when (val relatesTo = content.relatesTo) {
                    is RelatesTo.Reply -> {
                        currentReply.value = draftMessage.roomId to relatesTo.eventId
                        audio.loadAudioMessage(content)
                    }

                    is RelatesTo.Replace -> {
                        currentReplace.value = draftMessage.roomId to relatesTo.eventId
                        (relatesTo.newContent as? RoomMessageEventContent.FileBased.Audio)?.let {
                            audio.loadAudioMessage(it)
                        }
                    }

                    else -> {
                        audio.loadAudioMessage(content)
                    }
                }
            }
        }
    }

    private suspend fun MessageBuilder.reply(repliedEvent: Pair<RoomId, EventId>) {
        reply(matrixClient, repliedEvent)
    }

    private fun resetCurrentReply() {
        currentReply.value?.also {
            currentReply.value = null
            onMessageReplyFinished(it.first, it.second)
        }
    }

    private suspend fun getTextMessageBuilder(text: String): (suspend MessageBuilder.() -> Unit) {
        val references = TrixnityReference.findReferences(text)
        val userReferences = references.values.filterIsInstance<TrixnityReference.User>().map { it.userId }.toSet()
        val hasRoomMention = roomMentionRegex.containsMatchIn(text)
        val roomMentionEnabled = canRoomMention.value && hasRoomMention
        val formattedReferences =
            references
                .filterValues { it !is TrixnityReference.Link }
                .entries
                .withIndex()
                .windowed(size = 2, partialWindows = true) { mentionWindow ->
                    val first = mentionWindow[0]
                    val second = mentionWindow.getOrNull(1)

                    listOfNotNull(
                        if (first.index == 0) SubstringType.Text(text.substring(0 until first.value.key.first))
                        else null,
                        SubstringType.Reference(first.value.value),
                        if (second == null)
                            SubstringType.Text(text.substring(first.value.key.last + 1 until text.length))
                        else SubstringType.Text(text.substring(first.value.key.last + 1 until second.value.key.start)),
                    )
                }
                .flatten()
                .map { substring -> substring.format(matrixClient, roomId) }
                .joinToString("")
                .ifBlank { text }

        val formattedBody =
            when (useMarkdown.value) {
                true ->
                    HtmlGenerator(
                            formattedReferences,
                            markdownParser.buildMarkdownTreeFromString(formattedReferences),
                            markdownFlavourDescriptor,
                        )
                        .generateHtml(HtmlTagRenderer())

                false -> formattedReferences
            }

        val replacedEvent = currentReplace.value
        val repliedEvent = currentReply.value

        return {
            when {
                replacedEvent != null -> replace(replacedEvent.second)
                repliedEvent != null -> {
                    reply(repliedEvent)
                }
            }
            mentions(userReferences, roomMentionEnabled)
            text(body = text, format = "org.matrix.custom.html", formattedBody = formattedBody)
        }
    }

    private suspend fun saveTextAsDraft(text: String = textField.value.text) {
        if (text.isEmpty()) {
            if (draftMessage.first()?.content is TextBased.Text) {
                matrixClient.room.deleteDraftMessage(roomId)
            }
            return
        }
        matrixClient.room.setDraftMessage(roomId = roomId, builder = getTextMessageBuilder(text))
    }

    override fun selectMention(id: String) {
        if (suggestedMentions.value?.any { it.id == id } != true) return
        val textFieldValue = textField.value
        val (text, selection) = textFieldValue
        val idLocalPartBeforeCursor = textFieldValue.mentionBeforeCursor()
        if (idLocalPartBeforeCursor != null && selection != null) {
            val userIdStart = selection.last - idLocalPartBeforeCursor.length - 2 // 1 for @ and 1 for cursor
            textField.update(
                text =
                    buildString {
                        append(text.substring(0..userIdStart))
                        append(id)
                        append(text.substring(selection.last.coerceAtMost(text.length)))
                    },
                selection = IntRange(userIdStart + id.length + 1),
            )
        }
    }

    private fun TextFieldViewModel.State.mentionBeforeCursor() =
        if (text.isNotEmpty() && selection != null && selection.firstIsLast() && selection.first != 0) {
            text
                .substring(0..(selection.last - 1).coerceIn(0..text.lastIndex))
                .takeLast(50)
                .takeIf { it.contains('@') }
                ?.substringAfterLast('@')
        } else null

    override fun sendMessage() {
        log.trace { "try to send message" }
        if (isSendEnabled.value) {
            val text = textField.value.text
            textField.update("")
            coroutineScope.launch {
                if (enableMessageDrafts) {
                    draftMutex.withLock {
                        saveTextAsDraft(text)
                        log.debug { "send message" }
                        matrixClient.room.sendDraftMessage(roomId)
                    }
                } else {
                    matrixClient.room.sendMessage(roomId = roomId, builder = getTextMessageBuilder(text))
                }
                currentReplace.value?.also {
                    currentReplace.value = null
                    onMessageReplaceFinished(it.first, it.second)
                }
                resetCurrentReply()
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
            matrixClient.room.getTimelineEvent(roomId, eventId).first()?.content?.getOrNull()?.let { roomEventContent ->
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

    private fun getReceipts(roomId: RoomId): Flow<Map<EventId, Set<UserId>>> = flow {
        emitAll(
            getReceiptsByEventCache.read { get(roomId) }
                ?: getReceiptsByEventCache.write {
                    getOrPut(roomId) {
                        matrixClient.user
                            .getAllReceipts(roomId)
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
        currentReply
            .map { roomIdAndEventId ->
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
                timelineElementHolderViewModelFactory
                    .create(
                        viewModelContext = childContextWithOwnLifecycle(eventId.full, lifecycle),
                        key = "element",
                        timelineEventFlow = timelineEventFlow,
                        roomId = roomId,
                        eventId = eventId,
                        sender = timelineEvent.sender,
                        formattedDate =
                            formatDate(
                                Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone)
                            ),
                        formattedTime =
                            formatTime(
                                Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone)
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
                    )
                    .also {
                        repliedElementCache.value =
                            TimelineElementHolderViewModelWrapper(roomId, eventId, it, lifecycle)
                    }
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

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

    private val maxThumbnailSize = get<MatrixMessengerConfiguration>().loadLimits.thumbnail

    private suspend fun listOfUsers(search: String): List<InputAreaViewModel.SuggestedMention.User> {
        val allUsers = matrixClient.user.getAll(roomId).first() // wait for all users to load
        return allUsers.entries
            .asFlow()
            .map { users -> users.value.first() }
            .filterNotNull()
            .filter { roomUser ->
                val userId = roomUser.userId
                userId != matrixClient.userId &&
                    roomUser.membership == Membership.JOIN &&
                    (roomUser.name.contains(search, ignoreCase = true) ||
                        userId.localpart.contains(search, ignoreCase = true) ||
                        userId.domain.contains(search, ignoreCase = true))
            }
            .take(10)
            .map { roomUser -> roomUser.toUserInfoElement(coroutineScope, matrixClient, initials, maxThumbnailSize) }
            .map { userInfoElement -> InputAreaViewModel.SuggestedMention.User(userInfoElement) }
            .toList()
    }

    private val roomInfoElement =
        matrixClient.room
            .getById(roomId)
            .filterNotNull()
            .scopedMapLatest { room ->
                val roomName = room.name?.explicitName.orEmpty()
                room.toRoomInfoElement(this, initials, matrixClient, roomName, maxThumbnailSize)
            }
            .stateIn(coroutineScope, SharingStarted.Lazily, null)

    private val userPowerLevel = matrixClient.user.getPowerLevel(roomId, userId)
    private val defaultRoomNotificationPowerLevel = 50L
    private val requiredRoomMentionPowerLevel =
        matrixClient.room.getState<PowerLevelsEventContent>(roomId).map { powerLevelEvent ->
            powerLevelEvent?.content?.notifications?.get("room") ?: defaultRoomNotificationPowerLevel
        }

    private val canRoomMention: StateFlow<Boolean> =
        combine(userPowerLevel, requiredRoomMentionPowerLevel) { userPowerLevel, requiredPowerLevel ->
                when (userPowerLevel) {
                    is PowerLevel.Creator -> true
                    is PowerLevel.User -> userPowerLevel.level >= requiredPowerLevel
                }
            }
            .stateIn(coroutineScope, Eagerly, false)

    private suspend fun getRoomMentionIfAllowed(search: String): InputAreaViewModel.SuggestedMention.AllRoomMembers? {
        if (!canRoomMention.value) return null

        val roomInfo = roomInfoElement.filterNotNull().first()
        return roomInfoElement.value
            ?.let { InputAreaViewModel.SuggestedMention.AllRoomMembers(it) }
            ?.takeIf { it.id.contains(search, ignoreCase = true) }
    }

    private suspend fun typing() {
        if (isTyping.value.not()) {
            isTyping.value = true

            try {
                if (messengerSettings[userId].first()?.base?.typingIsPublic == true) {
                    // TODO after 30_000s is set to false on server, but not re-set to true by client again!
                    //  maybe consider using something like `lastTyping: StateFlow<Instant>` instead of 2 fields
                    // (isTyping, isStillTyping)
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
    override val listOfMentions: MutableStateFlow<List<UserInfoElement>> = MutableStateFlow(emptyList())
    override val suggestedMentions: MutableStateFlow<List<InputAreaViewModel.SuggestedMention>?> =
        MutableStateFlow(null)
    override val listOfMentionsLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val suggestedMentionsLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val useMarkdown: StateFlow<Boolean> = MutableStateFlow(true)
    override val audio: AudioRecordingAreaViewModel = PreviewAudioRecordingAreaViewModel()

    override fun selectMention(id: String) {}

    override fun sendMessage() {}

    override fun selectAttachment() {}

    override fun closeAttachmentDialog() {}

    override fun onAttachmentFileSelect(file: FileDescriptor) {}

    override fun replaceMessage(roomId: RoomId, eventId: EventId) {}

    override fun cancelReplace() {}

    override fun replyMessage(roomId: RoomId, eventId: EventId) {}

    override fun cancelReply() {}
}

internal suspend fun MessageBuilder.reply(matrixClient: MatrixClient, repliedEvent: Pair<RoomId, EventId>) {
    val event = matrixClient.room.getTimelineEvent(repliedEvent.first, repliedEvent.second).filterNotNull().first()
    reply(event)
}
