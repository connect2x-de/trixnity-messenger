package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.MatrixMentions
import de.connect2x.trixnity.messenger.util.html.AutoLinkifyVisitor
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.util.html.HtmlVisitor
import de.connect2x.trixnity.messenger.viewmodel.EventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toRoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import net.folivo.trixnity.core.model.events.m.room.formattedBodyWithoutFallback
import org.koin.core.component.get

abstract class TextBasedRoomMessageTimelineElementViewModel<C : RoomMessageEventContent.TextBased>(
    private val viewModelContext: MatrixClientViewModelContext,
    content: C,
    private val roomId: RoomId,
    private val onOpenMention: OpenMentionCallback,
) : RoomMessageTimelineElementViewModel.TextBased<C>, MatrixClientViewModelContext by viewModelContext {
    private val initials = get<Initials>()
    private val roomName = get<RoomName>()
    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    override val body: String = content.bodyWithoutFallback
    override val formattedBody: String? = content.formattedBodyWithoutFallback
    override val formattedBodyContent: HtmlNode.HtmlElement =
        content.formattedBodyWithoutFallback
            ?.let(HtmlVisitor::process)
            ?.let(AutoLinkifyVisitor::process)
            ?: HtmlNode.HtmlElement("#root", emptyMap(), listOf(HtmlNode.TextContent(content.body)))
                .let(AutoLinkifyVisitor::process)

    override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>> by lazy {
        MatrixMentions.findInText(body)
            .mapValues { (_, mention) -> processMention(mention) }
    }

    private val mentionFlowsInFormattedBody =
        formattedBodyContent
            .let(MatrixMentions::findInHtml)
            .mapValues { (_, mention) -> processMention(mention) }
            .map { (key, flow) -> flow.map { Pair(key, it) } }

    override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
        combine(mentionFlowsInFormattedBody) { it.toMap() }
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    override fun openMention(mention: TimelineElementMention) {
        onOpenMention(userId, mention)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun processMention(mention: Mention): StateFlow<TimelineElementMention?> =
        when (mention) {
            is Mention.User -> matrixClient.user.getById(roomId, mention.userId)
                .map {
                    TimelineElementMention.User(
                        // TODO call api.user.getProfile as fallback
                        it.toUserInfoElement(
                            coroutineScope,
                            matrixClient,
                            initials,
                            mention.userId,
                            maxMediaSizeInMemory,
                        )
                    )
                }
                .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

            is Mention.Room -> parseRoom(mention.roomId, matrixClient, initials)
                .map { info ->
                    TimelineElementMention.Room(info)
                }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

            is Mention.RoomAlias ->
                flow {
                    emitAll(
                        parseRoom(mention.roomAliasId, matrixClient, initials)
                            .map { info ->
                                info?.let { TimelineElementMention.Room(info) }
                            }
                    )
                }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

            is Mention.Event -> parseRoom(mention.roomId ?: roomId, matrixClient, initials)
                .map { roomInfo ->
                    TimelineElementMention.Event(EventInfoElement(mention.eventId), roomInfo)
                }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
        }

    private fun parseRoom(
        roomId: RoomId,
        matrixClient: MatrixClient,
        initials: Initials,
        forceAlias: RoomAliasId? = null
    ): Flow<RoomInfoElement> =
        combine(
            matrixClient.room.getById(roomId),
            roomName.getRoomName(roomId, matrixClient),
            matrixClient.room.getState<CanonicalAliasEventContent>(roomId).map { it?.content },
        ) { room, roomName, aliases ->
            room?.toRoomInfoElement(
                coroutineScope,
                initials,
                matrixClient,
                forceAlias?.full ?: roomName,
                maxMediaSizeInMemory
            ) ?: forceAlias?.let { alias ->
                RoomInfoElement(
                    name = forceAlias.full,
                    roomId = roomId,
                    roomImageInitials = initials.compute(forceAlias.full),
                    roomImage = null,
                )
            } ?: RoomInfoElement(
                roomName,
                roomId,
                initials.compute(roomName),
                null,
            )
        }

    private suspend fun findRoomAlias(roomAliasId: RoomAliasId): RoomId? =
        matrixClient.room.getAll().first().firstNotNullOfOrNull { (roomId, _) ->
            val aliasEvent = matrixClient.room.getState<CanonicalAliasEventContent>(roomId).first()?.content
                ?: return@firstNotNullOfOrNull null

            if (aliasEvent.alias == roomAliasId || aliasEvent.aliases?.contains(roomAliasId) == true) roomId
            else null
        }

    private suspend fun lookupRoomAlias(roomAliasId: RoomAliasId): RoomId? =
        matrixClient.api.room.getRoomAlias(roomAliasId).getOrNull()?.roomId

    private suspend fun parseRoom(
        roomAliasId: RoomAliasId,
        matrixClient: MatrixClient,
        initials: Initials,
    ): Flow<RoomInfoElement?> {
        val roomId = findRoomAlias(roomAliasId) ?: lookupRoomAlias(roomAliasId) ?: return flowOf(null)
        return parseRoom(roomId, matrixClient, initials, roomAliasId)
    }
}
