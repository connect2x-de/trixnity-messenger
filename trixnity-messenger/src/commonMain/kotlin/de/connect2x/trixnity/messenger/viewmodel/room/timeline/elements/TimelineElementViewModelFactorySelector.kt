package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.EncryptedErrorTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.EncryptedWaitTimelineElementViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.MessageEventContent
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.utils.concurrentMutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

interface TimelineElementViewModelFactorySelector {
    fun nextSupportedTimelineEvent(
        timelineEvents: Flow<Flow<TimelineEvent>>,
        filter: ((TimelineEvent) -> Boolean)? = null
    ): Flow<TimelineEvent?>

    suspend fun supports(timelineEvent: Flow<TimelineEvent>): Boolean

    suspend fun create(
        viewModelContext: MatrixClientViewModelContext,
        originalContent: RoomEventContent,
        content: Result<RoomEventContent>?,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
        ignoreReplacedEvents: Boolean,
    ): TimelineElementViewModel<*>
}

class TimelineElementViewModelFactorySelectorImpl(
    private val factories: List<TimelineElementViewModelFactory<*>>,
    private val encryptedWaitTimelineElementViewModelFactory: EncryptedWaitTimelineElementViewModelFactory,
    private val encryptedErrorTimelineElementViewModelFactory: EncryptedErrorTimelineElementViewModelFactory,
) : TimelineElementViewModelFactorySelector {
    companion object {
        private val log: Logger =
            Logger("de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactorySelectorImpl")
    }

    private sealed interface Mapping {
        data object None : Mapping
        data class Exist(
            val factory: TimelineElementViewModelFactory<RoomEventContent>,
        ) : Mapping

        fun getOrNull() = when (this) {
            is Exist -> factory
            is None -> null
        }
    }

    private val factoryMapping = concurrentMutableMap<KClass<out RoomEventContent>, Mapping>()

    override fun nextSupportedTimelineEvent(
        timelineEvents: Flow<Flow<TimelineEvent>>,
        filter: ((TimelineEvent) -> Boolean)?
    ): Flow<TimelineEvent?> =
        flow {
            timelineEvents.collect { timelineEvent ->
                timelineEvent
                    .map { filter?.invoke(it) != false && supports(it.event.content, it.content) }
                    .onEach { if (it) emitAll(timelineEvent) else emit(null) }
                    .first { !it }
            }
            emit(null) // if start of timeline reached
        }.distinctUntilChanged()

    override suspend fun supports(timelineEvent: Flow<TimelineEvent>): Boolean =
        timelineEvent.map { supports(it.event.content, it.content) }.first()

    private suspend fun supports(originalContent: RoomEventContent, content: Result<RoomEventContent>?): Boolean =
        isReplaceEvent(originalContent).not() &&
                (content == null || content.fold(
                    onFailure = { true },
                    onSuccess = { findFactory(it, ignoreReplacedEvents = false) != null }))

    override suspend fun create(
        viewModelContext: MatrixClientViewModelContext,
        originalContent: RoomEventContent,
        content: Result<RoomEventContent>?,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
        ignoreReplacedEvents: Boolean,
    ): TimelineElementViewModel<*> = when {

        ignoreReplacedEvents && isReplaceEvent(originalContent) -> TimelineElementViewModel.Empty

        content == null -> encryptedWaitTimelineElementViewModelFactory.create(
            viewModelContext = viewModelContext,
        ) ?: TimelineElementViewModel.Empty

        else -> content.fold(
            onFailure = { error ->
                encryptedErrorTimelineElementViewModelFactory.create(
                    viewModelContext = viewModelContext,
                    error = error,
                ) ?: TimelineElementViewModel.Empty
            },
            onSuccess = { decryptedContent ->
                val factory = findFactory(decryptedContent, ignoreReplacedEvents)

                // This has to run on the Main Thread as ViewModels can create routers or stacks which have to be
                // created on the Main Thread to prevent com.arkivanov.decompose.mainthread.NotOnMainThreadException
                withContext(Dispatchers.Main.immediate) {
                    factory?.create(
                        viewModelContext = viewModelContext,
                        content = decryptedContent,
                        roomId = roomId,
                        eventIdOrTransactionId = eventIdOrTransactionId,
                        onOpenMention = onOpenMention,
                    )
                } ?: TimelineElementViewModel.Empty
            },
        )
    }

    private suspend fun findFactory(
        content: RoomEventContent,
        ignoreReplacedEvents: Boolean,
    ): TimelineElementViewModelFactory<RoomEventContent>? {
        if (ignoreReplacedEvents && isReplaceEvent(content)) return null.also { log.debug { "Ignoring replace event: $content" } }

        val contentClass = content::class
        return (factoryMapping.read { get(contentClass) }
            ?: run {
                val foundFactory = factories.firstOrNull { it.supports.isInstance(content) }
                    ?: run {
                        log.warn {
                            "There are no registered view models for ${content::class.simpleName}. " +
                                    "This can be a missing factory in the DI or might be an element that should not be " +
                                    "visible in the timeline."
                        }
                        null
                    }
                if (foundFactory == null) {
                    factoryMapping.write { getOrPut(contentClass) { Mapping.None } }
                    Mapping.None
                } else {
                    @Suppress("UNCHECKED_CAST")
                    foundFactory as TimelineElementViewModelFactory<RoomEventContent>
                    val result = Mapping.Exist(foundFactory)
                    factoryMapping.write { getOrPut(contentClass) { result } }
                    result
                }
            }).getOrNull()
    }

    private fun isReplaceEvent(content: RoomEventContent): Boolean =
        content is MessageEventContent && content.relatesTo is RelatesTo.Replace
}
