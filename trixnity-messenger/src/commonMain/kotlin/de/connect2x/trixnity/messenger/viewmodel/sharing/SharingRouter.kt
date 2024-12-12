package de.connect2x.trixnity.messenger.viewmodel.sharing

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.util.SharedData
import de.connect2x.trixnity.messenger.util.SharedDataHandler
import de.connect2x.trixnity.messenger.util.launchReplaceAll
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.get

class SharingRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val sharedDataHandler = viewModelContext.get<SharedDataHandler>()
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        initialConfiguration = sharedDataHandler.value?.let { Config.ShareData(it) } ?: Config.None,
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

            is Config.ShareData -> Wrapper.ShareData(
                viewModelContext.get<ShareDataViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    sharedData = config.data,
                    onClose = ::close,
                )
            )

        }

    init {
        viewModelContext.coroutineScope.launch {
            sharedDataHandler.collectLatest {
                navigation.launchReplaceAll(
                    viewModelContext.coroutineScope,
                    if (it == null) Config.None else Config.ShareData(it)
                )
            }
        }
    }

    sealed class Wrapper {
        data object None : Wrapper()
        class ShareData(val viewModel: ShareDataViewModel) : Wrapper()
    }

    sealed class Config {
        data object None : Config()
        data class ShareData(val data: SharedData) : Config()
    }

    private fun close() {
        sharedDataHandler.onShare(null)
    }
}
