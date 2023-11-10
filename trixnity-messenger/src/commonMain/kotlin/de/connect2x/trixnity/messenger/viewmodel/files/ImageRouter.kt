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

class ImageRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val navigation = StackNavigation<ImageConfig>()
    val stack = viewModelContext.childStack(
        source = navigation,
        initialConfiguration = ImageConfig.None,
        key = "ImageRouter",
        childFactory = ::createChild,
    )

    private fun createChild(imageConfig: ImageConfig, componentContext: ComponentContext): ImageWrapper =
        when (imageConfig) {
            is ImageConfig.None -> ImageWrapper.None
            is ImageConfig.Image -> ImageWrapper.Image(
                viewModelContext.get<ImageViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, imageConfig.accountName),
                    mxcUrl = imageConfig.mxcUrl,
                    encryptedFile = imageConfig.encryptedFile,
                    fileName = imageConfig.fileName,
                    onCloseImage = ::closeImage,
                )
            )
        }

    suspend fun openImage(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, accountName: String) {
        navigation.pushSuspending(ImageConfig.Image(mxcUrl, encryptedFile, fileName, accountName))
    }

    fun closeImage() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    fun isImageOpen(): Boolean {
        return stack.value.active.configuration is ImageConfig.Image
    }

    sealed class ImageConfig : Parcelable {
        @Parcelize
        data class Image(
            val mxcUrl: String,
            val encryptedFile: @RawValue EncryptedFile?,
            val fileName: String,
            val accountName: String
        ) :
            ImageConfig()

        @Parcelize
        object None : ImageConfig()
    }

    sealed class ImageWrapper {
        class Image(val imageViewModel: ImageViewModel) : ImageWrapper()
        object None : ImageWrapper()
    }
}
