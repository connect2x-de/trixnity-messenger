package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.util.I18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.getSender
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomDisplayName
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership

private val log = KotlinLogging.logger { }

interface RoomName {
    fun getRoomNameElement(
        roomId: RoomId,
        matrixClient: MatrixClient,
    ): Flow<RoomNameElement>

    fun getRoomNameElement(
        room: Room,
        matrixClient: MatrixClient,
    ): Flow<RoomNameElement>

    suspend fun getInviter(
        roomId: RoomId,
        matrixClient: MatrixClient,
    ): String
}

class RoomNameImpl(private val i18n: I18n) : RoomName {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRoomNameElement(
        roomId: RoomId,
        matrixClient: MatrixClient,
    ): Flow<RoomNameElement> {
        return matrixClient.room.getById(roomId).flatMapLatest { room ->
            if (room == null) {
                calculateRoomName(roomId, null, matrixClient).map { RoomNameElement(it) }
            } else {
                getRoomNameElement(room, matrixClient)
            }
        }
    }

    override fun getRoomNameElement(
        room: Room,
        matrixClient: MatrixClient,
    ): Flow<RoomNameElement> {
        return if (room.membership == Membership.INVITE) {
            if (room.name != null) {
                calculateRoomName(
                    room.roomId,
                    room.name,
                    matrixClient,
                ).map {
                    RoomNameElement(
                        i18n.roomNameInvitationFrom(
                            if (room.isDirect) i18n.roomNameChat() else i18n.roomNameGroup(),
                            it
                        )
                    )
                }
            } else {
                flowOf(RoomNameElement(i18n.roomNameInvitation()))
            }
        } else {
            calculateRoomName(
                room.roomId,
                room.name,
                matrixClient,
            ).map { RoomNameElement(it) }
        }
    }

    override suspend fun getInviter(
        roomId: RoomId,
        matrixClient: MatrixClient,
    ) = matrixClient.room.getState<MemberEventContent>(
        roomId,
        matrixClient.userId.full,
    ).first().getSender()?.let { inviter ->
        matrixClient.room.getState<MemberEventContent>(
            roomId,
            inviter.full,
        ).first()?.content?.displayName ?: inviter.full
    } ?: i18n.commonUnknown()

    internal fun calculateRoomName(
        roomId: RoomId,
        name: RoomDisplayName?,
        matrixClient: MatrixClient,
    ): Flow<String> {
        if (name != null) {
            val (explicitName, roomIsEmpty, otherUsersCount) = name
            val heroes = name.heroes

            return when {
                !explicitName.isNullOrEmpty() -> flowOf(explicitName)
                otherUsersCount <= 0 -> {
                    when {
                        heroes.isEmpty() -> flowOf(
                            if (roomIsEmpty) i18n.roomNameEmptyChat()
                            else roomId.full
                        )

                        else -> {
                            combine(heroes.map { matrixClient.user.getById(roomId, it) }) {
                                val heroConcat = it.mapIndexed { index: Int, roomUser: RoomUser? ->
                                    when {
                                        index < heroes.size - 2 -> {
                                            name(roomUser, heroes, index) + ", "
                                        }

                                        index == heroes.size - 2 -> {
                                            name(roomUser, heroes, index) + " ${i18n.roomNameAnd()} "
                                        }

                                        else -> {
                                            name(roomUser, heroes, index)
                                        }
                                    }
                                }.joinToString("")
                                if (roomIsEmpty)
                                    i18n.roomNameEmptyChatWas(heroConcat)
                                else
                                    heroConcat
                            }
                        }
                    }
                }

                else -> {
                    if (heroes.isEmpty()) {
                        flowOf("")
                    } else {
                        combine(heroes.map { matrixClient.user.getById(roomId, it) }) {
                            it.mapIndexed { index: Int, roomUser: RoomUser? ->
                                when {
                                    index < heroes.size - 1 -> {
                                        name(roomUser, heroes, index) + ", "
                                    }

                                    else -> {
                                        name(roomUser, heroes, index) + " ${i18n.roomNameAnd()} "
                                    }
                                }
                            }.joinToString("")
                        }
                    }.map { heroNames ->
                        val heroConcat = when {
                            otherUsersCount > 1 ->
                                i18n.roomNameOthersCount(heroNames, otherUsersCount)

                            else ->
                                when {
                                    heroNames.isEmpty() && !roomIsEmpty -> i18n.roomNameOneOther()
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

                                    else -> heroNames + i18n.roomNameOneOther()
                                }
                        }
                        if (roomIsEmpty) i18n.roomNameEmptyChatWas(heroConcat) else heroConcat
                    }
                }
            }
        }
        return flowOf(roomId.full)
    }

    private fun name(
        roomUser: RoomUser?,
        heroes: List<UserId>,
        index: Int
    ) = (roomUser?.name ?: heroes[index].full)

}
