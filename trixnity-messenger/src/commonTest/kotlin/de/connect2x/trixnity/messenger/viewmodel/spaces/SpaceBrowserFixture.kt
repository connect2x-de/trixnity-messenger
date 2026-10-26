package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.model.room.GetHierarchy
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.StrippedStateEvent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.TopicEventContent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.core.model.events.m.space.ParentEventContent
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal class SpaceBrowserFixture(val account: UserId, val spaceId: RoomId) {
    val client = mock<MatrixClient>()
    val rooms = mock<RoomService>()
    val roomApi = mock<RoomApiClient>()
    val roomName = mock<RoomName>()
    val content = MutableStateFlow(SpaceContent())
    val space =
        MutableStateFlow<Room?>(
            Room(spaceId, createEventContent = CreateEventContent(type = CreateEventContent.RoomType.Space))
        )
    var hierarchyRequests = 0
    var parentCollectors = 0

    private fun local(item: SpaceBrowserItem) =
        Room(
            item.roomId,
            membership =
                when {
                    item.joined -> Membership.JOIN
                    item.invited -> Membership.INVITE
                    else -> Membership.LEAVE
                },
            createEventContent =
                CreateEventContent(type = if (item.isSpace) CreateEventContent.RoomType.Space else null),
            avatarUrl = item.avatarUrl,
        )

    init {
        val api = mock<MatrixClientServerApiClient>()
        every { client.api } returns api
        every { api.room } returns roomApi
        every { client.userId } returns account
        every { client.di } returns koinApplication { modules(module { single { rooms } }) }.koin
        every { rooms.getById(any()) } calls
            { args ->
                val id = args.arg<RoomId>(0)
                if (id == spaceId) space else content.map { it.items.find { it.roomId == id }?.let(::local) }
            }
        every { rooms.getAll() } returns
            content.map { content ->
                mapOf(spaceId to space) +
                    content.items.associate { item ->
                        item.roomId to
                            this.content.map { value -> value.items.find { it.roomId == item.roomId }?.let(::local) }
                    }
            }
        every { rooms.getAllState(any(), ParentEventContent::class) } returns
            flow {
                parentCollectors++
                try {
                    emit(emptyMap())
                    awaitCancellation()
                } finally {
                    parentCollectors--
                }
            }
        every { rooms.getAllState(any(), ChildEventContent::class) } calls
            { args ->
                if (args.arg<RoomId>(0) == spaceId)
                    content.map { value ->
                        value.items.associate { item ->
                            item.roomId.full to
                                flowOf(
                                    StrippedStateEvent(
                                        ChildEventContent(via = setOf(account.domain)),
                                        sender = account,
                                        stateKey = item.roomId.full,
                                    )
                                )
                        }
                    }
                else flowOf(emptyMap())
            }
        every { rooms.getState(spaceId, TopicEventContent::class, "") } returns flowOf(null)
        every { roomName.getRoomName(any<RoomId>(), client, false) } calls
            { args ->
                val id = args.arg<RoomId>(0)
                content.map { it.items.find { it.roomId == id }?.name ?: "Space" }
            }
        everySuspend { roomApi.getHierarchy(any(), any(), any(), any(), any()) } calls
            {
                hierarchyRequests++
                Result.success(
                    GetHierarchy.Response(
                        rooms =
                            content.value.items.map { item ->
                                hierarchyRoom(item.roomId)
                                    .copy(
                                        name = item.name,
                                        roomType = if (item.isSpace) CreateEventContent.RoomType.Space else null,
                                        joinRule =
                                            when {
                                                item.canJoin -> JoinRulesEventContent.JoinRule.Public
                                                else -> JoinRulesEventContent.JoinRule.Invite
                                            },
                                    )
                            }
                    )
                )
            }
    }

    fun context(scope: TestScope): ViewModelContext =
        with(scope) { testSpaceBrowserContext(MutableStateFlow(mapOf(account to client)), roomName) }
}
