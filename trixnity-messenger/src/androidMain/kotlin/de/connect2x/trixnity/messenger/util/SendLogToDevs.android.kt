package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import android.net.Uri

actual fun platformSendLogToDevsModule(): Module = module {
    single<SendLogToDevs> {
        val context = get<Context>()
        val rootPath = get<RootPath>().path
        SendLogToDevs { emailAddress, subject ->
            val parentDirectory = rootPath.toFile()
            val uri = FileProvider.getUriForFile(context, "de.connect2x.timmy.provider", parentDirectory.resolve("..").resolve("timmy.log"))
            val intent = Intent(Intent.ACTION_SEND).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // @see https://stackoverflow.com/a/22309656   to restrict to only email
            val restrictIntent = Intent(Intent.ACTION_SENDTO)
            val data = Uri.parse("mailto:?to=$emailAddress")
            restrictIntent.data = data
            intent.selector = restrictIntent
            ContextCompat.startActivity(context, Intent.createChooser(intent, "E-Mail").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null)
        }
    }
}
