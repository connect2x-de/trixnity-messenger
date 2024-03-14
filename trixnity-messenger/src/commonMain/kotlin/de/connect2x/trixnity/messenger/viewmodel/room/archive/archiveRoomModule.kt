package de.connect2x.trixnity.messenger.viewmodel.room.archive


import de.connect2x.trixnity.messenger.util.DragAndDropHandler
import de.connect2x.trixnity.messenger.util.DragAndDropHandlerBase
import de.connect2x.trixnity.messenger.viewmodel.verification.VerifyAccount
import de.connect2x.trixnity.messenger.viewmodel.verification.VerifyAccountImpl
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds


fun archiveRoomModule() = module {
    single<FileBaseArchiveSinkFactory> { FileBaseArchiveSinkFactory() }
//    single { PlainTexArchiveSink(get()) } bind ArchiveSink::class
//    single { CSVArchiveSink(get()) } bind ArchiveSink::class



//    get<ArchiveSink>()
//    single<ArchiveSink> { PlainTexArchiveSink(get()) }
//    single<ArchiveSink> { CSVArchiveSink(get()) }
    singleOf(::PlainTexArchiveSink).bind(ArchiveSink::class)
    singleOf(::CSVArchiveSink).bind(ArchiveSink::class)

//    single<ArchiveSink> { PlainTexArchiveSink(get())  }
//    single<ArchiveSink> { CSVArchiveSink(get())  }
//
//    single<PlainTexArchiveSink> { PlainTexArchiveSink(get())  }

//    single { CSVArchiveSink(get()) } bind ArchiveSink::class
//    single { PlainTexArchiveSink(get()) } bind ArchiveSink::class

//    single<ArchiveSink> {PlainTexArchiveSink() }
//    single<ArchiveSink> {CSVArchiveSink() }


}
