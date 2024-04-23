package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        val context = get<Context>()
        UriCaller { uri ->
            log.info { "call uri: $it" }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        }
    }
}
