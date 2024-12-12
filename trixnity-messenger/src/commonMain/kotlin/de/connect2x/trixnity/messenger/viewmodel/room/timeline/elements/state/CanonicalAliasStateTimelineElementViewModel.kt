package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

interface CanonicalAliasStateTimelineElementViewModelFactory :
    TimelineElementViewModelFactory<CanonicalAliasEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: CanonicalAliasEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): CanonicalAliasStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            CanonicalAliasStateTimelineElementViewModelImpl(
                viewModelContext,
                content,
                roomId,
                eventIdOrTransactionId.eventId,
            ) else null

    override val supports: KClass<CanonicalAliasEventContent>
        get() = CanonicalAliasEventContent::class

    companion object : CanonicalAliasStateTimelineElementViewModelFactory
}

interface CanonicalAliasStateTimelineElementViewModel : State<CanonicalAliasEventContent> {
    val changeMessage: StateFlow<List<String>?>
}

class CanonicalAliasStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: CanonicalAliasEventContent,
    roomId: RoomId,
    eventId: EventId,
) : CanonicalAliasStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val changeMessage =
        flow {
            val timelineEvent = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
            emitAll(
                combine(
                    matrixClient.user.getById(roomId, timelineEvent.sender),
                    matrixClient.room.getById(roomId).filterNotNull().map { it.isDirect },
                ) { userInfo, isDirect ->
                    val unsigned = timelineEvent.event.unsigned
                    val previousContent =
                        if (unsigned is UnsignedRoomEventData.UnsignedStateEventData) unsigned.previousContent else null

                    if (previousContent !is CanonicalAliasEventContent) {
                        return@combine emptyList()
                    }

                    if (previousContent == content) {
                        return@combine emptyList()
                    }

                    val previousAliases = previousContent.aliases ?: emptySet()
                    val currentAliases = content.aliases ?: emptySet()
                    val name = userInfo?.name ?: timelineEvent.sender.full
                    val mainAliasChange =
                        if (content.alias != null && content.alias != previousContent.alias) {
                            i18n.setAsMainAlias(name, content.alias.toString())
                        } else if (content.alias == null && currentAliases.contains(previousContent.alias)) {
                            i18n.removeAsMainAlias(name, previousContent.alias.toString())
                        } else null

                    val allCurrentAliases = currentAliases + content.alias
                    val allPreviousAliases = previousAliases + previousContent.alias

                    val newAliases = (currentAliases - allPreviousAliases).map {
                        it?.let { alias ->
                            i18n.addedAlias(name, alias.full)
                        }
                    }

                    val removedAliases = (allPreviousAliases - allCurrentAliases).map {
                        it?.let { alias ->
                            i18n.removedAlias(name, alias.full)
                        }
                    }

                    (newAliases + removedAliases + mainAliasChange).filterNotNull()
                        .ifEmpty {
                            log.warn { "Couldn't identify changes in event" }
                            listOf(i18n.aliasesChanged(name))
                        }
                }
            )
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
}
