package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface TimelineRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    suspend fun showTimeline(id: RoomId)
    suspend fun closeTimeline()
    fun isShown(): Boolean

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
    private val isBackButtonVisible: MutableStateFlow<Boolean>,
    private val onShowSettings: () -> Unit,
    private val onRoomBack: () -> Unit,
    private val onOpenMention: OpenMentionCallback,
    private val onOpenMetadata: (eventId: EventId) -> Unit,
) : TimelineRouter {

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
                    viewModelContext = viewModelContext.childContext(componentContext),
                    roomId = RoomId(timelineConfig.roomId),
                    isBackButtonVisible = isBackButtonVisible,
                    onShowSettings = onShowSettings,
                    onBack = onRoomBack,
                    onOpenMention = onOpenMention,
                    onOpenMetadata = onOpenMetadata,
                )
            )
        }

    override suspend fun showTimeline(id: RoomId) {
        log.debug { "show timeline: $id" }
        timelineNavigation.bringToFrontSuspending(Config.View(roomId = id.full))
    }

    override suspend fun closeTimeline() {
        timelineNavigation.popWhileSuspending { it !is Config.None }
    }

    override fun isShown(): Boolean =
        when (stack.value.active.configuration) {
            is Config.View -> true
            is Config.None -> false
        }
}
