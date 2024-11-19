package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelFactory
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.launchReplaceAll
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenMediaType
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
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
                    content = config.content,
                    onCloseVideo = ::closeMedia,
                )
            )

            is Config.Image -> Wrapper.Image(
                viewModelContext.get<ImageViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    content = config.content,
                    onCloseImage = ::closeMedia,
                )
            )

            is Config.PdfDocument -> Wrapper.Pdf(
                viewModelContext.get<PdfDocumentViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    content = config.content,
                    onCloseDocument = ::closeMedia,
                )
            )

            is Config.TextDocument -> Wrapper.Text(
                viewModelContext.get<MediaViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    content = config.content,
                    fileType = OpenMediaType.TEXT,
                    onCloseMedia = ::closeMedia,
                )
            )

            is Config.MarkdownDocument -> Wrapper.Markdown(
                viewModelContext.get<MediaViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    content = config.content,
                    fileType = OpenMediaType.MARKDOWN,
                    onCloseMedia = ::closeMedia,
                )
            )
        }

    suspend fun openVideo(content: RoomMessageEventContent.FileBased.Video, userId: UserId) {
        navigation.replaceAllSuspending(Config.Video(content, userId))
    }

    suspend fun openImage(content: RoomMessageEventContent.FileBased.Image, userId: UserId) {
        navigation.replaceAllSuspending(Config.Image(content, userId))
    }

    suspend fun openPdf(content: RoomMessageEventContent.FileBased.File, userId: UserId) {
        navigation.replaceAllSuspending(Config.PdfDocument(content, userId))
    }

    suspend fun openText(content: RoomMessageEventContent.FileBased.File, userId: UserId) {
        navigation.replaceAllSuspending(Config.TextDocument(content, userId))
    }

    suspend fun openMarkdown(content: RoomMessageEventContent.FileBased.File, userId: UserId) {
        navigation.replaceAllSuspending(Config.MarkdownDocument(content, userId))
    }

    fun closeMedia() {
        navigation.launchReplaceAll(viewModelContext.coroutineScope, Config.None)
    }

    fun isMediaOpen(): Boolean = (stack.value.active.configuration is Config.None).not()

    @Serializable
    sealed class Config {
        @Serializable
        data class Video(
            val content: RoomMessageEventContent.FileBased.Video,
            val userId: UserId,
        ) : Config()

        @Serializable
        data class Image(
            val content: RoomMessageEventContent.FileBased.Image,
            val userId: UserId,
        ) : Config()

        @Serializable
        data class PdfDocument(
            val content: RoomMessageEventContent.FileBased.File,
            val userId: UserId,
        ) : Config()

        @Serializable
        data class TextDocument(
            val content: RoomMessageEventContent.FileBased.File,
            val userId: UserId,
        ) : Config()

        @Serializable
        data class MarkdownDocument(
            val content: RoomMessageEventContent.FileBased.File,
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
