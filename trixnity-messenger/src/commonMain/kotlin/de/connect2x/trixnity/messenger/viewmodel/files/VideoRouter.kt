package de.connect2x.trixnity.messenger.viewmodel.files

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.koin.core.component.get

class VideoRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "VideoRouter",
        childFactory = ::createChild,
    )

    private fun createChild(videoConfig: Config, componentContext: ComponentContext): Wrapper =
        when (videoConfig) {
            is Config.None -> Wrapper.None
            is Config.Video -> Wrapper.Video(
                viewModelContext.get<VideoViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, videoConfig.userId),
                    mxcUrl = videoConfig.mxcUrl,
                    encryptedFile = videoConfig.encryptedFile,
                    fileName = videoConfig.fileName,
                    onCloseVideo = ::closeVideo,
                )
            )
        }

    suspend fun openVideo(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, userId: UserId) {
        navigation.pushSuspending(Config.Video(mxcUrl, encryptedFile, fileName, userId))
    }

    fun closeVideo() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    fun isVideoOpen(): Boolean {
        return stack.value.active.configuration is Config.Video
    }

    @Serializable
    sealed class Config {
        @Serializable
        data class Video(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val userId: UserId,
        ) :
            Config()

        @Serializable
        data object None : Config()
    }

    sealed class Wrapper {
        class Video(val viewModel: VideoViewModel) : Wrapper()
        data object None : Wrapper()
    }

}