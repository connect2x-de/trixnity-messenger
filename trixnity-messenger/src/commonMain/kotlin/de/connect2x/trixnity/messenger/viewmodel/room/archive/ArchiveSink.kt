package de.connect2x.trixnity.messenger.viewmodel.room.archive

import kotlinx.coroutines.flow.StateFlow

interface ArchiveSink {
    suspend fun processArchive(archiveStateCallback: (ArchiveSinkState) -> Unit)

    val archiveSinkState: StateFlow<ArchiveSinkState>
}

sealed interface ArchiveSinkState {
    data object None : ArchiveSinkState
    data object Loading : ArchiveSinkState
    data object Success : ArchiveSinkState
    data class Error(val error: String) : ArchiveSinkState
}
