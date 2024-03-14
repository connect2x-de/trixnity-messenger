package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId

interface ArchiveRoomSinkFactory<T : ArchiveFormat, C : ArchiveSinkConfig> {
    fun create(
        roomId: RoomId,
        matrixClient: MatrixClient,
        viewModelContext: ViewModelContext,
        sinkConfig: C
    ): T
}


class FileBaseArchiveSinkFactory : ArchiveRoomSinkFactory<ArchiveFormat, ArchiveSinkConfig> {
    override fun create(
        roomId: RoomId,
        matrixClient: MatrixClient,
        viewModelContext: ViewModelContext,
        sinkConfig: ArchiveSinkConfig
    ): ArchiveFormat {
        return when (sinkConfig) {
            is PlainTextArchiveSinkConfig -> PlainTextFormat(roomId, matrixClient, viewModelContext, sinkConfig)
            is CSVArchiveSinkConfig -> CSVArchiveFormat(viewModelContext.i18n)
            else -> throw IllegalArgumentException("Unsupported sink configuration")
        }

    }

}

class MyFactory : ArchiveRoomSinkFactory<ArchiveFormat, ArchiveSinkConfig> {
    override fun create(
        roomId: RoomId,
        matrixClient: MatrixClient,
        viewModelContext: ViewModelContext,
        sinkConfig: ArchiveSinkConfig
    ): ArchiveFormat {

        return when (sinkConfig) {
//            is MyConfig ->{
//                MyFormat()
//            }
            else -> FileBaseArchiveSinkFactory().create(
                roomId,
                matrixClient,
                viewModelContext,
                sinkConfig
            )
        }

    }
}

//class MyFormat: ArchiveFormat{
//    override val formatExtension: String
//        get() = "REst"
//    override val formatName: String
//        get() = ".txt"
//
//    override suspend fun transformMessage(timelineEvent: TimelineEvent): String? {
//      //
//    }
//
//}

data class MyConfig(val path: String) : ArchiveSinkConfig

interface ArchiveSinkConfig

data class PlainTextArchiveSinkConfig(val fileName: String?, val path: String?) : ArchiveSinkConfig

class CSVArchiveSinkConfig : ArchiveSinkConfig {
    val path: String = ""

}









