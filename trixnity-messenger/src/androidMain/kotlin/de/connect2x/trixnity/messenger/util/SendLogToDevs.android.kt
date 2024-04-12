package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI


private val log = KotlinLogging.logger { }

actual fun platformSendLogToDevsModule(): Module = module {
    single<SendLogToDevs> {
        val context = get<Context>()
        val rootPath = get<RootPath>().path
        SendLogToDevs { emailAddress, subject ->
            val email = Intent(Intent.ACTION_SEND)
            val uri = FileProvider.getUriForFile(context, "de.connect2x.timmy.provider",  rootPath.resolve("timmy.log").toFile())
            log.error{ "Path: ${uri} and type: ${context.contentResolver.getType(uri)}"}

            email.apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                putExtra(Intent.EXTRA_SUBJECT, subject)
//                putExtra(
//                    Intent.EXTRA_STREAM,
//                    FileProvider.getUriForFile(
//                        context,
//                        "de.connect2x.timmy.provider",  // TODO must be configurable
//                        rootPath.resolve("timmy.log").toFile() // TODO must be configurable
//                    )
//                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, context.contentResolver.getType(uri))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            }
            //need this to prompts email client only
            email.type = "message/rfc822"
            context.startActivity(
                Intent.createChooser(email, "Choose an Email client :").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )


//            val intent = Intent(Intent.ACTION_SEND).apply {
//                 data = Uri.parse("mailto:")
//                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
//                putExtra(Intent.EXTRA_SUBJECT, subject)
//                putExtra(
//                    Intent.EXTRA_STREAM,
//                    FileProvider.getUriForFile(
//                        context,
//                        "de.connect2x.timmy.provider",  // TODO must be configurable
//                        rootPath.resolve("timmy.log").toFile() // TODO must be configurable
//                    )
//                )
////                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
//            }
//
//            // @see https://stackoverflow.com/a/22309656   to restrict to only email
//            val restrictIntent = Intent(Intent.ACTION_SENDTO)
//            val data = Uri.parse("mailto:?to=$emailAddress")
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            restrictIntent.data = data
//            intent.selector = restrictIntent
//            context.startActivity(Intent.createChooser(intent, "E-Mail").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
