package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
    ): RepliedTimelineElementHolderViewModel =
        RepliedTimelineElementHolderViewModelImpl(
            viewModelContext = viewModelContext,
            timelineEventFlow = timelineEventFlow,
            roomId = roomId,
            eventId = eventId,
            onOpenMention = onOpenMention,
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

    /**
     * This element should show the sender.
     */
    val showSender: StateFlow<Boolean?>
}

class RepliedTimelineElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    protected val timelineEventFlow: Flow<TimelineEvent?>,
    protected val roomId: RoomId,
    override val eventId: EventId,
    private val onOpenMention: OpenMentionCallback,
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
                childContextWithOwnLifecycle(lifecycle),
                timelineEvent.event.content,
                content,
                roomId,
                EventIdOrTransactionId(eventId),
                onOpenMention,
            ).also {
                elementCache.value = TimelineElementViewModelWrapper(it, lifecycle)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5.seconds), null)

    private val senderUserId = coroutineScope.async {
        timelineEventFlow.map { it?.sender }.filterNotNull().first()
    }

    override val sender: StateFlow<UserInfoElement?> =
        flow {
            val userId = senderUserId.await()
            emitAll(
                matrixClient.user.getById(roomId, userId).map { user ->
                    user.toUserInfoElement(coroutineScope, matrixClient, initials, config.avatarMaxSize, userId)
                }
            )
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val showSender: StateFlow<Boolean?> =
        matrixClient.room.getById(roomId)
            .filterNotNull()
            .map { it.isDirect }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val isByMe: StateFlow<Boolean?> =
        flow {
            emit(senderUserId.await() == userId)
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
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
    override val showSender: StateFlow<Boolean?> = MutableStateFlow(false)
}
