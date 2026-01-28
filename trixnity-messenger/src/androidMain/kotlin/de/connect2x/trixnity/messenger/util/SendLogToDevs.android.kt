package de.connect2x.trixnity.messenger.util

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformSendLogToDevsModule(): Module = module {
    single<SendLogToDevs> {
        val contextGetter = get<ContextGetter>()
        val rootPath = get<RootPath>().path
        SendLogToDevs { emailAddress, subject ->
            val context = contextGetter()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                rootPath.toFile().resolve("messenger.log")
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                data = "mailto:".toUri()
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
            val data = "mailto:?to=$emailAddress".toUri()
            restrictIntent.data = data
            intent.selector = restrictIntent
            @Suppress("DEPRECATION")
            ContextCompat.startActivity(
                context,
                Intent.createChooser(intent, "E-Mail").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                null
            )
        }
    }
}
