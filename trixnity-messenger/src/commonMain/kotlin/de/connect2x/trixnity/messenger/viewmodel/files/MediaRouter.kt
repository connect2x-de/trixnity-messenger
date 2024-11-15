package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelFactory
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.launchReplaceAll
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
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

            is Config.Video -> Wrapper.Video(
                viewModelContext.get<VideoViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    mxcUrl = config.mxcUrl,
                    encryptedFile = config.encryptedFile,
                    fileName = config.fileName,
                    fileSize = config.fileSize,
                    onCloseVideo = ::closeMedia,
                )
            )

            is Config.Image -> Wrapper.Image(
                viewModelContext.get<ImageViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    mxcUrl = config.mxcUrl,
                    encryptedFile = config.encryptedFile,
                    fileName = config.fileName,
                    fileSize = config.fileSize,
                    onCloseImage = ::closeMedia,
                )
            )

            is Config.PdfDocument -> Wrapper.Pdf(
                viewModelContext.get<PdfDocumentViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    mxcUrl = config.mxcUrl,
                    encryptedFile = config.encryptedFile,
                    fileName = config.fileName,
                    fileSize = config.fileSize,
                    onCloseDocument = ::closeMedia,
                )
            )

            is Config.TextDocument -> Wrapper.Text(
                viewModelContext.get<MediaViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    mxcUrl = config.mxcUrl,
                    encryptedFile = config.encryptedFile,
                    fileName = config.fileName,
                    fileSize = config.fileSize,
                    fileType = OpenModalType.TEXT,
                    onCloseMedia = ::closeMedia,
                )
            )

            is Config.MarkdownDocument -> Wrapper.Markdown(
                viewModelContext.get<MediaViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    mxcUrl = config.mxcUrl,
                    encryptedFile = config.encryptedFile,
                    fileName = config.fileName,
                    fileSize = config.fileSize,
                    fileType = OpenModalType.MARKDOWN,
                    onCloseMedia = ::closeMedia,
                )
            )
        }

    suspend fun openVideo(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, fileSize: Long?, userId: UserId) {
        navigation.replaceAllSuspending(Config.Video(mxcUrl, encryptedFile, fileName, fileSize, userId))
    }

    suspend fun openImage(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, fileSize: Long?, userId: UserId) {
        navigation.replaceAllSuspending(Config.Image(mxcUrl, encryptedFile, fileName, fileSize, userId))
    }

    suspend fun openPdf(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String,  fileSize: Long?, userId: UserId) {
        navigation.replaceAllSuspending(Config.PdfDocument(mxcUrl, encryptedFile, fileName, fileSize, userId))
    }

    suspend fun openText(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, fileSize: Long?, userId: UserId) {
        navigation.replaceAllSuspending(Config.TextDocument(mxcUrl, encryptedFile, fileName, fileSize, userId))
    }

    suspend fun openMarkdown(mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, fileSize: Long?, userId: UserId) {
        navigation.replaceAllSuspending(Config.MarkdownDocument(mxcUrl, encryptedFile, fileName, fileSize, userId))
    }

    fun closeMedia() {
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.None)
    }

    fun isMediaOpen(): Boolean = (stack.value.active.configuration is Config.None).not()

    @Serializable
    sealed class Config {
        @Serializable
        data class Video(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val fileSize: Long?,
            val userId: UserId,
        ) : Config()

        @Serializable
        data class Image(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val fileSize: Long?,
            val userId: UserId,
        ) : Config()

        @Serializable
        data class PdfDocument(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val fileSize: Long?,
            val userId: UserId,
        ) : Config()

        @Serializable
        data class TextDocument(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val fileSize: Long?,
            val userId: UserId,
        ) : Config()

        @Serializable
        data class MarkdownDocument(
            val mxcUrl: String,
            val encryptedFile: EncryptedFile?,
            val fileName: String,
            val fileSize: Long?,
            val userId: UserId,
        ) : Config()

        @Serializable
        data object None : Config()
    }

    sealed class Wrapper {
        class Video(val viewModel: VideoViewModel) : Wrapper()
        class Image(val viewModel: ImageViewModel) : Wrapper()
        class Pdf(val viewModel: PdfDocumentViewModel) : Wrapper()
        class Markdown(val viewModel: MediaViewModel) : Wrapper()
        class Text(val viewModel: MediaViewModel) : Wrapper()
        data object None : Wrapper()
    }
}
