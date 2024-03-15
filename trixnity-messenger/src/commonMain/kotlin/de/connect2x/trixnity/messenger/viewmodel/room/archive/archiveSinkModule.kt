package de.connect2x.trixnity.messenger.viewmodel.room.archive


import org.koin.dsl.module



fun archiveRoomModule() = module {
    single<ArchiveSinkFactory> { FileBaseArchiveSinkFactory(get()) }
}
