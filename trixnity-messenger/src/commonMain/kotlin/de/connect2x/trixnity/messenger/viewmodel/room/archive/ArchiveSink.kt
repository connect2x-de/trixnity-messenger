package de.connect2x.trixnity.messenger.viewmodel.room.archive

import kotlinx.coroutines.flow.MutableStateFlow

interface ArchiveSink {
    val sinkName: String
    val archiveSinkState: MutableStateFlow<ArchiveSinkState>
}

sealed interface ArchiveSinkState {
    data object None : ArchiveSinkState
    data object Loading : ArchiveSinkState
    data object Success : ArchiveSinkState
    data class Error(val error: String) : ArchiveSinkState
}
