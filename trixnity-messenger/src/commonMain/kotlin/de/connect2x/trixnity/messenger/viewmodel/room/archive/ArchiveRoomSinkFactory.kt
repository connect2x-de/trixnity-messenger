package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.RoomId

interface ArchiveRoomSinkFactory<T : ArchiveSink, C : ArchiveSinkConfig> {
    fun create(
        roomId: RoomId,
        matrixClient: MatrixClient,
        viewModelContext: ViewModelContext,
        sinkConfig: C
    ): T
}


class FileBaseArchiveSinkFactory : ArchiveRoomSinkFactory<ArchiveSink, ArchiveSinkConfig> {
    override fun create(
        roomId: RoomId, matrixClient: MatrixClient, viewModelContext: ViewModelContext, sinkConfig: ArchiveSinkConfig
    ): ArchiveSink {
        return when (sinkConfig) {
            is PlainTextArchiveSinkConfig -> PlainTextFormat(roomId, matrixClient, viewModelContext, sinkConfig)
            is CSVArchiveSinkConfig -> CSVArchiveFormat(viewModelContext.i18n)
            else -> throw IllegalArgumentException("Unsupported sink configuration")
        }

    }
}

class MyFactory : ArchiveRoomSinkFactory<ArchiveSink, ArchiveSinkConfig> {
    override fun create(
        roomId: RoomId,
        matrixClient: MatrixClient,
        viewModelContext: ViewModelContext,
        sinkConfig: ArchiveSinkConfig
    ): ArchiveSink {

        return when (sinkConfig) {
            is MyConfig -> {
                MyFormat(
                    roomId,
                    matrixClient,
                    viewModelContext,
                    sinkConfig
                )
            }

            else -> FileBaseArchiveSinkFactory().create(
                roomId,
                matrixClient,
                viewModelContext,
                sinkConfig
            )
        }

    }
}

class MyFormat(
    roomId: RoomId,
    matrixClient: MatrixClient,
    viewModelContext: ViewModelContext,
    sinkConfig: ArchiveSinkConfig
) : ArchiveSink {
    override val sinkName: String
        get() = "RESt"


    fun processWithRestInterfacet() {



    }
}

data class MyConfig(val path: String) : ArchiveSinkConfig

interface ArchiveSinkConfig

data class PlainTextArchiveSinkConfig(val fileName: String?, val path: String?) : ArchiveSinkConfig

class CSVArchiveSinkConfig : ArchiveSinkConfig {
    val path: String = ""

}









