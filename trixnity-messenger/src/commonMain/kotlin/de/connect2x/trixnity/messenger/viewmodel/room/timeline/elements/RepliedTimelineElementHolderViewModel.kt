package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.RelatesTo
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger { }

interface RepliedTimelineElementHolderViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEventFlow: Flow<TimelineEvent?>,
        roomId: RoomId,
        eventId: EventId,
        onOpenMention: OpenMentionCallback,
        onOpenMedia: OpenMediaCallback,
    ): RepliedTimelineElementHolderViewModel =
        RepliedTimelineElementHolderViewModelImpl(
            viewModelContext = viewModelContext,
            timelineEventFlow = timelineEventFlow,
            roomId = roomId,
            eventId = eventId,
            onOpenMention = onOpenMention,
            onOpenMedia = onOpenMedia,
        )

    companion object : RepliedTimelineElementHolderViewModelFactory
}

interface RepliedTimelineElementHolderViewModel {
    val eventId: EventId

    /**
     * The actual content of the element.
     */
    val element: StateFlow<TimelineElementViewModel<*>?>

    /**
     * This event is from us.
     */
    val isByMe: StateFlow<Boolean?>

    /**
     * The sender of this event.
     */
    val sender: StateFlow<UserInfoElement?>
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RepliedTimelineElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    protected val timelineEventFlow: Flow<TimelineEvent?>,
    protected val roomId: RoomId,
    override val eventId: EventId,
    private val onOpenMention: OpenMentionCallback,
    private val onOpenMedia: OpenMediaCallback,
) : RepliedTimelineElementHolderViewModel, MatrixClientViewModelContext by viewModelContext {
    private val config = get<MatrixMessengerConfiguration>()

    private val initials = get<Initials>()
    private val timelineElementViewModelFactorySelector = get<TimelineElementViewModelFactorySelector>()

    private data class TimelineElementViewModelWrapper(
        val viewModel: TimelineElementViewModel<*>,
        val lifecycle: LifecycleRegistry,
    )

    private val elementCache = MutableStateFlow<TimelineElementViewModelWrapper?>(null)

    private fun getNewContentIfAvailable(msg: RoomOutboxMessage<*>?) =
        (msg?.content?.relatesTo as? RelatesTo.Replace)?.takeIf { it.eventId == eventId }?.newContent

    private val newContentIfReplaced = matrixClient.room.getOutbox(roomId).flatten()
        .map { it.reversed().firstNotNullOfOrNull(::getNewContentIfAvailable) }

    override val element =
        combine(
            timelineEventFlow.filterNotNull(),
            newContentIfReplaced.distinctUntilChanged(),
        ) { timelineEvent, newContent ->
            val currentElement = elementCache.value
            currentElement?.lifecycle?.destroy()

            log.trace { "compute element (timelineEvent=$timelineEvent, newContent=$newContent)" }
            val content = newContent?.let { Result.success(it) } ?: timelineEvent.content

            val lifecycle = LifecycleRegistry()
            lifecycle.start()
            timelineElementViewModelFactorySelector.create(
                childContext("element", lifecycle),
                content,
                roomId,
                EventIdOrTransactionId(eventId),
                onOpenMention,
                onOpenMedia,
            ).also {
                elementCache.value = TimelineElementViewModelWrapper(it, lifecycle)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5.seconds), null)

    private val senderUserId =
        timelineEventFlow.map { it?.sender }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement?> =
        flow {
            emitAll(
                matrixClient.user.getById(roomId, senderUserId.filterNotNull().first()).map { user ->
                    user?.toUserInfoElement(matrixClient, initials, config.avatarMaxSize)
                }
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val isByMe: StateFlow<Boolean?> =
        flow {
            emit(senderUserId.filterNotNull().first() == userId)
        }.stateIn(coroutineScope, WhileSubscribed(), null)
}

class PreviewRepliedTimelineElementViewModel1 : RepliedTimelineElementHolderViewModel {
    override val eventId: EventId = EventId("\$1:localhost")
    override val element: StateFlow<TimelineElementViewModel<*>?> =
        MutableStateFlow(object : RoomMessageTimelineElementViewModel.TextBased.Text {
            override val body: String = "Hello everyone!"
            override val formattedBody: String = "Hello <b/>everyone!"
            override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override fun openMention(timelineElementMention: TimelineElementMention) {}
        })
    override val isByMe: StateFlow<Boolean> = MutableStateFlow(true)
    override val sender: StateFlow<UserInfoElement?> = MutableStateFlow(null)
}
