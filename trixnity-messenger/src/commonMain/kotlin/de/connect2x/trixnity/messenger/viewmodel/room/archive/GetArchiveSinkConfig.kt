package de.connect2x.trixnity.messenger.viewmodel.room.archive


data class GetPlainTextArchiveSinkConfig(var fileName: String? = null) : ArchiveSinkConfig

data class GetCSVArchiveSinkConfig(val fileName: String? = null, val requireHeadingLabels: Boolean = true) : ArchiveSinkConfig
