package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.MutableStateFlow

class CSVArchiveSink(private val i18n:I18n ) : ArchiveSink {


    private val formatExtension: String = ".csv"
    override val sinkName: String
        get() = i18n.csvFormat()
    override val archiveSinkState: MutableStateFlow<ArchiveSinkState> get() = MutableStateFlow(ArchiveSinkState.None)


    //    override val formatExtension: String
//        get() = ".csv"
//    override val formatName: String
//        get() = i18n.csvFormat()
//
//    private var appendColumnNames: Boolean = false
//
//    fun updateColumnNames() {
//        appendColumnNames = true
//    }
//
//    override suspend fun transformMessage(timelineEvent: TimelineEvent): String? {
//        val sender = timelineEvent.sender.full
//        val event = timelineEvent.event
//        val receivedDateTime = localDateTimeOf(event).formatLocalDateTime()
//        val formattedResult = timelineEvent.content?.fold(onSuccess = {
//            if (it is RoomMessageEventContent.TextBased) {
//                if (appendColumnNames) {
//                    val columnHeadings =
//                        """${i18n.csvFormatDateHeading()}, ${i18n.csvFormatTimeHeading()}, ${i18n.csvFormatSenderHeading()},  ${i18n.csvFormatMessageHeading()}"""
//                    val headingContent = columnHeadings + "\n" + "$receivedDateTime, $sender, ${it.body}"
//                    appendColumnNames = false
//                    headingContent
//                } else {
//                    "$receivedDateTime, $sender, ${it.body}"
//                }
//            } else
//                null
//        }, onFailure = {
//            log.error(it) { "failed to archive room" }
//            null
//        })
//
//        return formattedResult
//    }
}
