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
    private val navigation = StackNavigation<ImageConfig>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = ImageConfig.serializer(),
        initialConfiguration = ImageConfig.None,
        key = "ImageRouter",
        childFactory = ::createChild,
    )

    private fun createChild(imageConfig: ImageConfig, componentContext: ComponentContext): ImageWrapper =
        when (imageConfig) {
            is ImageConfig.None -> ImageWrapper.None
            is ImageConfig.Image -> ImageWrapper.Image(
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
        navigation.pushSuspending(ImageConfig.Image(mxcUrl, encryptedFile, fileName, userId))
    }

    fun closeImage() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    fun isImageOpen(): Boolean {
        return stack.value.active.configuration is ImageConfig.Image
    }

    @Serializable
    sealed class ImageConfig {
        @Serializable
        data class Image(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val userId: UserId
        ) : ImageConfig()

        @Serializable
        data object None : ImageConfig()
    }

    sealed class ImageWrapper {
        class Image(val imageViewModel: ImageViewModel) : ImageWrapper()
        data object None : ImageWrapper()
    }
}
