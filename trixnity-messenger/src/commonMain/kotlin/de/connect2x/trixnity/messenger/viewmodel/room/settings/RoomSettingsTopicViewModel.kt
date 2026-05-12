package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.canSendEvent
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.TopicEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.html.AutoLinkifyVisitor
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.PreviewApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.MentionHelper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get


interface RoomSettingsTopicViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onOpenMention: OpenMentionCallback,
    ): RoomSettingsTopicViewModel =
        RoomSettingsTopicViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            onOpenMention = onOpenMention,
        )

    companion object : RoomSettingsTopicViewModelFactory
}

interface RoomSettingsTopicViewModel {
    /** Indicates whether the current user is permitted to submit changes. */
    val canChangeRoomTopic: StateFlow<Boolean>

    /** Indicates whether the corresponding UI element needs to be shown. */
    val canViewRoomTopic: StateFlow<Boolean>

    /** Access the state and value of the room topic. */
    val roomTopic: ApprovableTextFieldViewModel

    /**
     * The HTML version of the topic as a tree of HTML nodes, if present.
     */
    val formattedRoomTopic: StateFlow<HtmlNode.HtmlElement>

    /**
     * Users, Events and Room mentioned in the topic's formatted body
     */
    val mentionsInFormattedRoomTopic: StateFlow<Map<String, TimelineElementMention?>>

    /**
     * Open the mention in the UI
     */
    fun openMention(mention: TimelineElementMention)
}

class RoomSettingsTopicViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onOpenMention: OpenMentionCallback,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsTopicViewModel {
    private val mentionHelper = MentionHelper(
        coroutineScope,
        matrixClient,
        selectedRoomId,
        get<Initials>(),
        get<RoomName>(),
        get<MatrixMessengerConfiguration>().maxMediaSizeInMemory,
    )

    override val canChangeRoomTopic: StateFlow<Boolean> =
        matrixClient.user
            .canSendEvent<TopicEventContent>(selectedRoomId)
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canViewRoomTopic: StateFlow<Boolean> =
        matrixClient.room
            .getById(selectedRoomId)
            .map { it?.isDirect?.not() ?: false }
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val roomTopic: ApprovableTextFieldViewModel =
        ApprovableTextFieldViewModelImpl(
            serverValue = matrixClient.room
                .getState<TopicEventContent>(roomId = selectedRoomId)
                .map { it?.content?.topic?.text?.plain ?: it?.content?.legacy?.topic },
            maxLength = 20_000,
            coroutineScope = coroutineScope,
            onApplyChange = { newTopic ->
                matrixClient.api.room.sendStateEvent(
                    selectedRoomId,
                    TopicEventContent(newTopic),
                )
            },
        )

    override val formattedRoomTopic: StateFlow<HtmlNode.HtmlElement> = roomTopic
        .map { formatContent(it.text) }
        .stateIn(coroutineScope, WhileSubscribed(), formatContent(roomTopic.value.text))

    private fun formatContent(body: String): HtmlNode.HtmlElement =
        HtmlNode.HtmlElement("#root", emptyMap(), listOf(HtmlNode.TextContent(body)))
            .let(AutoLinkifyVisitor::process)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val mentionsInFormattedRoomTopic: StateFlow<Map<String, TimelineElementMention?>> =
        formattedRoomTopic.flatMapLatest(mentionHelper::processMentions)
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    override fun openMention(mention: TimelineElementMention) {
        onOpenMention(userId, mention)
    }
}

class PreviewRoomSettingsTopicViewModel : RoomSettingsTopicViewModel {
    override val roomTopic: ApprovableTextFieldViewModel = PreviewApprovableTextFieldViewModel()
    override val canChangeRoomTopic: StateFlow<Boolean> = MutableStateFlow(true)
    override val canViewRoomTopic: StateFlow<Boolean> = MutableStateFlow(true)
    override val formattedRoomTopic: StateFlow<HtmlNode.HtmlElement> = MutableStateFlow(
        HtmlNode.HtmlElement("#root", emptyMap(), listOf(HtmlNode.TextContent("")))
    )
    override val mentionsInFormattedRoomTopic: StateFlow<Map<String, TimelineElementMention?>> =
        MutableStateFlow(emptyMap())

    override fun openMention(mention: TimelineElementMention) = Unit
}
