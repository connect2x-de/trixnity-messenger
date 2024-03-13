package de.connect2x.trixnity.messenger.viewmodel.room.archive


import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind


fun archiveRoomModule()  = module {
    singleOf(::PlainTextFormat) bind ArchiveFormat::class
    singleOf(::CSVArchiveFormat) bind ArchiveFormat::class
    singleOf(::FileBasedResultProcessor) bind ArchiveResultProcessor::class
}
