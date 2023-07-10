package de.connect2x.trixnity.messenger

import net.folivo.trixnity.client.media.MediaStore
import org.koin.core.module.Module

// since the MatrixClient can also be created by Android Services that can run without an Activity which sets the
// globally available context in the DI, we _have_ to provide a context here (only the Android impl uses it)
internal expect suspend fun createRepositoriesModule(context: Any?, accountName: String): Module

internal expect suspend fun createMediaStore(context: Any?, accountName: String): MediaStore

internal expect fun deleteDatabase(accountName: String)

expect fun closeApp()

internal expect fun getVersion(): String

internal expect fun getLicenses(): String

internal expect fun isNetworkAvailable(): Boolean

internal expect fun deviceDisplayName(): String

expect fun getLogContent(): String

expect fun sendLogToDevs(emailAddress: String, subject: String, content: String)