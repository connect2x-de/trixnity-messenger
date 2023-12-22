package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.folivo.trixnity.client.MatrixClient

interface DragAndDropHandler {
    /**
     * Files are dropped onto the messenger view
     */
    val onDrop: Flow<List<FileDescriptor>>

    /**
     * Files are dragged into the messenger view
     */
    val onDrag: Flow<List<FileDescriptor>>

    /**
     * Files ares no longer dragged above the messenger view
     */
    val onDragExit: Flow<Unit>
}

open class DragAndDropHandlerBase : DragAndDropHandler {
    fun drop(files: List<FileDescriptor>) {
        _onDrop.tryEmit(files)
    }

    private val _onDrop = MutableSharedFlow<List<FileDescriptor>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val onDrop: SharedFlow<List<FileDescriptor>> = _onDrop.asSharedFlow()

    fun drag(files: List<FileDescriptor>) {
        _onDrag.tryEmit(files)
    }

    private val _onDrag = MutableSharedFlow<List<FileDescriptor>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val onDrag: SharedFlow<List<FileDescriptor>> = _onDrag.asSharedFlow()

    fun dragExit() {
        _onDragExit.tryEmit(Unit)
    }

    private val _onDragExit = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val onDragExit: SharedFlow<Unit> = _onDragExit.asSharedFlow()
}

val MatrixClient.defaultDragAndDropHandler: DragAndDropHandlerBase
    get() = checkNotNull(di.get<DragAndDropHandler>() as? DragAndDropHandlerBase) {
        "default DragAndDropHandler has been overridden and is not of expected type DragAndDropHandlerBase"
    }