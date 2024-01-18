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

class ImageRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "ImageRouter",
        childFactory = ::createChild,
    )

    private fun createChild(imageConfig: Config, componentContext: ComponentContext): Wrapper =
        when (imageConfig) {
            is Config.None -> Wrapper.None
            is Config.Image -> Wrapper.Image(
                viewModelContext.get<ImageViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, imageConfig.userId),
                    mxcUrl = imageConfig.mxcUrl,
                    encryptedFile = imageConfig.encryptedFile,
                    fileName = imageConfig.fileName,
                    onCloseImage = ::closeImage,
                )
            )
        }

    suspend fun openImage(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, userId: UserId) {
        navigation.pushSuspending(Config.Image(mxcUrl, encryptedFile, fileName, userId))
    }

    fun closeImage() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    fun isImageOpen(): Boolean {
        return stack.value.active.configuration is Config.Image
    }

    @Serializable
    sealed class Config {
        @Serializable
        data class Image(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val userId: UserId
        ) : Config()

        @Serializable
        data object None : Config()
    }

    sealed class Wrapper {
        class Image(val viewModel: ImageViewModel) : Wrapper()
        data object None : Wrapper()
    }
}
