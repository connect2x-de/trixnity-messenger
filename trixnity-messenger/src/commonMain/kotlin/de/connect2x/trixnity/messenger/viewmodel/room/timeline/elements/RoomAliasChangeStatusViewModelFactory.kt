package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent

interface RoomAliasChangeStatusViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: CanonicalAliasEventContent,
        formattedDate: String,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<UserInfoElement>,
        isDirectFlow: StateFlow<Boolean>,
    ): RoomAliasChangeStatusViewModel {
        return RoomAliasChangeStatusViewModelImpl(
            viewModelContext,
            timelineEvent,
            content,
            formattedDate,
            showDateAbove,
            invitation,
            sender,
            isDirectFlow,
        )
    }

    companion object : RoomAliasChangeStatusViewModelFactory
}

interface RoomAliasChangeStatusViewModel : BaseTimelineElementViewModel {
    val roomAliasChangeMessage: StateFlow<List<String>>
}

open class RoomAliasChangeStatusViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: CanonicalAliasEventContent,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<UserInfoElement>,
    isDirectFlow: StateFlow<Boolean>,
) : MatrixClientViewModelContext by viewModelContext, RoomAliasChangeStatusViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, WhileSubscribed(), null)

    override val roomAliasChangeMessage =
        sender.map { userInfo ->
            val unsigned = timelineEvent?.event?.unsigned
            val previousContent =
                if (unsigned is UnsignedRoomEventData.UnsignedStateEventData) unsigned.previousContent else null

            if (previousContent !is CanonicalAliasEventContent) {
                return@map emptyList()
            }

            val previousAliases = previousContent.aliases ?: emptySet()
            val currentAliases = content.aliases ?: emptySet()

            val mainAliasChange =
                if (content.alias != null && previousAliases.contains(content.alias)) {
                    i18n.setAsMainAlias(userInfo.name, content.alias.toString())
                } else if (previousContent.alias != null && content.alias != previousContent.alias) {
                    i18n.removeAsMainAlias(userInfo.name, previousContent.alias.toString())
                } else null

            val newAliases = (currentAliases - previousAliases - previousContent.alias).map {
                it?.let { alias ->
                    i18n.addedAlias(userInfo.name, alias.full)
                }
            }

            val removedAliases = (previousAliases - previousAliases - content.alias).map {
                it?.let { alias ->
                    i18n.removedAlias(userInfo.name, alias.full)
                }
            }

            (newAliases + removedAliases + mainAliasChange).filterNotNull()
                .ifEmpty { listOf(i18n.aliasesChanged(userInfo.name)) }
        }.stateIn(coroutineScope, WhileSubscribed(), emptyList())
}
