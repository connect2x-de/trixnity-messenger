package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.TimelineConfig
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.TimelineWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface TimelineRouter {
    val timelineStack: Value<ChildStack<TimelineConfig, TimelineWrapper>>
    suspend fun showTimeline(id: RoomId)
    suspend fun closeTimeline()
    fun isShown(): Boolean

    @Serializable
    sealed class TimelineConfig {
        @Serializable
        data object None : TimelineConfig()

        @Serializable
        data class View(val roomId: String) : TimelineConfig()
    }

    sealed class TimelineWrapper {
        data class View(val timelineViewModel: TimelineViewModel) : TimelineWrapper()
        data object None : TimelineWrapper()
    }
}

class TimelineRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val isBackButtonVisible: MutableStateFlow<Boolean>,
    private val onShowSettings: () -> Unit,
    private val onRoomBack: () -> Unit,
    private val onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
) : TimelineRouter {

    private val timelineNavigation = StackNavigation<TimelineConfig>()
    override val timelineStack =
        viewModelContext.childStack(
            source = timelineNavigation,
            serializer = TimelineConfig.serializer(),
            initialConfiguration = TimelineConfig.None,
            key = "TimelineRouter",
            childFactory = ::createTimelineChild,
        )

    private fun createTimelineChild(
        timelineConfig: TimelineConfig,
        componentContext: ComponentContext
    ): TimelineWrapper =
        when (timelineConfig) {
            is TimelineConfig.None -> TimelineWrapper.None
            is TimelineConfig.View -> TimelineWrapper.View(
                viewModelContext.get<TimelineViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    selectedRoomId = RoomId(timelineConfig.roomId),
                    isBackButtonVisible = isBackButtonVisible,
                    onShowSettings = onShowSettings,
                    onBack = onRoomBack,
                    onOpenModal = onOpenModal,
                )
            )
        }


    override suspend fun showTimeline(id: RoomId) {
        log.debug { "show timeline: $id" }
        timelineNavigation.bringToFrontSuspending(TimelineConfig.View(roomId = id.full))
    }

    override suspend fun closeTimeline() {
        timelineNavigation.popWhileSuspending { it !is TimelineConfig.None }
    }

    override fun isShown(): Boolean =
        when (timelineStack.value.active.configuration) {
            is TimelineConfig.View -> true
            is TimelineConfig.None -> false
        }

}