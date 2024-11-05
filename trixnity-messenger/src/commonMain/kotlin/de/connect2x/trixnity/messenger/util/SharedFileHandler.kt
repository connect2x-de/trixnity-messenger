package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val log = KotlinLogging.logger { }

interface SharedFileHandler : StateFlow<List<FileDescriptor>?> {
    fun onShare(files: List<FileDescriptor>?)
}

open class SharedFileHandlerImpl(
    protected val flow: MutableStateFlow<List<FileDescriptor>?> = MutableStateFlow(null)
) : SharedFileHandler, StateFlow<List<FileDescriptor>?> by flow {
    override fun onShare(files: List<FileDescriptor>?) {
        flow.tryEmit(files)
    }
}
