package de.connect2x.trixnity.messenger.viewmodel.files

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.RawValue
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.koin.core.component.get

class VideoRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val navigation = StackNavigation<VideoConfig>()
    val stack = viewModelContext.childStack(
        source = navigation,
        initialConfiguration = VideoConfig.None,
        key = "VideoRouter",
        childFactory = ::createChild,
    )

    private fun createChild(videoConfig: VideoConfig, componentContext: ComponentContext): VideoWrapper =
        when (videoConfig) {
            is VideoConfig.None -> VideoWrapper.None
            is VideoConfig.Video -> VideoWrapper.Video(
                viewModelContext.get<VideoViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, videoConfig.accountName),
                    mxcUrl = videoConfig.mxcUrl,
                    encryptedFile = videoConfig.encryptedFile,
                    fileName = videoConfig.fileName,
                    onCloseVideo = ::closeVideo,
                )
            )
        }

    suspend fun openVideo(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, accountName: String) {
        navigation.pushSuspending(VideoConfig.Video(mxcUrl, encryptedFile, fileName, accountName))
    }

    fun closeVideo() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    fun isVideoOpen(): Boolean {
        return stack.value.active.configuration is VideoConfig.Video
    }

    sealed class VideoConfig : Parcelable {
        @Parcelize
        data class Video(
            val mxcUrl: String,
            val encryptedFile: @RawValue EncryptedFile?,
            val fileName: String,
            val accountName: String,
        ) :
            VideoConfig()

        @Parcelize
        object None : VideoConfig()
    }

    sealed class VideoWrapper {
        class Video(val videoViewModel: VideoViewModel) : VideoWrapper()
        object None : VideoWrapper()
    }

}