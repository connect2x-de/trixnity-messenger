package de.connect2x.trixnity.messenger.export

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun exportModule(): Module = module {
    single<ExportRoom> { ExportRoomImpl(getAll()) }
    single<ExportRoomSinkFactory> { FileBasedExportRoomSinkFactory(getAll(), get(), get(), get()) }
    singleOf(::PlainTextFileBasedExportRoomSinkConverterFactory).bind<FileBasedExportRoomSinkConverterFactory>()
    singleOf(::CSVFileBasedExportRoomSinkConverterFactory).bind<FileBasedExportRoomSinkConverterFactory>()
    includes(platformFileBasedExportRoomSinkWriter())
}
