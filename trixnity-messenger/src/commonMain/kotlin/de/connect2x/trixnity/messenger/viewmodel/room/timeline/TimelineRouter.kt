package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import kotlinx.serialization.Serializable
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.component.get

interface TimelineRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    suspend fun openTimeline(id: RoomId)
    suspend fun closeTimeline()

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data class View(val roomId: String) : Config()
    }

    sealed class Wrapper {
        data class View(val viewModel: TimelineViewModel) : Wrapper()
        data object None : Wrapper()
    }
}

class TimelineRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val onCloseRoom: () -> Unit,
    private val onOpenRoomSettings: () -> Unit,
    private val onOpenUserProfile: (UserId) -> Unit,
    private val onOpenMention: OpenMentionCallback,
    private val onOpenMetadata: (eventId: EventId) -> Unit,
) : TimelineRouter {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.room.TimelineRouterImpl")
    }

    private val timelineNavigation = StackNavigation<Config>()
    override val stack =
        viewModelContext.childStack(
            source = timelineNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "TimelineRouter",
            childFactory = ::createTimelineChild,
        )

    private fun createTimelineChild(
        timelineConfig: Config,
        componentContext: ComponentContext,
    ): Wrapper =
        when (timelineConfig) {
            is Config.None -> Wrapper.None
            is Config.View -> Wrapper.View(
                viewModelContext.get<TimelineViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext("Timeline", componentContext),
                    roomId = RoomId(timelineConfig.roomId),
                    onBack = onCloseRoom,
                    onOpenRoomSettings = onOpenRoomSettings,
                    onOpenUserProfile = onOpenUserProfile,
                    onOpenMention = onOpenMention,
                    onOpenMetadata = onOpenMetadata,
                )
            )
        }

    override suspend fun openTimeline(id: RoomId) {
        log.debug { "show timeline: $id" }
        timelineNavigation.bringToFrontSuspending(Config.View(roomId = id.full))
    }

    override suspend fun closeTimeline() {
        timelineNavigation.popWhileSuspending { it !is Config.None }
    }
}
