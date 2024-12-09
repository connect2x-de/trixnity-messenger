package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.utils.concurrentMutableMap
import kotlin.reflect.KClass

interface TimelineElementViewModelFactorySelector {
    suspend fun supports(content: RoomEventContent): Boolean

    suspend fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: Result<RoomEventContent>?,
        roomId: RoomId,
        eventId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): TimelineElementViewModel<*>
}

class TimelineElementViewModelFactorySelectorImpl(
    private val factories: List<TimelineElementViewModelFactory<*>>,
    private val encryptedWaitTimelineElementViewModelFactory: EncryptedWaitTimelineElementViewModelFactory,
    private val encryptedErrorTimelineElementViewModelFactory: EncryptedErrorTimelineElementViewModelFactory,
) : TimelineElementViewModelFactorySelector {

    private val factoryMapping =
        concurrentMutableMap<KClass<out RoomEventContent>, TimelineElementViewModelFactory<RoomEventContent>>()

    override suspend fun supports(content: RoomEventContent): Boolean = findFactory(content) != null

    override suspend fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: Result<RoomEventContent>?,
        roomId: RoomId,
        eventId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): TimelineElementViewModel<*> {
        if (content == null)
            return encryptedWaitTimelineElementViewModelFactory.create(
                viewModelContext = viewModelContext,
            ) ?: TimelineElementViewModel.Empty
        return content.fold(
            onFailure = { error ->
                encryptedErrorTimelineElementViewModelFactory.create(
                    viewModelContext = viewModelContext,
                    error = error,
                ) ?: TimelineElementViewModel.Empty
            },
            onSuccess = { content ->
                findFactory(content)
                    ?.create(
                        viewModelContext = viewModelContext,
                        content = content,
                        roomId = roomId,
                        eventId = eventId,
                        onOpenMention = onOpenMention,
                    )
                    ?: TimelineElementViewModel.Empty
            }
        )
    }

    private suspend fun findFactory(content: RoomEventContent): TimelineElementViewModelFactory<RoomEventContent>? {
        if (replaceEventsShouldNotBeRendered(content)) return null

        val contentClass = content::class
        return factoryMapping.read { get(contentClass) }
            ?: run {
                val foundFactory = factories.firstOrNull { it.supports.isInstance(content) }
                if (foundFactory == null) return@run null
                @Suppress("UNCHECKED_CAST")
                foundFactory as TimelineElementViewModelFactory<RoomEventContent>
                factoryMapping.write { getOrPut(contentClass) { foundFactory } }
            }
    }

    private fun replaceEventsShouldNotBeRendered(content: RoomEventContent): Boolean {
        println(content)
        return content is MessageEventContent && content.relatesTo is RelatesTo.Replace
    }
}
