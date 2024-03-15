package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.RoomId

interface ArchiveSinkConfig

interface ArchiveSinkFactory {
    val supportedFormats: List<Pair<String, ArchiveSinkConfig>>
    fun create(
        roomId: RoomId,
        matrixClient: MatrixClient,
        viewModelContext: ViewModelContext,
        sinkConfig: ArchiveSinkConfig
    ): ArchiveSink
}


class FileBaseArchiveSinkFactory(private val i18n: I18n) : ArchiveSinkFactory {
    override val supportedFormats: List<Pair<String, ArchiveSinkConfig>>
        get() = listOf(
            Pair(i18n.csvFormat(), GetCSVArchiveSinkConfig()),
            Pair(i18n.textPlainFormat(), GetPlainTextArchiveSinkConfig())
        )

    override fun create(
        roomId: RoomId, matrixClient: MatrixClient, viewModelContext: ViewModelContext, sinkConfig: ArchiveSinkConfig
    ): ArchiveSink {
        return when (sinkConfig) {
            is GetPlainTextArchiveSinkConfig -> PlainTexArchiveSink(
                viewModelContext.i18n,
                roomId = roomId,
                matrixClient = matrixClient,
                sinkConfig = sinkConfig
            )

            is GetCSVArchiveSinkConfig -> CSVArchiveSink(
                viewModelContext.i18n,
                roomId = roomId,
                matrixClient = matrixClient,
                sinkConfig = sinkConfig
            )

            else -> throw IllegalArgumentException("Unsupported sink configuration $sinkConfig")
        }

    }
}
