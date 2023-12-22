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
    private val navigation = StackNavigation<VideoConfig>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = VideoConfig.serializer(),
        initialConfiguration = VideoConfig.None,
        key = "VideoRouter",
        childFactory = ::createChild,
    )

    private fun createChild(videoConfig: VideoConfig, componentContext: ComponentContext): VideoWrapper =
        when (videoConfig) {
            is VideoConfig.None -> VideoWrapper.None
            is VideoConfig.Video -> VideoWrapper.Video(
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
        navigation.pushSuspending(VideoConfig.Video(mxcUrl, encryptedFile, fileName, userId))
    }

    fun closeVideo() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    fun isVideoOpen(): Boolean {
        return stack.value.active.configuration is VideoConfig.Video
    }

    @Serializable
    sealed class VideoConfig {
        @Serializable
        data class Video(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val userId: UserId,
        ) :
            VideoConfig()

        @Serializable
        data object None : VideoConfig()
    }

    sealed class VideoWrapper {
        class Video(val videoViewModel: VideoViewModel) : VideoWrapper()
        data object None : VideoWrapper()
    }

}