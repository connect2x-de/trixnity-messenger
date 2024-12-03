package de.connect2x.trixnity.messenger.viewmodel.media

import MediaViewModel
import MediaViewModelFactory
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.launchReplaceAll
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.EventContent
import org.koin.core.component.get


class MediaRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "MediaRouter",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper =
        when (config) {
            is Config.None -> Wrapper.None

            is Config.Media -> Wrapper.Media(
                viewModelContext.get<MediaViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    content = config.content,
                    onCloseMedia = ::closeMedia,
                )
            )
        }

    suspend fun openMedia(userId: UserId, content: EventContent) {
        navigation.replaceAllSuspending(Config.Media(userId, content))
    }

    fun closeMedia() {
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.None)
    }

    fun isMediaOpen(): Boolean = (stack.value.active.configuration is Config.None).not()

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data class Media(
            val userId: UserId,
            val content: EventContent,
        ) : Config()
    }

    sealed class Wrapper {
        data object None : Wrapper()
        class Media(val viewModel: MediaViewModel) : Wrapper()
    }
}
