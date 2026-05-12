package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface SharedData {
    data class SingleFile(val file: FileDescriptor) : SharedData
    data class MultipleFiles(val files: List<FileDescriptor>) : SharedData
    data class PlainText(val text: String) : SharedData
    data class Url(val url: String, val icon: FileDescriptor?) : SharedData
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
interface SharedDataHandler : StateFlow<SharedData?> {
    fun onShare(files: SharedData?)
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
open class SharedDataHandlerImpl(
    protected val flow: MutableStateFlow<SharedData?> = MutableStateFlow(null)
) : SharedDataHandler, StateFlow<SharedData?> by flow {
    override fun onShare(files: SharedData?) {
        flow.tryEmit(files)
    }
}

val MatrixMessenger.defaultSharedDataHandler: SharedDataHandler
    get() = di.get<SharedDataHandler>()

val MatrixMultiMessenger.defaultSharedDataHandler: SharedDataHandler
    get() = di.get<SharedDataHandler>()
