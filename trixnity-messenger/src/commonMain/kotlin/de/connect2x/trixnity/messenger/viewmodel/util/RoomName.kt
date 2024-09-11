package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.i18n.I18n
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
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


interface RoomName {
    fun getRoomName(
        roomId: RoomId,
        matrixClient: MatrixClient,
        formatted: Boolean = true,
    ): Flow<String>

    fun getRoomName(
        room: Room,
        matrixClient: MatrixClient,
        formatted: Boolean = true,
    ): Flow<String>

    suspend fun getInviterName(
        roomId: RoomId,
        matrixClient: MatrixClient,
    ): String
}

open class RoomNameImpl(
    private val i18n: I18n,
    private val roomInviter: RoomInviter,
) : RoomName {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRoomName(
        roomId: RoomId,
        matrixClient: MatrixClient,
        formatted: Boolean
    ): Flow<String> {
        return matrixClient.room.getById(roomId).flatMapLatest { room ->
            if (room == null) {
                calculateRoomName(roomId, null, matrixClient)
            } else {
                getRoomName(room, matrixClient)
            }
        }
    }

    override fun getRoomName(
        room: Room,
        matrixClient: MatrixClient,
        formatted: Boolean
    ): Flow<String> {
        return if (room.membership == Membership.INVITE && formatted) {
            if (room.name != null) {
                calculateRoomName(
                    room.roomId,
                    room.name,
                    matrixClient,
                ).map {
                    i18n.roomNameInvitationFrom(
                        if (room.isDirect) i18n.roomNameChat() else i18n.roomNameGroup(),
                        it
                    )
                }
            } else {
                flowOf(i18n.roomNameInvitation())
            }
        } else {
            calculateRoomName(
                room.roomId,
                room.name,
                matrixClient,
            )
        }
    }

    override suspend fun getInviterName(
        roomId: RoomId,
        matrixClient: MatrixClient,
    ) = roomInviter.getInviter(matrixClient, roomId)?.let { inviter ->
        matrixClient.room.getState<MemberEventContent>(
            roomId,
            inviter.full,
        ).first()?.content?.displayName ?: inviter.full
    } ?: i18n.commonUnknown()

    open fun calculateRoomName(
        roomId: RoomId,
        name: RoomDisplayName?,
        matrixClient: MatrixClient,
    ): Flow<String> {
        if (name != null) {
            val (explicitName, roomIsEmpty, otherUsersCount) = name
            val heroes = name.heroes

            return when {
                !explicitName.isNullOrEmpty() -> flowOf(explicitName)
                heroes.isEmpty() || roomIsEmpty -> flowOf(i18n.roomNameEmptyChat())
                else -> combine(heroes.map { matrixClient.user.getById(roomId, it) }) {
                    val heroConcat = it.mapIndexed { index: Int, roomUser: RoomUser? ->
                        when {
                            otherUsersCount == 0L && index < heroes.size - 2 || otherUsersCount > 0L && index < heroes.size - 1 -> {
                                nameFromHeroes(roomUser, heroes, index) + ", "
                            }

                            otherUsersCount == 0L && index == heroes.size - 2 -> {
                                nameFromHeroes(roomUser, heroes, index) + " ${i18n.roomNameAnd()} "
                            }

                            otherUsersCount > 0L && index == heroes.size - 1 -> {
                                nameFromHeroes(
                                    roomUser,
                                    heroes,
                                    index
                                ) + " ${i18n.roomNameAnd()} ${i18n.roomNameOther(otherUsersCount)}"
                            }

                            else -> {
                                nameFromHeroes(roomUser, heroes, index)
                            }
                        }
                    }.joinToString("")
                    if (roomIsEmpty) i18n.roomNameEmptyChatWas(heroConcat)
                    else heroConcat
                }
            }
        }

        return flowOf(roomId.full)
    }

    protected open fun nameFromHeroes(
        roomUser: RoomUser?,
        heroes: List<UserId>,
        index: Int
    ) = (roomUser?.name ?: heroes[index].full)

}
