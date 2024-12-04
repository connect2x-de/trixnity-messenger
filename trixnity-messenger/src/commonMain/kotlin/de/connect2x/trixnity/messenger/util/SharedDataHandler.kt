package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val log = KotlinLogging.logger { }

sealed interface SharedData {

    data class SingleFile(val file: FileDescriptor) : SharedData
    data class MultipleFiles(val files: List<FileDescriptor>) : SharedData
    data class PlainText(val text: String) : SharedData
    data class Url(val url: String, val icon: FileDescriptor?) : SharedData
}

interface SharedDataHandler : StateFlow<SharedData?> {
    fun onShare(files: SharedData?)
}

open class SharedDataHandlerImpl(
    protected val flow: MutableStateFlow<SharedData?> = MutableStateFlow(null)
) : SharedDataHandler, StateFlow<SharedData?> by flow {
    override fun onShare(files: SharedData?) {
        flow.tryEmit(files)
    }
}
