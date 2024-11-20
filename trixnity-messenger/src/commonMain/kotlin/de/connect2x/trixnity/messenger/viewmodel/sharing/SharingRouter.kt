package de.connect2x.trixnity.messenger.viewmodel.sharing

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.SharedFileHandler
import de.connect2x.trixnity.messenger.util.launchReplaceAll
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.get

class SharingRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val sharedFileHandler = viewModelContext.get<SharedFileHandler>()
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        initialConfiguration = sharedFileHandler.value?.let { Config.ShareFiles(it) } ?: Config.None,
        serializer = null,
        handleBackButton = false,
        childFactory = ::createChild,
        key = "SharingRouter-${uuid4()}"
    )

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (config) {
            is Config.None -> Wrapper.None

            is Config.ShareFiles -> Wrapper.ShareFiles(
                viewModelContext.get<ShareFilesViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    sharedFiles = config.files,
                    onClose = ::close,
                )
            )
        }

    init {
        viewModelContext.coroutineScope.launch {
            sharedFileHandler.collectLatest {
                navigation.launchReplaceAll(
                    viewModelContext.coroutineScope,
                    if (it == null) Config.None else Config.ShareFiles(it)
                )
            }
        }
    }

    sealed class Wrapper {
        data object None : Wrapper()
        class ShareFiles(val viewModel: ShareFilesViewModel) : Wrapper()
    }

    sealed class Config {
        data object None : Config()
        data class ShareFiles(val files: List<FileDescriptor>) : Config()
    }

    private fun close() {
        sharedFileHandler.onShare(null)
    }
}
