package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.ClientEvent
import kotlinx.serialization.KSerializer
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.core.serialization.events.EventContentSerializerMappings
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

interface TimelineElementDevInfoViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        eventId: EventId,
        roomId: RoomId,
        onBack: () -> Unit,
    ): TimelineElementDevInfoViewModel =
        TimelineElementDevInfoViewModelImpl(
            viewModelContext = viewModelContext,
            eventId = eventId,
            roomId = roomId,
            onBack = onBack,
        )

    companion object : TimelineElementDevInfoViewModelFactory
}

interface TimelineElementDevInfoViewModel {
    val eventId: EventId
    val eventJson: StateFlow<String?>
    val decryptedEventJson: StateFlow<String?>
    fun back()
}

@OptIn(ExperimentalSerializationApi::class)
class TimelineElementDevInfoViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val eventId: EventId,
    roomId: RoomId,
    private val onBack: () -> Unit,
) : TimelineElementDevInfoViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timelineEventEvent: SharedFlow<ClientEvent.RoomEvent<*>> =
        matrixClient.room.getTimelineEvent(roomId, eventId)
            .filterNotNull()
            .map { it.event }
            .shareIn(coroutineScope, WhileSubscribed(), replay = 1)
    private val timelineEventMergedEvent: SharedFlow<ClientEvent.RoomEvent<*>?> =
        matrixClient.room.getTimelineEvent(roomId, eventId)
            .map { it?.mergedEvent?.getOrNull() }
            .shareIn(coroutineScope, WhileSubscribed(), replay = 1)

    private val json = Json(matrixClient.di.get<Json>()) {
        prettyPrint = true
    }
    override val eventJson: StateFlow<String?> =
        timelineEventEvent.map { event ->
            Json.encodeToString(
                json.serializersModule.getContextual(
                    ClientEvent.RoomEvent::class
                ) as SerializationStrategy<ClientEvent.RoomEvent<*>>,
                event
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val decryptedEventJson: StateFlow<String?> =
        timelineEventMergedEvent.filterNotNull().map { event ->
            Json.encodeToString(
                json.serializersModule.getContextual(
                    ClientEvent.RoomEvent::class
                ) as SerializationStrategy<ClientEvent.RoomEvent<*>>,
                event
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    private val backCallback = BackCallback {
        onBack()
    }

    init {
        registerBackCallback(backCallback)
    }

    override fun back() {
        onBack()
    }
}
