package de.connect2x.trixnity.messenger

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import java.io.File

actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                getContext(), "de.connect2x.timmy.provider", File(content)
            )
        )
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    // @see https://stackoverflow.com/a/22309656   to restrict to only email
    val restrictIntent = Intent(Intent.ACTION_SENDTO)
    val data = Uri.parse("mailto:?to=$emailAddress")
    restrictIntent.data = data
    intent.selector = restrictIntent

    startActivity(getContext(), Intent.createChooser(intent, "E-Mail"), null)
}